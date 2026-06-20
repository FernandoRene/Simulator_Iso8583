package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.web.dto.BalanceInquiryRequest;
import com.iso8583.simulator.web.service.TransactionService;
import com.iso8583.simulator.core.transaction.model.*;
import com.iso8583.simulator.web.dto.CashAdvanceRequest;
import com.iso8583.simulator.web.dto.PurchaseRequest;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * Controlador REST para transacciones usando Strategy Pattern
 * Integra con el ConnectionManager existente
 * Endpoints para Transfer y Authorization
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

    @Operation(summary = "Consulta de saldo",
            description = "Procesa una consulta de saldo ISO8583")
    @PostMapping("/balance-inquiry")
    public CompletableFuture<ResponseEntity<TransactionResponse>> balanceInquiry(
            @RequestBody BalanceInquiryRequest request) {

        // ValidaciÃ³n segura para logging
        String panForLog = "N/A";
        if (request.getPan() != null && request.getPan().length() >= 6) {
            panForLog = request.getPan().substring(0, 6) + "..." +
                    request.getPan().substring(request.getPan().length()-4);
        } else if (request.getPan() != null) {
            panForLog = request.getPan(); // PAN corto, mostrar completo
        }

        logger.info("ðŸ” Consulta de saldo - PAN: {}, Terminal: {}",
                panForLog, request.getTerminalId());

        logger.info("ðŸ” Consulta de saldo - PAN: {}...{}, Terminal: {}",
                request.getPan().substring(0, 6),
                request.getPan().substring(request.getPan().length()-4),
                request.getTerminalId());

        // Convertir DTO especÃ­fico a TransactionRequest genÃ©rico
        TransactionRequest transRequest = TransactionRequest.balanceInquiry(
                request.getPan(),
                request.getTrack2(),
                request.getTerminalId(),
                request.getCardAcceptorId(),
                request.getAccount()
        );

        // 🆕 Pasar additionalFields (incluye PIN block, moneda, settlement, etc.)
        if (request.getAdditionalFields() != null && !request.getAdditionalFields().isEmpty()) {
            transRequest.setAdditionalFields(request.getAdditionalFields());
        }

        // Usar el mismo processTransaction que las demÃ¡s transacciones
        return transactionService.processTransaction(transRequest)
                .thenApply(response -> {
                    if (response.isSuccessful()) {
                        logger.info("âœ… Consulta de saldo exitosa - STAN: {}", response.getStan());
                    } else {
                        logger.warn("âš ï¸ Consulta de saldo rechazada - CÃ³digo: {}", response.getResponseCode());
                    }
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    logger.error("âŒ Error en consulta de saldo: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError(ex.getMessage()));
                });
    }

    /**
     * Endpoint para Cash Advance
     */
    @PostMapping("/cash-advance")
    public CompletableFuture<ResponseEntity<TransactionResponse>> cashAdvance(
            @RequestBody CashAdvanceRequest request) {

        logger.info("ðŸ“¤ Cash Advance request - PAN: {}...{}, Amount: {}",
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

        // 🆕 Pasar additionalFields (incluye PIN block, moneda, settlement, etc.)
        if (request.getAdditionalFields() != null && !request.getAdditionalFields().isEmpty()) {
            transRequest.setAdditionalFields(request.getAdditionalFields());
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

        // 🆕 Pasar additionalFields
        if (request.getAdditionalFields() != null && !request.getAdditionalFields().isEmpty()) {
            transRequest.setAdditionalFields(request.getAdditionalFields());
            logger.info("📋 Additional fields provided: {}", request.getAdditionalFields().keySet());
        }

        // Entry Mode elegido en el formulario (campo 22) - PurchaseStrategy lo busca
        // dentro de additionalFields["22"], no como propiedad aparte.
        if (request.getEntryMode() != null && !request.getEntryMode().trim().isEmpty()) {
            if (transRequest.getAdditionalFields() == null) {
                transRequest.setAdditionalFields(new java.util.HashMap<>());
            }
            transRequest.getAdditionalFields().putIfAbsent("22", request.getEntryMode());
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
     * Endpoint generico para cualquier tipo de transaccion
     */
    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processTransaction(
            @RequestBody TransactionRequest request) {
        // Loggear todo el objeto request
        logger.debug("Received request: {}", request);

        // O loggear específicamente cada campo
        logger.debug("TransactionType: {}, PAN: {}, Other fields...",
                request.getTransactionType(),
                request.getPan()
        );
        logger.info("ðŸ“¤ Generic transaction - Type: {}, PAN: {}...{}",
                request.getTransactionType(),
                request.getPan().substring(0, 6),
                request.getPan().substring(request.getPan().length()-4));

        return transactionService.processTransaction(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Error en transacciÃ³n genÃ©rica: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError(ex.getMessage()));
                });
    }

    // ============================================================================
    // NUEVOS ENDPOINTS - TRANSFERENCIAS
    // ============================================================================

    /**
     * Endpoint genÃ©rico para transferencias
     */
    @PostMapping("/transfer")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processTransfer(
            @RequestBody TransactionRequest request) {

        logger.info("ðŸ”„ Processing transfer request - PAN: {}...{}, Amount: {}, Type: {}",
                request.getPan().substring(0, 6), request.getPan().substring(request.getPan().length()-4),
                request.getAmount(), request.getProcessingCode());

        // Asegurar que sea transferencia
        request.setTransactionType("TRANSFER");

        return transactionService.processTransaction(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("âŒ Error processing transfer: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError("Transfer error: " + ex.getMessage()));
                });
    }

    /**
     * Endpoint especifico para transferencias ACH (otros bancos)
     */
    @PostMapping("/transfer/ach")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processACHTransfer(
            @RequestBody Map<String, String> requestData) {

        logger.info("ðŸ”„ Processing ACH transfer request");

        try {
            TransactionRequest request = TransactionRequest.achTransfer(
                    requestData.get("pan"),
                    requestData.get("track2"),
                    requestData.get("amount"),
                    requestData.get("terminalId"),
                    requestData.get("cardAcceptorId"),
                    requestData.get("sourceAccount"),
                    requestData.get("targetBankCode")
            );

            // Override cardAcceptorName si estÃ¡ presente
            if (requestData.get("cardAcceptorName") != null) {
                request.setCardAcceptorName(requestData.get("cardAcceptorName"));
            }

            return processTransfer(request);

        } catch (Exception e) {
            logger.error("âŒ Error creating ACH transfer request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Invalid ACH transfer request: " + e.getMessage()))
            );
        }
    }

    /**
     * Endpoint especÃ­fico para transferencias a cuentas propias
     */
    @PostMapping("/transfer/own-account")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processOwnAccountTransfer(
            @RequestBody Map<String, String> requestData) {

        logger.info("Processing own account transfer request");

        try {
            TransactionRequest request = TransactionRequest.transfer(
                    requestData.get("pan"),
                    requestData.get("track2"),
                    requestData.get("amount"),
                    requestData.get("terminalId"),
                    requestData.get("cardAcceptorId"),
                    requestData.get("sourceAccount"),
                    requestData.get("targetAccount"),
                    "OWN_ACCOUNT"
            );

            if (requestData.get("cardAcceptorName") != null) {
                request.setCardAcceptorName(requestData.get("cardAcceptorName"));
            }

            return processTransfer(request);

        } catch (Exception e) {
            logger.error("Error creating own account transfer request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Invalid own account transfer request: " + e.getMessage()))
            );
        }
    }

    /**
     * Endpoint especifico para transferencias a terceros afiliados
     */
    @PostMapping("/transfer/affiliated")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processAffiliatedTransfer(
            @RequestBody Map<String, String> requestData) {

        logger.info("Processing affiliated third party transfer request");

        try {
            TransactionRequest request = TransactionRequest.transfer(
                    requestData.get("pan"),
                    requestData.get("track2"),
                    requestData.get("amount"),
                    requestData.get("terminalId"),
                    requestData.get("cardAcceptorId"),
                    requestData.get("sourceAccount"),
                    requestData.get("targetAccount"),
                    "AFFILIATED"
            );

            if (requestData.get("cardAcceptorName") != null) {
                request.setCardAcceptorName(requestData.get("cardAcceptorName"));
            }

            return processTransfer(request);

        } catch (Exception e) {
            logger.error("Error creating affiliated transfer request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Invalid affiliated transfer request: " + e.getMessage()))
            );
        }
    }

    /**
     * Endpoint especÃ­fico para transferencias a terceros nuevos
     */
    @PostMapping("/transfer/new-third-party")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processNewThirdPartyTransfer(
            @RequestBody Map<String, String> requestData) {

        logger.info("Processing new third party transfer request");

        try {
            TransactionRequest request = TransactionRequest.transfer(
                    requestData.get("pan"),
                    requestData.get("track2"),
                    requestData.get("amount"),
                    requestData.get("terminalId"),
                    requestData.get("cardAcceptorId"),
                    requestData.get("sourceAccount"),
                    requestData.get("targetAccount"),
                    "NEW_THIRD_PARTY"
            );

            if (requestData.get("cardAcceptorName") != null) {
                request.setCardAcceptorName(requestData.get("cardAcceptorName"));
            }

            return processTransfer(request);

        } catch (Exception e) {
            logger.error("âŒ Error creating new third party transfer request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Invalid new third party transfer request: " + e.getMessage()))
            );
        }
    }

    // ============================================================================
    // NUEVOS ENDPOINTS - AUTORIZACIONES
    // ============================================================================

    /**
     * Endpoint genÃ©rico para autorizaciones (MTI 0100)
     */
    @PostMapping("/authorization")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processAuthorization(
            @RequestBody TransactionRequest request) {

        logger.info("Processing authorization request - PAN: {}...{}, Amount: {}, Country: {}",
                request.getPan().substring(0, 6), request.getPan().substring(request.getPan().length()-4),
                request.getAmount(), request.getAcquiringCountry());

        // Asegurar que sea autorizaciÃ³n
        request.setTransactionType("AUTHORIZATION");

        return transactionService.processTransaction(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Error processing authorization: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError("Authorization error: " + ex.getMessage()));
                });
    }
    /**
     * Endpoint para transacciones de Depósito
     * Processing Code: 21XXXX
     * MTI: 0200
     */
    @PostMapping("/deposit")
    public CompletableFuture<ResponseEntity<TransactionResponse>> deposit(
            @RequestBody TransactionRequest request) {

        logger.info("📤 Deposit request - PAN: {}...{}, Amount: {}, Account: {}",
                request.getPan().substring(0, 6),
                request.getPan().substring(request.getPan().length()-4),
                request.getAmount(),
                request.getAccount());

        // Validar campos requeridos
        if (request.getPan() == null || request.getPan().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("PAN es requerido para depósitos"))
            );
        }

        if (request.getAmount() == null || request.getAmount().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Amount es requerido para depósitos"))
            );
        }

        if (request.getAccount() == null || request.getAccount().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Cuenta destino (campo 103) es requerida para depósitos"))
            );
        }

        // Establecer tipo de transacción
        request.setTransactionType("DEPOSIT");

        return transactionService.processTransaction(request)
                .thenApply(response -> {
                    logger.info("✅ Deposit procesado - ResponseCode: {}, RRN: {}",
                            response.getResponseCode(), response.getRrn());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    logger.error("❌ Error en deposit: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError(ex.getMessage()));
                });
    }

    /**
     * Endpoint para transacciones de Cashback
     * Processing Code: 090000
     * MTI: 0100 o 0200
     */
    @PostMapping("/cashback")
    public CompletableFuture<ResponseEntity<TransactionResponse>> cashback(
            @RequestBody TransactionRequest request) {

        logger.info("📤 Cashback request - PAN: {}...{}, Amount: {}, CashbackAmount: {}",
                request.getPan().substring(0, 6),
                request.getPan().substring(request.getPan().length()-4),
                request.getAmount(),
                request.getCashbackAmount());

        // Validar campos requeridos
        if (request.getPan() == null || request.getPan().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("PAN es requerido para cashback"))
            );
        }

        if (request.getAmount() == null || request.getAmount().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Amount es requerido para cashback"))
            );
        }

        if (request.getTrack2() == null || request.getTrack2().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Track2 es requerido para cashback"))
            );
        }

        if (request.getCashbackAmount() == null || request.getCashbackAmount().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Cashback amount es requerido"))
            );
        }

        // Establecer tipo de transacción
        request.setTransactionType("CASHBACK");

        return transactionService.processTransaction(request)
                .thenApply(response -> {
                    logger.info("✅ Cashback procesado - ResponseCode: {}, RRN: {}",
                            response.getResponseCode(), response.getRrn());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    logger.error("❌ Error en cashback: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError(ex.getMessage()));
                });
    }

    /**
     * Endpoint especifico para autorizaciones de compras extranjeras
     */
    @PostMapping("/authorization/foreign-purchase")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processForeignPurchase(
            @RequestBody Map<String, String> requestData) {

        logger.info("Processing foreign purchase authorization request");

        try {
            TransactionRequest request = TransactionRequest.foreignPurchase(
                    requestData.get("pan"),
                    requestData.get("track2"),
                    requestData.get("amount"),
                    requestData.get("terminalId"),
                    requestData.get("cardAcceptorId"),
                    requestData.getOrDefault("countryCode", "840") // Default USA
            );

            if (requestData.get("cardAcceptorName") != null) {
                request.setCardAcceptorName(requestData.get("cardAcceptorName"));
            }
            if (requestData.get("merchantType") != null) {
                request.setMerchantType(requestData.get("merchantType"));
            }

            return processAuthorization(request);

        } catch (Exception e) {
            logger.error("Error creating foreign purchase request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Invalid foreign purchase request: " + e.getMessage()))
            );
        }
    }

    /**
     * Endpoint para transacciones de Reversa (Reversal)
     * Processing Code: 200000
     * MTI: 0400 (Financial Transaction Reversal Request) o 0420 (Reversal Advice Request)
     */
    @PostMapping("/reversal")
    public CompletableFuture<ResponseEntity<TransactionResponse>> reversal(
            @RequestBody TransactionRequest request) {

        logger.info("📤 Reversal request - PAN: {}...{}, Amount: {}, MTI: {}",
                request.getPan() != null ? request.getPan().substring(0, 6) : "N/A",
                request.getPan() != null ? request.getPan().substring(request.getPan().length()-4) : "N/A",
                request.getAmount(),
                request.getMti() != null ? request.getMti() : "0400");

        // Validar campos requeridos
        if (request.getPan() == null || request.getPan().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("PAN es requerido para reversas"))
            );
        }

        if (request.getAmount() == null || request.getAmount().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Amount es requerido para reversas"))
            );
        }

        if (request.getTerminalId() == null || request.getTerminalId().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Terminal ID es requerido para reversas"))
            );
        }

        if (request.getCardAcceptorId() == null || request.getCardAcceptorId().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Card Acceptor ID es requerido para reversas"))
            );
        }

        // ✅ Track2 es OPCIONAL para reversas (a diferencia de cashback)
        // ✅ Los campos originalMTI, originalSTAN, originalDateTime, etc. son opcionales
        //    y se manejan dentro de ReversalStrategy

        // Establecer tipo de transacción
        request.setTransactionType("REVERSAL");

        return transactionService.processTransaction(request)
                .thenApply(response -> {
                    logger.info("✅ Reversal procesado - ResponseCode: {}, RRN: {}, STAN: {}",
                            response.getResponseCode(),
                            response.getRrn(),
                            response.getStan());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    logger.error("❌ Error en reversal: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponse.systemError(ex.getMessage()));
                });
    }

    /**
     * Endpoint especifico para autorizaciones de retiros ATM externos
     */
    @PostMapping("/authorization/external-atm")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processExternalATMWithdrawal(
            @RequestBody Map<String, String> requestData) {

        logger.info("Processing external ATM withdrawal authorization request");

        try {
            TransactionRequest request = TransactionRequest.authorization(
                    requestData.get("pan"),
                    requestData.get("track2"),
                    requestData.get("amount"),
                    requestData.get("terminalId"),
                    requestData.get("cardAcceptorId"),
                    "EXTERNAL_ATM"
            );

            if (requestData.get("cardAcceptorName") != null) {
                request.setCardAcceptorName(requestData.get("cardAcceptorName"));
            }
            if (requestData.get("countryCode") != null) {
                request.setAcquiringCountry(requestData.get("countryCode"));
            }

            return processAuthorization(request);

        } catch (Exception e) {
            logger.error("Error creating external ATM request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(TransactionResponse.systemError("Invalid external ATM request: " + e.getMessage()))
            );
        }
    }

    // ============================================================================
    // ENDPOINTS DE VALIDACIÃ“N ESPECÃFICOS
    // ============================================================================

    /**
     * Endpoint para validar configuraciÃ³n de transferencia sin ejecutar
     */
    @PostMapping("/transfer/validate")
    public ResponseEntity<ValidationResult> validateTransferConfig(@RequestBody TransactionRequest request) {
        logger.info("Validating transfer configuration");

        try {
            request.setTransactionType("TRANSFER");
            ValidationResult result = transactionService.validateTransactionConfig(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("âŒ Error validating transfer config: {}", e.getMessage(), e);
            ValidationResult errorResult = new ValidationResult();
            errorResult.addError("Error validating configuration: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    /**
     * Endpoint para validar configuraciÃ³n de autorizaciÃ³n sin ejecutar
     */
    @PostMapping("/authorization/validate")
    public ResponseEntity<ValidationResult> validateAuthorizationConfig(@RequestBody TransactionRequest request) {
        logger.info("ðŸ” Validating authorization configuration");

        try {
            request.setTransactionType("AUTHORIZATION");
            ValidationResult result = transactionService.validateTransactionConfig(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("âŒ Error validating authorization config: {}", e.getMessage(), e);
            ValidationResult errorResult = new ValidationResult();
            errorResult.addError("Error validating configuration: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    // ============================================================================
    // ENDPOINTS DE INFORMACIÃ“N
    // ============================================================================

    /**
     * Endpoint para obtener informaciÃ³n sobre tipos de transferencia soportados
     */
    @GetMapping("/transfer/types")
    public ResponseEntity<Map<String, Object>> getTransferTypes() {
        Map<String, Object> transferTypes = new HashMap<>();

        transferTypes.put("ACH", Map.of(
                "code", "400020",
                "description", "Transferencia ACH a otros bancos",
                "requiresTargetBank", true,
                "mti", "0200"
        ));

        transferTypes.put("OWN_ACCOUNT", Map.of(
                "code", "400040",
                "description", "Transferencia entre cuentas propias",
                "requiresTargetBank", false,
                "mti", "0200"
        ));

        transferTypes.put("AFFILIATED", Map.of(
                "code", "400060",
                "description", "Transferencia a terceros afiliados",
                "requiresTargetBank", false,
                "mti", "0200"
        ));

        transferTypes.put("NEW_THIRD_PARTY", Map.of(
                "code", "400080",
                "description", "Transferencia a terceros nuevos",
                "requiresTargetBank", false,
                "mti", "0200"
        ));

        return ResponseEntity.ok(transferTypes);
    }

    /**
     * Endpoint para obtener informaciÃ³n sobre tipos de autorizaciÃ³n soportados
     */
    @GetMapping("/authorization/types")
    public ResponseEntity<Map<String, Object>> getAuthorizationTypes() {
        Map<String, Object> authTypes = new HashMap<>();

        authTypes.put("PURCHASE", Map.of(
                "codes", Arrays.asList("000000", "001000", "003000"),
                "description", "Compras (POS/Internet)",
                "requiresPIN", false,
                "mti", "0100"
        ));

        authTypes.put("ATM_WITHDRAWAL", Map.of(
                "codes", Arrays.asList("010000", "011000", "012000"),
                "description", "Retiros ATM",
                "requiresPIN", true,
                "mti", "0100"
        ));

        authTypes.put("FOREIGN_TRANSACTION", Map.of(
                "description", "Transacciones del exterior",
                "supportedCountries", Arrays.asList("840", "978", "276", "826"),
                "mti", "0100"
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
     * Validar configuraciÃ³n de transacciÃ³n sin ejecutarla
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
     * Verificar si el servicio estÃ¡ listo para procesar transacciones
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> isReady() {
        boolean ready = transactionService.isReadyToProcess();

        Map<String, Object> response = Map.of(
                "ready", ready,
                "message", ready ?
                        "Servicio listo para procesar transacciones" :
                        "Servicio no disponible - verificar conexiÃ³n",
                "timestamp", System.currentTimeMillis()
        );

        // âœ… Status code correcto segÃºn el estado
        if (ready) {
            return ResponseEntity.ok(response);  // 200 OK
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)  // 503
                    .body(response);
        }
    }
}