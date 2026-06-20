package com.iso8583.simulator.core.cardvault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.iso8583.simulator.core.cardvault.model.BulkLoadResult;
import com.iso8583.simulator.core.cardvault.model.BulkMode;
import com.iso8583.simulator.core.cardvault.model.CardBin;
import com.iso8583.simulator.core.cardvault.model.CardIssuer;
import com.iso8583.simulator.core.cardvault.model.CardLocation;
import com.iso8583.simulator.core.cardvault.model.CardRecord;
import com.iso8583.simulator.core.cardvault.model.CardStatus;
import com.iso8583.simulator.core.cardvault.model.CardVaultData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Repositorio del Card Vault.
 *
 * Mantiene el estado completo en memoria (un árbol Emisor -> BIN -> Tarjeta)
 * y lo persiste en un archivo JSON externo (NO dentro del jar), de forma que
 * pueda leerse, editarse manualmente y exportarse sin depender de una base
 * de datos.
 *
 * Toda operación de escritura (alta, edición, baja, carga masiva) reescribe
 * el archivo completo de forma "atómica" (escribe a un .tmp y luego renombra).
 */
@Component
public class CardVaultRepository implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(CardVaultRepository.class);

    @Autowired
    private CardVaultProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private CardVaultData data = new CardVaultData();

    @Override
    public void afterPropertiesSet() {
        load();
    }

    // -----------------------------------------------------------------
    // Carga / Persistencia
    // -----------------------------------------------------------------

    private void load() {
        Path path = Paths.get(properties.getStoragePath());

        lock.writeLock().lock();
        try {
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    data = objectMapper.readValue(in, CardVaultData.class);
                    logger.info("📇 Card Vault cargado desde {} ({} emisor(es))",
                            path.toAbsolutePath(), data.getIssuers().size());
                    return;
                } catch (Exception e) {
                    logger.error("❌ Error leyendo Card Vault desde {}: {}", path, e.getMessage(), e);
                    data = new CardVaultData();
                    return;
                }
            }

            data = loadSeed();
            persistInternal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private CardVaultData loadSeed() {
        String seedPath = properties.getSeedPath();
        if (seedPath == null || seedPath.isBlank()) {
            logger.info("📇 Card Vault: no existe archivo en {} y no hay semilla configurada, se inicia vacío",
                    properties.getStoragePath());
            return new CardVaultData();
        }

        try (InputStream in = openResource(seedPath)) {
            if (in == null) {
                logger.warn("⚠️ Semilla de Card Vault configurada ({}) pero no se encontró, se inicia vacío", seedPath);
                return new CardVaultData();
            }
            CardVaultData seeded = objectMapper.readValue(in, CardVaultData.class);
            logger.info("📇 Card Vault inicializado desde semilla {} ({} emisor(es))",
                    seedPath, seeded.getIssuers().size());
            return seeded;
        } catch (Exception e) {
            logger.error("❌ Error leyendo semilla de Card Vault {}: {}", seedPath, e.getMessage(), e);
            return new CardVaultData();
        }
    }

    private InputStream openResource(String location) throws IOException {
        if (location.startsWith("classpath:")) {
            return getClass().getClassLoader().getResourceAsStream(location.substring("classpath:".length()));
        }
        Path p = Paths.get(location);
        return Files.exists(p) ? Files.newInputStream(p) : null;
    }

    /** Escribe el estado actual a disco. Debe llamarse con el writeLock ya tomado. */
    private void persistInternal() {
        Path path = Paths.get(properties.getStoragePath());
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                objectMapper.writeValue(out, data);
            }

            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.debug("💾 Card Vault persistido en {}", path.toAbsolutePath());
        } catch (IOException e) {
            logger.error("❌ Error escribiendo Card Vault en {}: {}", path, e.getMessage(), e);
            throw new RuntimeException("No se pudo persistir el Card Vault: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------
    // Lectura
    // -----------------------------------------------------------------

    public CardVaultData getAll() {
        lock.readLock().lock();
        try {
            return deepCopy(data);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<CardLocation> findByPan(String pan) {
        lock.readLock().lock();
        try {
            return locate(data, pan).map(CardVaultRepository::deepCopyLocation);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<CardLocation> findCards(String issuerId, String bin, CardStatus status, String search) {
        lock.readLock().lock();
        try {
            List<CardLocation> result = new ArrayList<>();
            for (CardIssuer issuer : data.getIssuers()) {
                if (issuerId != null && !issuerId.equalsIgnoreCase(issuer.getId())) {
                    continue;
                }
                for (CardBin cardBin : issuer.getBins()) {
                    if (bin != null && !bin.equals(cardBin.getBin())) {
                        continue;
                    }
                    for (CardRecord card : cardBin.getCards()) {
                        if (status != null && card.getStatus() != status) {
                            continue;
                        }
                        if (search != null && !search.isBlank()) {
                            String needle = search.toLowerCase();
                            boolean matches = (card.getPan() != null && card.getPan().toLowerCase().contains(needle))
                                    || (card.getCardholderName() != null
                                    && card.getCardholderName().toLowerCase().contains(needle));
                            if (!matches) {
                                continue;
                            }
                        }
                        result.add(deepCopyLocation(
                                new CardLocation(issuer.getId(), issuer.getName(), cardBin.getBin(), card)));
                    }
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    // -----------------------------------------------------------------
    // Escritura
    // -----------------------------------------------------------------

    /**
     * Crea o actualiza una tarjeta. Si el PAN ya existe en otro emisor/BIN,
     * se elimina de su ubicación anterior y se coloca en la indicada
     * (emisor/BIN se crean automáticamente si no existen).
     */
    public CardLocation upsertCard(String issuerId, String issuerName, String issuerDescription,
                                   String bin, String binDescription, CardRecord card) {
        if (card.getPan() == null || card.getPan().isBlank()) {
            throw new IllegalArgumentException("El PAN es obligatorio");
        }

        String resolvedIssuerId = (issuerId == null || issuerId.isBlank()) ? "DEFAULT" : issuerId;
        String resolvedBin = (bin == null || bin.isBlank())
                ? card.getPan().substring(0, Math.min(6, card.getPan().length()))
                : bin;

        if (card.getStatus() == null) {
            card.setStatus(CardStatus.ACTIVE);
        }

        lock.writeLock().lock();
        try {
            removeCardInternal(card.getPan());

            CardIssuer issuer = findOrCreateIssuer(resolvedIssuerId, issuerName, issuerDescription);
            CardBin cardBin = findOrCreateBin(issuer, resolvedBin, binDescription);
            cardBin.getCards().add(card);

            persistInternal();
            return new CardLocation(issuer.getId(), issuer.getName(), cardBin.getBin(), card);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Actualiza los datos de una tarjeta existente sin mover su emisor/BIN. Devuelve empty si no existe. */
    public Optional<CardLocation> updateCard(String pan, CardRecord updated) {
        lock.writeLock().lock();
        try {
            Optional<CardLocation> existing = locate(data, pan);
            if (existing.isEmpty()) {
                return Optional.empty();
            }

            CardRecord target = existing.get().getCard();
            target.setTrack2(updated.getTrack2());
            target.setExpirationDate(updated.getExpirationDate());
            target.setCvv2(updated.getCvv2());
            target.setPinBlock(updated.getPinBlock());
            target.setBalance(updated.getBalance());
            target.setCurrencyCode(updated.getCurrencyCode());
            target.setStatus(updated.getStatus() != null ? updated.getStatus() : target.getStatus());
            target.setCardholderName(updated.getCardholderName());
            target.setNotes(updated.getNotes());

            persistInternal();
            return Optional.of(deepCopyLocation(existing.get()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean deleteCard(String pan) {
        lock.writeLock().lock();
        try {
            boolean removed = removeCardInternal(pan);
            if (removed) {
                persistInternal();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Carga masiva de tarjetas.
     *
     * mode = REPLACE: descarta el contenido actual y carga solo lo recibido.
     * mode = MERGE:   inserta/actualiza por PAN; lo que no esté en el archivo
     *                 cargado se conserva tal cual.
     */
    public BulkLoadResult bulkLoad(CardVaultData incoming, BulkMode mode) {
        lock.writeLock().lock();
        try {
            BulkLoadResult result = new BulkLoadResult();

            if (mode == BulkMode.REPLACE) {
                data = new CardVaultData();
            }

            for (CardIssuer incomingIssuer : incoming.getIssuers()) {
                String issuerId = (incomingIssuer.getId() == null || incomingIssuer.getId().isBlank())
                        ? "DEFAULT" : incomingIssuer.getId();

                for (CardBin incomingBin : incomingIssuer.getBins()) {
                    for (CardRecord card : incomingBin.getCards()) {
                        if (card.getPan() == null || card.getPan().isBlank()) {
                            result.addError("Tarjeta sin PAN omitida (emisor=" + issuerId + ")");
                            continue;
                        }
                        if (card.getStatus() == null) {
                            card.setStatus(CardStatus.ACTIVE);
                        }

                        String bin = (incomingBin.getBin() == null || incomingBin.getBin().isBlank())
                                ? card.getPan().substring(0, Math.min(6, card.getPan().length()))
                                : incomingBin.getBin();

                        boolean existed = removeCardInternal(card.getPan());

                        CardIssuer issuer = findOrCreateIssuer(issuerId, incomingIssuer.getName(), incomingIssuer.getDescription());
                        CardBin cardBin = findOrCreateBin(issuer, bin, incomingBin.getDescription());
                        cardBin.getCards().add(card);

                        if (existed) {
                            result.incrementUpdated();
                        } else {
                            result.incrementCreated();
                        }
                    }
                }
            }

            persistInternal();
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public byte[] exportJson() {
        lock.readLock().lock();
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error exportando Card Vault a JSON: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public byte[] exportCsv() {
        lock.readLock().lock();
        try {
            return CardVaultCsvUtil.write(data).getBytes(StandardCharsets.UTF_8);
        } finally {
            lock.readLock().unlock();
        }
    }

    // -----------------------------------------------------------------
    // Helpers internos (deben llamarse con el lock correspondiente tomado)
    // -----------------------------------------------------------------

    private CardIssuer findOrCreateIssuer(String issuerId, String issuerName, String issuerDescription) {
        for (CardIssuer issuer : data.getIssuers()) {
            if (issuer.getId().equalsIgnoreCase(issuerId)) {
                if (issuerName != null && !issuerName.isBlank()) {
                    issuer.setName(issuerName);
                }
                if (issuerDescription != null && !issuerDescription.isBlank()) {
                    issuer.setDescription(issuerDescription);
                }
                return issuer;
            }
        }
        CardIssuer issuer = new CardIssuer();
        issuer.setId(issuerId);
        issuer.setName((issuerName == null || issuerName.isBlank()) ? issuerId : issuerName);
        issuer.setDescription(issuerDescription);
        data.getIssuers().add(issuer);
        return issuer;
    }

    private CardBin findOrCreateBin(CardIssuer issuer, String bin, String binDescription) {
        for (CardBin cardBin : issuer.getBins()) {
            if (cardBin.getBin().equals(bin)) {
                if (binDescription != null && !binDescription.isBlank()) {
                    cardBin.setDescription(binDescription);
                }
                return cardBin;
            }
        }
        CardBin cardBin = new CardBin();
        cardBin.setBin(bin);
        cardBin.setDescription(binDescription);
        issuer.getBins().add(cardBin);
        return cardBin;
    }

    /** Elimina una tarjeta por PAN de cualquier emisor/BIN, eliminando contenedores que queden vacíos. */
    private boolean removeCardInternal(String pan) {
        boolean removed = false;

        Iterator<CardIssuer> issuerIt = data.getIssuers().iterator();
        while (issuerIt.hasNext()) {
            CardIssuer issuer = issuerIt.next();

            Iterator<CardBin> binIt = issuer.getBins().iterator();
            while (binIt.hasNext()) {
                CardBin bin = binIt.next();

                Iterator<CardRecord> cardIt = bin.getCards().iterator();
                while (cardIt.hasNext()) {
                    CardRecord card = cardIt.next();
                    if (pan.equals(card.getPan())) {
                        cardIt.remove();
                        removed = true;
                    }
                }

                if (bin.getCards().isEmpty()) {
                    binIt.remove();
                }
            }

            if (issuer.getBins().isEmpty()) {
                issuerIt.remove();
            }
        }

        return removed;
    }

    private static Optional<CardLocation> locate(CardVaultData data, String pan) {
        for (CardIssuer issuer : data.getIssuers()) {
            for (CardBin bin : issuer.getBins()) {
                for (CardRecord card : bin.getCards()) {
                    if (pan.equals(card.getPan())) {
                        return Optional.of(new CardLocation(issuer.getId(), issuer.getName(), bin.getBin(), card));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private CardVaultData deepCopy(CardVaultData source) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(source), CardVaultData.class);
        } catch (Exception e) {
            throw new RuntimeException("Error copiando Card Vault: " + e.getMessage(), e);
        }
    }

    private static CardLocation deepCopyLocation(CardLocation location) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            CardRecord copy = mapper.readValue(mapper.writeValueAsBytes(location.getCard()), CardRecord.class);
            return new CardLocation(location.getIssuerId(), location.getIssuerName(), location.getBin(), copy);
        } catch (Exception e) {
            throw new RuntimeException("Error copiando tarjeta: " + e.getMessage(), e);
        }
    }
}
