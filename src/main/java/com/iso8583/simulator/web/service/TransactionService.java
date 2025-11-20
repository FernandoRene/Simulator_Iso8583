package com.iso8583.simulator.web.service;

import com.iso8583.simulator.core.connection.ConnectionManager;
import com.iso8583.simulator.core.transaction.factory.TransactionStrategyFactory;
import com.iso8583.simulator.core.transaction.strategy.TransactionStrategy;
import com.iso8583.simulator.core.transaction.model.*;
import com.iso8583.simulator.core.config.ValidationConfigService;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.iso8583.simulator.core.transaction.model.TransactionTypeDetails;


/**
 * Servicio principal para procesamiento de transacciones
 * Orquesta las estrategias y utiliza el ConnectionManager existente
 */
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private ConnectionManager connectionManager;

    @Autowired
    private TransactionStrategyFactory strategyFactory;

    @Autowired
    private ValidationConfigService validationConfig;

    /**
     * Procesa cualquier tipo de transacción usando Strategy Pattern
     */
    public CompletableFuture<TransactionResponse> processTransaction(TransactionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("🔄 Procesando transacción: {} para PAN: {}...{}",
                        request.getTransactionType(),
                        request.getPan().substring(0, 6),
                        request.getPan().substring(request.getPan().length()-4)
                );

                // 1. Obtener estrategia para el tipo de transacción
                TransactionStrategy strategy = strategyFactory.getStrategy(request.getTransactionType());

                // 2. Validar request (solo formato, NO negocio)
                ValidationResult validation = strategy.validateRequest(request);

                if (!validation.isValid()) {
                    logger.error("❌ Validación fallida: {}", validation.getErrors());
                    return TransactionResponse.validationError(validation);
                }

                if (!validation.getWarnings().isEmpty()) {
                    logger.warn("⚠️ Warnings: {}", validation.getWarnings());
                }

                // 3. Construir mensaje ISO 8583
                ISOMsg isoRequest = strategy.buildMessage(request);

                logger.info("📝 Mensaje construido - MTI: {}, Processing Code: {}, STAN: {}",
                        isoRequest.getMTI(), isoRequest.getString(3), isoRequest.getString(11));

                // 4. Enviar al core bancario usando ConnectionManager existente
                long startTime = System.currentTimeMillis();
                ISOMsg isoResponse = connectionManager.sendMessage(isoRequest).get();
                long responseTime = System.currentTimeMillis() - startTime;

                // 5. Procesar respuesta usando la estrategia
                TransactionResponse response = strategy.processResponse(isoRequest, isoResponse);
                response.setResponseTime(responseTime);
                response.setValidationWarnings(validation.getWarnings());

                logger.info("✅ Transacción completada - Code: {}, {}ms",
                        response.getResponseCode(), responseTime);

                return response;

            } catch (Exception e) {
                logger.error("❌ Error procesando transacción: {}", e.getMessage(), e);
                return TransactionResponse.systemError(e.getMessage());
            }
        });
    }

    /**
     * Obtiene información sobre los tipos de transacciones soportados
     */

    public TransactionTypesInfo getSupportedTransactionTypes() {
        List<String> types = strategyFactory.getSupportedTransactionTypes();
        Map<String, TransactionTypeDetails> details = new HashMap<>();

        for (String type : types) {
            TransactionStrategy strategy = strategyFactory.getStrategy(type);

            // Crear objeto TransactionTypeDetails
            TransactionTypeDetails detail = new TransactionTypeDetails(
                    type,
                    strategy.getProcessingCodes(),
                    strategy.getRequiredFields(),
                    strategy.requiresPIN()
            );

            details.put(type, detail);
        }

        return new TransactionTypesInfo(types, details);
    }

    /**
     * Valida configuración de transacción sin ejecutarla
     */
    public ValidationResult validateTransactionConfig(TransactionRequest request) {
        try {
            TransactionStrategy strategy = strategyFactory.getStrategy(request.getTransactionType());
            ValidationResult result = strategy.validateRequest(request);

            // Agregar validaciones adicionales si están habilitadas
            if (validationConfig.isIso8583FormatValidationEnabled()) {
                validateISOFormat(request, result);
            }

            return result;

        } catch (Exception e) {
            ValidationResult result = new ValidationResult();
            result.addError("Error validando configuración: " + e.getMessage());
            return result;
        }
    }

    /**
     * Verifica estado de conexión antes de procesar
     */
    public boolean isReadyToProcess() {
        return connectionManager.isConnected();
    }

    /**
     * Obtiene estadísticas del servicio
     */
    public Map<String, Object> getServiceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("connected", connectionManager.isConnected());
        stats.put("supportedTypes", strategyFactory.getSupportedTransactionTypes().size());
        stats.put("pendingRequests", connectionManager.getPendingRequestsCount());
        stats.put("validationEnabled", validationConfig.isIso8583FormatValidationEnabled());
        return stats;
    }

    // ============================================================================
    // MÉTODOS PRIVADOS
    // ============================================================================

    private void validateISOFormat(TransactionRequest request, ValidationResult result) {
        result.addValidation("ISO8583_COMPLIANCE");

        // Validaciones básicas de cumplimiento ISO 8583
        if (request.getPan() != null && request.getPan().length() > 19) {
            result.addError("PAN excede límite ISO 8583 (19 dígitos)");
        }

        if (request.getTerminalId() != null && request.getTerminalId().length() > 8) {
            result.addError("Terminal ID excede límite ISO 8583 (8 caracteres)");
        }

        if (request.getCardAcceptorId() != null && request.getCardAcceptorId().length() > 15) {
            result.addError("Card Acceptor ID excede límite ISO 8583 (15 caracteres)");
        }
    }
}