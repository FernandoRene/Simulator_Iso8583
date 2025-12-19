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
 * Estrategia para transacciones de Cashback
 * Processing Code: 090000
 * MTI: 0100 o 0200 (soporta ambos)
 * Campos especiales: 6 (Additional Amount), 54 (Cashback Amount), 51, 52, 62
 */
@Component
public class CashbackStrategy implements TransactionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CashbackStrategy.class);

    @Override
    public String getTransactionType() {
        return "CASHBACK";
    }

    @Override
    public String[] getProcessingCodes() {
        return new String[]{"090000"};
    }

    @Override
    public ISOMsg buildMessage(TransactionRequest request) throws ISOException {
        ISOMsg msg = new ISOMsg();

        Map<String, String> additionalFields = request.getAdditionalFields();

        // MTI: Soportar 0100 (Authorization) o 0200 (Financial) según additionalFields
        String mti = getFieldOrDefault(additionalFields, "0", "0100");
        msg.setMTI(mti);

        // Campos obligatorios
        msg.set(2, request.getPan());
        msg.set(3, getFieldOrDefault(additionalFields, "3", "090000")); // Processing code
        msg.set(4, formatAmount(request.getAmount())); // Transaction amount

        // Campo 6: Additional amounts (si viene en additionalFields)
        if (additionalFields != null && additionalFields.containsKey("6")) {
            msg.set(6, additionalFields.get("6"));
        }

        // Campos de fecha/hora SIEMPRE se regeneran (seguridad)
        msg.set(7, getCurrentTransmissionDateTime());
        msg.set(12, getCurrentTime());
        msg.set(13, getCurrentDate());
        msg.set(15, getCurrentDate());

        // Campos que pueden venir de additionalFields o se generan
        msg.set(11, getFieldOrGenerate(additionalFields, "11", this::generateStan));
        msg.set(14, getFieldOrExtract(additionalFields, "14", () -> extractExpiryFromTrack2(request.getTrack2())));
        msg.set(18, getFieldOrDefault(additionalFields, "18", "5411")); // Merchant type
        msg.set(19, getFieldOrDefault(additionalFields, "19", "068"));
        msg.set(22, getFieldOrDefault(additionalFields, "22", "051"));
        msg.set(25, getFieldOrDefault(additionalFields, "25", "00"));
        msg.set(32, getFieldOrDefault(additionalFields, "32", "416686"));

        // Track2 REQUERIDO para cashback
        msg.set(35, request.getTrack2());

        msg.set(37, getFieldOrGenerate(additionalFields, "37", this::generateRrn));
        msg.set(41, request.getTerminalId());
        msg.set(42, request.getCardAcceptorId());
        msg.set(43, getFieldOrDefault(additionalFields, "43", request.getCardAcceptorName()));
        msg.set(49, getFieldOrDefault(additionalFields, "49", request.getCurrencyCode()));

        // Campo 51: Currency code cardholder billing (opcional)
        if (additionalFields != null && additionalFields.containsKey("51")) {
            msg.set(51, additionalFields.get("51"));
        } else {
            msg.set(51, "840"); // USD default
        }

        // Campo 52: PIN data (opcional, viene del HSM generalmente)
        if (additionalFields != null && additionalFields.containsKey("52")) {
            msg.set(52, additionalFields.get("52"));
        }

        // Campo 54: Additional amounts - CASHBACK AMOUNT (REQUERIDO)
        // Formato: 0040068D000000001487 (Account type, Amount type, Currency code, Amount sign, Amount)
        if (additionalFields != null && additionalFields.containsKey("54")) {
            msg.set(54, additionalFields.get("54"));
        } else {
            // Si no viene, generar formato básico
            // 00 (account type default), 40 (cashback), 068 (BOB), D (debit), monto
            String cashbackAmount = request.getCashbackAmount();
            if (cashbackAmount != null && !cashbackAmount.trim().isEmpty()) {
                String formatted54 = buildField54(cashbackAmount, request.getCurrencyCode());
                msg.set(54, formatted54);
            }
        }

        // Campo 62: Private data (opcional)
        if (additionalFields != null && additionalFields.containsKey("62")) {
            msg.set(62, additionalFields.get("62"));
        }

        // Agregar todos los campos adicionales restantes
        if (additionalFields != null && !additionalFields.isEmpty()) {
            for (Map.Entry<String, String> entry : additionalFields.entrySet()) {
                try {
                    int fieldNumber = Integer.parseInt(entry.getKey());
                    // No sobrescribir campos críticos de seguridad ni los ya seteados
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
        validateTrack2Format(request.getTrack2(), entryMode, result); // Track2 REQUERIDO
        validateTerminalFormat(request.getTerminalId(), result);
        validateCardAcceptorFormat(request.getCardAcceptorId(), result);

        // Validación específica de cashback: Campo 54 o cashbackAmount
        validateCashbackAmount(request, result);

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
        return true; // Cashback requiere PIN
    }

    @Override
    public String[] getRequiredFields() {
        return new String[]{"pan", "track2", "amount", "cashbackAmount", "terminalId", "cardAcceptorId"};
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

        // Track2 es OBLIGATORIO para cashback
        if (track2 == null || track2.trim().isEmpty()) {
            result.addError("Track2 es obligatorio para Cashback");
            return;
        }

        // Validar formato
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

    private void validateCashbackAmount(TransactionRequest request, ValidationResult result) {
        result.addValidation("CASHBACK_AMOUNT");

        // Verificar si campo 54 viene en additionalFields
        if (request.getAdditionalFields() != null && request.getAdditionalFields().containsKey("54")) {
            logger.debug("Campo 54 presente en additionalFields");
            return; // OK
        }

        // Si no viene campo 54, debe venir cashbackAmount
        String cashbackAmount = request.getCashbackAmount();
        if (cashbackAmount == null || cashbackAmount.trim().isEmpty()) {
            result.addError("Cashback amount es obligatorio (campo 54 o cashbackAmount)");
        }
    }

    // ============================================================================
    // MÉTODOS UTILITARIOS
    // ============================================================================

    /**
     * Construye campo 54 (Additional Amounts) para cashback
     * Formato: AABBCCCDnnnnnnnnnnnn
     * AA = Account type (00 = default, 10 = savings, 20 = checking, 30 = credit)
     * BB = Amount type (40 = cashback)
     * CCC = Currency code (068 = BOB, 840 = USD)
     * D = Amount sign (C = credit, D = debit)
     * nnnnnnnnnnnn = Amount (12 digits)
     */
    private String buildField54(String cashbackAmount, String currencyCode) {
        String accountType = "00"; // Default
        String amountType = "40"; // Cashback
        String currency = currencyCode != null ? currencyCode : "068"; // BOB default
        String amountSign = "D"; // Debit
        String formattedAmount = String.format("%012d", Long.parseLong(cashbackAmount));

        return accountType + amountType + currency + amountSign + formattedAmount;
    }

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