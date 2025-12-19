package com.iso8583.simulator.core.transaction.strategy.impl;

import com.iso8583.simulator.core.transaction.strategy.TransactionStrategy;
import com.iso8583.simulator.core.transaction.model.TransactionRequest;
import com.iso8583.simulator.core.transaction.model.TransactionResponse;
import com.iso8583.simulator.core.transaction.model.ValidationResult;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Estrategia para transacciones de Depósito
 * Processing Code: 21XXXX (210003 cuenta propia, 210004 cuenta tercero)
 * MTI: 0200
 */
@Component
public class DepositStrategy implements TransactionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DepositStrategy.class);

    @Override
    public String getTransactionType() {
        return "DEPOSIT";
    }

    @Override
    public String[] getProcessingCodes() {
        // Validar solo los primeros 2 dígitos "21"
        return new String[]{"210003", "210004", "210000"}; // Ejemplos, validación flexible
    }

    @Override
    public ISOMsg buildMessage(TransactionRequest request) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200"); // Financial Transaction Request

        Map<String, String> additionalFields = request.getAdditionalFields();

        // Campos obligatorios
        msg.set(2, request.getPan());
        msg.set(3, getFieldOrDefault(additionalFields, "3", "210003")); // Processing code default
        msg.set(4, formatAmount(request.getAmount()));

        // Campos de fecha/hora SIEMPRE se regeneran (seguridad)
        msg.set(7, getCurrentTransmissionDateTime());
        msg.set(12, getCurrentTime());
        msg.set(13, getCurrentDate());
        msg.set(15, getCurrentDate());

        // Campos que pueden venir de additionalFields o se generan
        msg.set(11, getFieldOrGenerate(additionalFields, "11", this::generateStan));
        msg.set(14, getFieldOrExtract(additionalFields, "14", () -> extractExpiryFromTrack2(request.getTrack2())));
        msg.set(18, getFieldOrDefault(additionalFields, "18", "6011")); // Merchant type para depósitos
        msg.set(19, getFieldOrDefault(additionalFields, "19", "068"));
        msg.set(22, getFieldOrDefault(additionalFields, "22", "051"));
        msg.set(25, getFieldOrDefault(additionalFields, "25", "00"));
        msg.set(32, getFieldOrDefault(additionalFields, "32", "409911"));

        // Track2 OPCIONAL para depósitos
        if (request.getTrack2() != null && !request.getTrack2().trim().isEmpty()) {
            msg.set(35, request.getTrack2());
        }

        msg.set(37, getFieldOrGenerate(additionalFields, "37", this::generateRrn));
        msg.set(41, request.getTerminalId());
        msg.set(42, request.getCardAcceptorId());
        msg.set(43, getFieldOrDefault(additionalFields, "43", request.getCardAcceptorName()));
        msg.set(49, getFieldOrDefault(additionalFields, "49", request.getCurrencyCode()));

        // Campo 103: Cuenta destino (REQUERIDO para depósitos)
        msg.set(103, request.getAccount());

        // Agregar todos los campos adicionales
        if (additionalFields != null && !additionalFields.isEmpty()) {
            for (Map.Entry<String, String> entry : additionalFields.entrySet()) {
                try {
                    int fieldNumber = Integer.parseInt(entry.getKey());
                    if (fieldNumber != 7 && fieldNumber != 12 && fieldNumber != 13 && fieldNumber != 15) {
                        if (!msg.hasField(fieldNumber)) {
                            msg.set(fieldNumber, entry.getValue());
                            logger.debug("Campo adicional {} agregado: {}", fieldNumber, entry.getValue());
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Campo adicional ignorado (no numérico): {}", entry.getKey());
                }
            }
        }

        return msg;
    }

    @Override
    public ValidationResult validateRequest(TransactionRequest request) {
        ValidationResult result = new ValidationResult();

        String entryMode = "051"; // Default
        if (request.getAdditionalFields() != null && request.getAdditionalFields().containsKey("22")) {
            entryMode = request.getAdditionalFields().get("22");
        }

        // Validaciones comunes
        validatePanFormat(request.getPan(), result);
        validateAmountFormat(request.getAmount(), result);
        validateTrack2Format(request.getTrack2(), entryMode, result); // Track2 opcional
        validateTerminalFormat(request.getTerminalId(), result);
        validateCardAcceptorFormat(request.getCardAcceptorId(), result);

        // Validación específica de depósitos: Cuenta destino
        validateAccountFormat(request.getAccount(), result);

        // Validar processing code (flexible: solo los primeros 2 dígitos)
        validateProcessingCode(request.getAdditionalFields(), result);

        return result;
    }

    @Override
    public TransactionResponse processResponse(ISOMsg request, ISOMsg response) {
        try {
            long responseTime = System.currentTimeMillis();
            return TransactionResponse.fromISOResponse(request, response, responseTime);
        } catch (ISOException e) {
            return TransactionResponse.systemError("Error procesando respuesta: " + e.getMessage());
        }
    }

    @Override
    public boolean requiresPIN() {
        return false; // Depósitos generalmente no requieren PIN
    }

    @Override
    public String[] getRequiredFields() {
        return new String[]{"pan", "amount", "account", "terminalId", "cardAcceptorId"};
    }

    // ============================================================================
    // MÉTODOS DE VALIDACIÓN
    // ============================================================================

    private void validatePanFormat(String pan, ValidationResult result) {
        result.addValidation("PAN_FORMAT");
        if (pan == null || pan.trim().isEmpty()) {
            result.addError("PAN es obligatorio");
            return;
        }
        if (!pan.matches("\\d{13,19}")) {
            result.addError("PAN debe ser numérico entre 13-19 dígitos");
        }
    }

    private void validateAmountFormat(String amount, ValidationResult result) {
        result.addValidation("AMOUNT_FORMAT");
        if (amount == null || amount.trim().isEmpty()) {
            result.addError("Amount es obligatorio");
            return;
        }
        if (!amount.matches("\\d{1,12}")) {
            result.addError("Amount debe ser numérico hasta 12 dígitos");
        }
    }

    private void validateTrack2Format(String track2, String entryMode, ValidationResult result) {
        result.addValidation("TRACK2_FORMAT");

        // Track2 es OPCIONAL para depósitos
        if (track2 == null || track2.trim().isEmpty()) {
            logger.debug("Track2 opcional para depósitos");
            return;
        }

        // Si viene, validar formato
        if (!track2.matches("\\d{13,19}[D=]\\d{4}.*")) {
            result.addWarning("⚠️ Track2 no tiene formato estándar");
        }
    }

    private void validateTerminalFormat(String terminalId, ValidationResult result) {
        result.addValidation("TERMINAL_FORMAT");
        if (terminalId == null || terminalId.trim().isEmpty()) {
            result.addError("Terminal ID es obligatorio");
        } else if (terminalId.length() > 8) {
            result.addError("Terminal ID no puede exceder 8 caracteres");
        }
    }

    private void validateCardAcceptorFormat(String cardAcceptorId, ValidationResult result) {
        result.addValidation("CARD_ACCEPTOR_FORMAT");
        if (cardAcceptorId == null || cardAcceptorId.trim().isEmpty()) {
            result.addError("Card Acceptor ID es obligatorio");
        } else if (cardAcceptorId.length() > 15) {
            result.addError("Card Acceptor ID no puede exceder 15 caracteres");
        }
    }

    private void validateAccountFormat(String account, ValidationResult result) {
        result.addValidation("ACCOUNT_FORMAT");
        if (account == null || account.trim().isEmpty()) {
            result.addError("Cuenta destino (campo 103) es obligatoria para depósitos");
        } else if (account.length() > 28) {
            result.addError("Cuenta no puede exceder 28 caracteres");
        }
    }

    private void validateProcessingCode(Map<String, String> additionalFields, ValidationResult result) {
        result.addValidation("PROCESSING_CODE_FORMAT");

        String processingCode = "210003"; // Default
        if (additionalFields != null && additionalFields.containsKey("3")) {
            processingCode = additionalFields.get("3");
        }

        // Validar que empiece con "21"
        if (!processingCode.startsWith("21")) {
            result.addError("Processing Code debe empezar con '21' para depósitos");
        }
    }

    // ============================================================================
    // MÉTODOS UTILITARIOS
    // ============================================================================

    private String formatAmount(String amount) {
        return String.format("%012d", Long.parseLong(amount));
    }

    private String extractExpiryFromTrack2(String track2) {
        if (track2 == null || track2.trim().isEmpty()) {
            return "2709"; // Default
        }
        try {
            int equalIndex = track2.indexOf('=');
            if (equalIndex > 0 && track2.length() > equalIndex + 4) {
                return track2.substring(equalIndex + 1, equalIndex + 5);
            }
        } catch (Exception e) {
            // Usar fecha por defecto
        }
        return "2709";
    }

    private String getCurrentTransmissionDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
    }

    private String generateStan() {
        return String.format("%06d", (int)(Math.random() * 999999) + 1);
    }

    private String generateRrn() {
        String julian = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyDDD"));
        String stan = generateStan();
        String rrn = julian + stan;

        if (rrn.length() > 12) {
            rrn = rrn.substring(0, 12);
        } else if (rrn.length() < 12) {
            rrn = rrn + "0".repeat(12 - rrn.length());
        }

        return rrn;
    }

    private String getFieldOrDefault(Map<String, String> additionalFields, String fieldNumber, String defaultValue) {
        if (additionalFields != null && additionalFields.containsKey(fieldNumber)) {
            String value = additionalFields.get(fieldNumber);
            logger.debug("Usando campo {} desde additionalFields: {}", fieldNumber, value);
            return value;
        }
        return defaultValue;
    }

    private String getFieldOrGenerate(Map<String, String> additionalFields, String fieldNumber, java.util.function.Supplier<String> generator) {
        if (additionalFields != null && additionalFields.containsKey(fieldNumber)) {
            String value = additionalFields.get(fieldNumber);
            logger.debug("Usando campo {} desde additionalFields: {}", fieldNumber, value);
            return value;
        }
        String generated = generator.get();
        logger.debug("Generando campo {}: {}", fieldNumber, generated);
        return generated;
    }

    private String getFieldOrExtract(Map<String, String> additionalFields, String fieldNumber, java.util.function.Supplier<String> extractor) {
        if (additionalFields != null && additionalFields.containsKey(fieldNumber)) {
            String value = additionalFields.get(fieldNumber);
            logger.debug("Usando campo {} desde additionalFields: {}", fieldNumber, value);
            return value;
        }
        return extractor.get();
    }
}