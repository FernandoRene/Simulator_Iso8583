package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.web.service.TransactionService;
import com.iso8583.simulator.core.transaction.model.*;
import com.iso8583.simulator.web.dto.CashAdvanceRequest;
import com.iso8583.simulator.web.dto.PurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Controlador REST para transacciones usando Strategy Pattern
 * Integra con el ConnectionManager existente
 * EXTENDIDO: Endpoints para Transfer y Authorization
 */
@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    // ============================================================================
    // ENDPOINTS EXISTENTES
    // ============================================================================

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

    // ============================================================================
    // NUEVOS ENDPOINTS - TRANSFERENCIAS
    // ============================================================================

    /**
     * Endpoint genérico para transferencias
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> processTransfer(@RequestBody TransactionRequest request) {
        logger.info("🔄 Processing transfer request: {}", request.getTransactionId());

        try {
            // Configurar como transferencia
            request.setTransactionType("TRANSFER");

            CompletableFuture<TransactionResponse> futureResponse = transactionService.processTransaction(request);
            TransactionResponse response = futureResponse.get(30, TimeUnit.SECONDS);

            return ResponseEntity.ok(response);

        } catch (TimeoutException e) {
            logger.error("❌ Transfer timeout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(TransactionResponse.systemError("Transfer timeout"));
        } catch (Exception e) {
            logger.error("❌ Error processing transfer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TransactionResponse.systemError("System error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint específico para transferencias ACH (otros bancos)
     */
    @PostMapping("/transfer/ach")
    public ResponseEntity<TransactionResponse> processACHTransfer(@RequestBody TransactionRequest request) {
        logger.info("🔄 Processing ACH transfer request: {}", request.getTransactionId());

        // Configurar específicamente para ACH
        request.setTransactionType("TRANSFER");
        request.setProcessingCode("400020"); // ACH transfer code

        return processTransfer(request);
    }

    /**
     * Endpoint específico para transferencias a cuentas propias
     */
    @PostMapping("/transfer/own-account")
    public ResponseEntity<TransactionResponse> processOwnAccountTransfer(@RequestBody TransactionRequest request) {
        logger.info("🔄 Processing own account transfer request: {}", request.getTransactionId());

        // Configurar específicamente para cuentas propias
        request.setTransactionType("TRANSFER");
        request.setProcessingCode("400040"); // Own account transfer code

        return processTransfer(request);
    }

    /**
     * Endpoint específico para transferencias a terceros afiliados
     */
    @PostMapping("/transfer/affiliated")
    public ResponseEntity<TransactionResponse> processAffiliatedTransfer(@RequestBody TransactionRequest request) {
        logger.info("🔄 Processing affiliated third party transfer request: {}", request.getTransactionId());

        // Configurar específicamente para terceros afiliados
        request.setTransactionType("TRANSFER");
        request.setProcessingCode("400060"); // Affiliated third party transfer code

        return processTransfer(request);
    }

    /**
     * Endpoint específico para transferencias a terceros nuevos
     */
    @PostMapping("/transfer/new-third-party")
    public ResponseEntity<TransactionResponse> processNewThirdPartyTransfer(@RequestBody TransactionRequest request) {
        logger.info("🔄 Processing new third party transfer request: {}", request.getTransactionId());

        // Configurar específicamente para terceros nuevos
        request.setTransactionType("TRANSFER");
        request.setProcessingCode("400080"); // New third party transfer code

        return processTransfer(request);
    }

    // ============================================================================
    // NUEVOS ENDPOINTS - AUTORIZACIONES
    // ============================================================================

    /**
     * Endpoint genérico para autorizaciones (MTI 0100)
     */
    @PostMapping("/authorization")
    public ResponseEntity<TransactionResponse> processAuthorization(@RequestBody TransactionRequest request) {
        logger.info("🔄 Processing authorization request: {}", request.getTransactionId());

        try {
            // Configurar como autorización
            request.setTransactionType("AUTHORIZATION");

            CompletableFuture<TransactionResponse> futureResponse = transactionService.processTransaction(request);
            TransactionResponse response = futureResponse.get(30, TimeUnit.SECONDS);

            return ResponseEntity.ok(response);

        } catch (TimeoutException e) {
            logger.error("❌ Authorization timeout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(TransactionResponse.systemError("Authorization timeout"));
        } catch (Exception e) {
            logger.error("❌ Error processing authorization: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TransactionResponse.systemError("System error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint específico para autorizaciones de compras extranjeras
     */
    @PostMapping("/authorization/foreign-purchase")
    public ResponseEntity<TransactionResponse> processForeignPurchase(@RequestBody TransactionRequest request) {
        logger.info("🔄 Processing foreign purchase authorization: {}", request.getTransactionId());

        // Configurar específicamente para compra extranjera
        request.setTransactionType("AUTHORIZATION");
        if (request.getProcessingCode() == null) {
            request.setProcessingCode("000000"); // Default purchase code
        }
        // Simular transacción extranjera
        if (request.getAcquiringCountry() == null) {
            request.setAcquiringCountry("840"); // USA por defecto
        }

        return processAuthorization(request);
    }

    /**
     * Endpoint específico para autorizaciones de retiros ATM externos
     */
    @PostMapping("/authorization/external-atm")
    public ResponseEntity<TransactionResponse> processExternalATMWithdrawal(@RequestBody TransactionRequest request) {
        logger.info("🔄 Processing external ATM withdrawal authorization: {}", request.getTransactionId());

        // Configurar específicamente para retiro ATM externo
        request.setTransactionType("AUTHORIZATION");
        if (request.getProcessingCode() == null) {
            request.setProcessingCode("010000"); // Default ATM withdrawal code
        }

        return processAuthorization(request);
    }

    // ============================================================================
    // ENDPOINTS DE VALIDACIÓN ESPECÍFICOS
    // ============================================================================

    /**
     * Endpoint para validar configuración de transferencia sin ejecutar
     */
    @PostMapping("/transfer/validate")
    public ResponseEntity<ValidationResult> validateTransferConfig(@RequestBody TransactionRequest request) {
        logger.info("🔍 Validating transfer configuration: {}", request.getTransactionId());

        try {
            request.setTransactionType("TRANSFER");
            ValidationResult result = transactionService.validateTransactionConfig(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Error validating transfer config: {}", e.getMessage(), e);
            ValidationResult errorResult = new ValidationResult();
            errorResult.addError("Error validating configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Endpoint para validar configuración de autorización sin ejecutar
     */
    @PostMapping("/authorization/validate")
    public ResponseEntity<ValidationResult> validateAuthorizationConfig(@RequestBody TransactionRequest request) {
        logger.info("🔍 Validating authorization configuration: {}", request.getTransactionId());

        try {
            request.setTransactionType("AUTHORIZATION");
            ValidationResult result = transactionService.validateTransactionConfig(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Error validating authorization config: {}", e.getMessage(), e);
            ValidationResult errorResult = new ValidationResult();
            errorResult.addError("Error validating configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    // ============================================================================
    // ENDPOINTS DE INFORMACIÓN
    // ============================================================================

    /**
     * Endpoint para obtener información sobre tipos de transferencia soportados
     */
    @GetMapping("/transfer/types")
    public ResponseEntity<Map<String, Object>> getTransferTypes() {
        Map<String, Object> transferTypes = new HashMap<>();

        transferTypes.put("ACH", Map.of(
                "code", "400020",
                "description", "Transferencia a otros bancos",
                "requiresTargetBank", true
        ));

        transferTypes.put("OWN_ACCOUNT", Map.of(
                "code", "400040",
                "description", "Transferencia entre cuentas propias",
                "requiresTargetBank", false
        ));

        transferTypes.put("AFFILIATED", Map.of(
                "code", "400060",
                "description", "Transferencia a terceros afiliados",
                "requiresTargetBank", false
        ));

        transferTypes.put("NEW_THIRD_PARTY", Map.of(
                "code", "400080",
                "description", "Transferencia a terceros nuevos",
                "requiresTargetBank", false
        ));

        return ResponseEntity.ok(transferTypes);
    }

    /**
     * Endpoint para obtener información sobre tipos de autorización soportados
     */
    @GetMapping("/authorization/types")
    public ResponseEntity<Map<String, Object>> getAuthorizationTypes() {
        Map<String, Object> authTypes = new HashMap<>();

        authTypes.put("PURCHASE", Map.of(
                "codes", Arrays.asList("000000", "001000", "003000"),
                "description", "Compras (POS/Internet)",
                "requiresPIN", false
        ));

        authTypes.put("ATM_WITHDRAWAL", Map.of(
                "codes", Arrays.asList("010000", "011000", "012000"),
                "description", "Retiros ATM",
                "requiresPIN", true
        ));

        return ResponseEntity.ok(authTypes);
    }

    // ============================================================================
    // ENDPOINTS EXISTENTES - MANTENIDOS
    // ============================================================================

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