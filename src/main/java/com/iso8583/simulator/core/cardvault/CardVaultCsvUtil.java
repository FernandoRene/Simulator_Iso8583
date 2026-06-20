package com.iso8583.simulator.core.cardvault;

import com.iso8583.simulator.core.cardvault.model.CardBin;
import com.iso8583.simulator.core.cardvault.model.CardIssuer;
import com.iso8583.simulator.core.cardvault.model.CardRecord;
import com.iso8583.simulator.core.cardvault.model.CardStatus;
import com.iso8583.simulator.core.cardvault.model.CardVaultData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Import/export del Card Vault en formato CSV plano: una fila = una tarjeta,
 * incluyendo a qué emisor/BIN pertenece.
 *
 * Columna obligatoria: pan. El resto son opcionales; si faltan, se aplican
 * valores por defecto (issuer_id -> "DEFAULT", bin -> primeros 6 dígitos del
 * PAN, status -> ACTIVE).
 *
 * Limitación conocida: los campos entre comillas no soportan saltos de línea
 * embebidos (sí soportan comas y comillas escapadas ""). Para los datos de
 * tarjetas esto no debería ser un problema en la práctica.
 */
public final class CardVaultCsvUtil {

    public static final List<String> COLUMNS = List.of(
            "issuer_id", "issuer_name", "bin", "pan", "track2", "expiration_date",
            "cvv2", "pin_block", "balance", "currency_code", "status",
            "cardholder_name", "notes"
    );

    private CardVaultCsvUtil() {
    }

    // -----------------------------------------------------------------
    // Import
    // -----------------------------------------------------------------

    public static CardVaultData parse(InputStream input) throws IOException {
        List<List<String>> rows = readAllRows(input);
        CardVaultData data = new CardVaultData();

        if (rows.isEmpty()) {
            return data;
        }

        List<String> header = rows.get(0).stream()
                .map(h -> h.trim().toLowerCase())
                .collect(Collectors.toList());

        int panIdx = header.indexOf("pan");
        if (panIdx < 0) {
            throw new IllegalArgumentException("El CSV debe incluir una columna 'pan'");
        }

        Map<String, CardIssuer> issuersById = new LinkedHashMap<>();

        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.isEmpty() || row.stream().allMatch(String::isBlank)) {
                continue;
            }

            Map<String, String> cells = new HashMap<>();
            for (int c = 0; c < header.size() && c < row.size(); c++) {
                String value = row.get(c);
                cells.put(header.get(c), (value == null || value.isBlank()) ? null : value.trim());
            }

            String pan = cells.get("pan");
            if (pan == null) {
                throw new IllegalArgumentException("Fila " + (r + 1) + ": el PAN es obligatorio");
            }

            String issuerId = cells.get("issuer_id");
            if (issuerId == null) {
                issuerId = "DEFAULT";
            }
            String issuerName = cells.get("issuer_name");

            String bin = cells.get("bin");
            if (bin == null) {
                bin = pan.substring(0, Math.min(6, pan.length()));
            }

            CardRecord card = new CardRecord();
            card.setPan(pan);
            card.setTrack2(cells.get("track2"));
            card.setExpirationDate(cells.get("expiration_date"));
            card.setCvv2(cells.get("cvv2"));
            card.setPinBlock(cells.get("pin_block"));

            String balanceStr = cells.get("balance");
            if (balanceStr != null) {
                try {
                    card.setBalance(Long.parseLong(balanceStr));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Fila " + (r + 1) + ": balance inválido '" + balanceStr + "'");
                }
            }

            card.setCurrencyCode(cells.get("currency_code"));

            String statusStr = cells.get("status");
            if (statusStr != null) {
                try {
                    card.setStatus(CardStatus.valueOf(statusStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Fila " + (r + 1) + ": estado inválido '" + statusStr
                            + "' (valores válidos: " + Arrays.toString(CardStatus.values()) + ")");
                }
            }

            card.setCardholderName(cells.get("cardholder_name"));
            card.setNotes(cells.get("notes"));

            final String finalIssuerName = issuerName;
            CardIssuer issuer = issuersById.computeIfAbsent(issuerId, id -> {
                CardIssuer i = new CardIssuer();
                i.setId(id);
                i.setName((finalIssuerName != null) ? finalIssuerName : id);
                data.getIssuers().add(i);
                return i;
            });
            if (issuerName != null && !issuerName.isBlank()) {
                issuer.setName(issuerName);
            }

            final String finalBin = bin;
            CardBin cardBin = issuer.getBins().stream()
                    .filter(b -> finalBin.equals(b.getBin()))
                    .findFirst()
                    .orElseGet(() -> {
                        CardBin b = new CardBin();
                        b.setBin(finalBin);
                        issuer.getBins().add(b);
                        return b;
                    });

            cardBin.getCards().add(card);
        }

        return data;
    }

    // -----------------------------------------------------------------
    // Export
    // -----------------------------------------------------------------

    public static String write(CardVaultData data) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", COLUMNS)).append("\n");

        for (CardIssuer issuer : data.getIssuers()) {
            for (CardBin bin : issuer.getBins()) {
                for (CardRecord card : bin.getCards()) {
                    List<String> values = Arrays.asList(
                            value(issuer.getId()),
                            value(issuer.getName()),
                            value(bin.getBin()),
                            value(card.getPan()),
                            value(card.getTrack2()),
                            value(card.getExpirationDate()),
                            value(card.getCvv2()),
                            value(card.getPinBlock()),
                            card.getBalance() == null ? "" : String.valueOf(card.getBalance()),
                            value(card.getCurrencyCode()),
                            card.getStatus() == null ? "" : card.getStatus().name(),
                            value(card.getCardholderName()),
                            value(card.getNotes())
                    );
                    sb.append(values.stream().map(CardVaultCsvUtil::escape).collect(Collectors.joining(",")));
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    private static String value(String s) {
        return s == null ? "" : s;
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // -----------------------------------------------------------------
    // Lector CSV mínimo con soporte de comillas (sin saltos de línea embebidos)
    // -----------------------------------------------------------------

    private static List<List<String>> readAllRows(InputStream input) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(splitCsvLine(line));
            }
        }
        return rows;
    }

    private static List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(field.toString());
                    field.setLength(0);
                } else {
                    field.append(c);
                }
            }
        }

        result.add(field.toString());
        return result;
    }
}
