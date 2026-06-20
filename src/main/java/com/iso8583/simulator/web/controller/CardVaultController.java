package com.iso8583.simulator.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iso8583.simulator.core.cardvault.CardVaultCsvUtil;
import com.iso8583.simulator.core.cardvault.CardVaultRepository;
import com.iso8583.simulator.core.cardvault.model.BulkLoadResult;
import com.iso8583.simulator.core.cardvault.model.BulkMode;
import com.iso8583.simulator.core.cardvault.model.CardIssuer;
import com.iso8583.simulator.core.cardvault.model.CardLocation;
import com.iso8583.simulator.core.cardvault.model.CardRecord;
import com.iso8583.simulator.core.cardvault.model.CardStatus;
import com.iso8583.simulator.core.cardvault.model.CardUpsertRequest;
import com.iso8583.simulator.core.cardvault.model.CardVaultData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * API REST del Card Vault: repositorio genérico de tarjetas simuladas,
 * agrupadas por emisor/BIN, con persistencia en archivo externo.
 *
 * Pensado para ser consumido íntegramente desde el frontend: alta, edición,
 * baja, carga masiva (JSON/CSV) y exportación.
 */
@RestController
@RequestMapping("/api/v1/card-vault")
@Tag(name = "Card Vault", description = "Repositorio de tarjetas para simulación (Adquirente/Emisor)")
public class CardVaultController {

    @Autowired
    private CardVaultRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Obtener todo el Card Vault (emisores, BINs y tarjetas)")
    @GetMapping
    public ResponseEntity<CardVaultData> getAll() {
        return ResponseEntity.ok(repository.getAll());
    }

    @Operation(summary = "Listar emisores configurados, con sus BINs y tarjetas")
    @GetMapping("/issuers")
    public ResponseEntity<List<CardIssuer>> getIssuers() {
        return ResponseEntity.ok(repository.getAll().getIssuers());
    }

    @Operation(summary = "Buscar tarjetas con filtros opcionales")
    @GetMapping("/cards")
    public ResponseEntity<List<CardLocation>> findCards(
            @RequestParam(required = false) String issuerId,
            @RequestParam(required = false) String bin,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(repository.findCards(issuerId, bin, status, search));
    }

    @Operation(summary = "Obtener una tarjeta por PAN")
    @GetMapping("/cards/{pan}")
    public ResponseEntity<CardLocation> getCard(@PathVariable String pan) {
        return repository.findByPan(pan)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear una tarjeta nueva o moverla/actualizarla si el PAN ya existe")
    @PostMapping("/cards")
    public ResponseEntity<?> upsertCard(@RequestBody CardUpsertRequest request) {
        if (request.getCard() == null) {
            return ResponseEntity.badRequest().body("Falta el objeto 'card' en el cuerpo de la petición");
        }
        try {
            CardLocation location = repository.upsertCard(
                    request.getIssuerId(), request.getIssuerName(), request.getIssuerDescription(),
                    request.getBin(), request.getBinDescription(), request.getCard());
            return ResponseEntity.ok(location);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Actualizar los datos de una tarjeta existente (no cambia emisor/BIN)")
    @PutMapping("/cards/{pan}")
    public ResponseEntity<CardLocation> updateCard(@PathVariable String pan, @RequestBody CardRecord card) {
        return repository.updateCard(pan, card)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar una tarjeta por PAN")
    @DeleteMapping("/cards/{pan}")
    public ResponseEntity<Void> deleteCard(@PathVariable String pan) {
        return repository.deleteCard(pan)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Carga masiva de tarjetas desde un archivo JSON o CSV")
    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkLoadResult> bulkLoad(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(defaultValue = "merge") String mode) {

        try {
            CardVaultData incoming;
            if ("csv".equalsIgnoreCase(format)) {
                incoming = CardVaultCsvUtil.parse(file.getInputStream());
            } else {
                incoming = objectMapper.readValue(file.getInputStream(), CardVaultData.class);
            }

            BulkMode bulkMode = "replace".equalsIgnoreCase(mode) ? BulkMode.REPLACE : BulkMode.MERGE;
            return ResponseEntity.ok(repository.bulkLoad(incoming, bulkMode));

        } catch (IllegalArgumentException e) {
            BulkLoadResult error = new BulkLoadResult();
            error.addError(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IOException e) {
            BulkLoadResult error = new BulkLoadResult();
            error.addError("Error leyendo el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(summary = "Exportar el Card Vault completo (JSON o CSV)")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "json") String format) {
        byte[] body;
        String filename;
        MediaType mediaType;

        if ("csv".equalsIgnoreCase(format)) {
            body = repository.exportCsv();
            filename = "card-vault.csv";
            mediaType = MediaType.parseMediaType("text/csv");
        } else {
            body = repository.exportJson();
            filename = "card-vault.json";
            mediaType = MediaType.APPLICATION_JSON;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
