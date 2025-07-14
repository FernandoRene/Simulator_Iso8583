package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.web.service.TransactionService;
import com.iso8583.simulator.core.transaction.model.*;
import com.iso8583.simulator.web.dto.CashAdvanceRequest;
import com.iso8583.simulator.web.dto.PurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para transacciones usando Strategy Pattern
 * Integra con el ConnectionManager existente
 */
@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    /**
     * Endpoint para Cash Advance
     */
    @PostMapping("/cash-advance")
    public CompletableFuture<ResponseEntity<TransactionResponse>> cashAdvance(
            @RequestBody CashAdvanceRequest request) {

        logger.info("📤 Cash Advance request - PAN: {}...{}, Amount: {}",
                request.getPan().substring(0, 6), request.getPan().substring(request.getPan().length()-4),
                request.getAmount());

        // Convertir DTO a TransactionRequest
        TransactionRequest transRequest = TransactionRequest.cashAdvance(
                request.getPan(),
                request.getTrack2(),
                request.getAmount(),
                request.getTerminalId(),
                request.getCardAcceptorId()
        );

        // Opcional: sobrescribir cardAcceptorName si viene en el request
        if (request.getCardAcceptorName() != null) {
            transRequest.setCardAcceptorName(request.getCardAcceptorName());
        }

        return transactionService.processTransaction(transRequest)
                .thenApply(response -> {
                    if (response.isSuccessful()) {
                        return ResponseEntity.ok(response);
                    } else {
                        // Retornar 200 con datos del error para que el frontend pueda procesarlo
                        return ResponseEntity.ok(response);
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error en cash advance: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError(ex.getMessage()));
                });
    }

    /**
     * Endpoint para Purchase
     */
    @PostMapping("/purchase")
    public CompletableFuture<ResponseEntity<TransactionResponse>> purchase(
            @RequestBody PurchaseRequest request) {

        logger.info("📤 Purchase request - PAN: {}...{}, Amount: {}",
                request.getPan().substring(0, 6), request.getPan().substring(request.getPan().length()-4),
                request.getAmount());

        // Convertir DTO a TransactionRequest
        TransactionRequest transRequest = TransactionRequest.purchase(
                request.getPan(),
                request.getTrack2(),
                request.getAmount(),
                request.getTerminalId(),
                request.getCardAcceptorId()
        );

        if (request.getCardAcceptorName() != null) {
            transRequest.setCardAcceptorName(request.getCardAcceptorName());
        }

        return transactionService.processTransaction(transRequest)
                .thenApply(response -> {
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    logger.error("Error en purchase: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError(ex.getMessage()));
                });
    }

    /**
     * Endpoint genérico para cualquier tipo de transacción
     */
    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processTransaction(
            @RequestBody TransactionRequest request) {

        logger.info("📤 Generic transaction - Type: {}, PAN: {}...{}",
                request.getTransactionType(),
                request.getPan().substring(0, 6),
                request.getPan().substring(request.getPan().length()-4));

        return transactionService.processTransaction(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Error en transacción genérica: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError(ex.getMessage()));
                });
    }

    /**
     * Obtener tipos de transacciones soportados
     */
    @GetMapping("/types")
    public ResponseEntity<TransactionTypesInfo> getSupportedTypes() {
        TransactionTypesInfo info = transactionService.getSupportedTransactionTypes();
        return ResponseEntity.ok(info);
    }

    /**
     * Validar configuración de transacción sin ejecutarla
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateTransaction(
            @RequestBody TransactionRequest request) {

        ValidationResult result = transactionService.validateTransactionConfig(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Estado del servicio de transacciones
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = transactionService.getServiceStats();
        return ResponseEntity.ok(status);
    }

    /**
     * Verificar si el servicio está listo para procesar transacciones
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> isReady() {
        boolean ready = transactionService.isReadyToProcess();
        Map<String, Object> response = Map.of(
                "ready", ready,
                "message", ready ? "Servicio listo para procesar transacciones" : "Servicio no disponible - verificar conexión"
        );
        return ResponseEntity.ok(response);
    }
}