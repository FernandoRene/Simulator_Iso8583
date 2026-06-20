package com.iso8583.simulator.core.transaction.strategy.impl;

import com.iso8583.simulator.core.transaction.strategy.TransactionStrategy;
import com.iso8583.simulator.core.transaction.model.TransactionRequest;
import com.iso8583.simulator.core.transaction.model.TransactionResponse;
import com.iso8583.simulator.core.transaction.model.ValidationResult;
import com.iso8583.simulator.web.controller.TransactionController;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Estrategia para transacciones de Reversal (Reversa)
 * Processing Code: 200000
 * MTI: 0400 (Financial Transaction Reversal Request) o 0420 (Reversal Advice)
 *
 * Las reversas se utilizan para cancelar una transacción previamente autorizada.
 * Requieren información de la transacción original (STAN, RRN, DateTime, etc.)
 */
@Component
@Service
public class ReversalStrategy implements TransactionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Override
    public String getTransactionType() {
        return "REVERSAL";
    }

    @Override
    public String[] getProcessingCodes() {
        return new String[]{"200000"};
    }

    @Override
    public ISOMsg buildMessage(TransactionRequest request) throws ISOException {
        ISOMsg msg = new ISOMsg();

        Map<String, String> additionalFields = request.getAdditionalFields();

        // MTI parametrizable: 0400 (default) o 0420
        String mti = getFieldOrDefault(additionalFields, "mti", "0400");
        if (!mti.equals("0400") && !mti.equals("0420")) {
            logger.warn("MTI inválido para reversa: {}. Usando 0400 por defecto", mti);
            mti = "0400";
        }
        msg.setMTI(mti);
        logger.debug("Reversa con MTI: {}", mti);

        // Campos obligatorios básicos
        msg.set(2, request.getPan());
        msg.set(3, getFieldOrDefault(additionalFields, "3", "200000")); // Processing Code para Reversal
        msg.set(4, formatAmount(request.getAmount()));

        // Campos de fecha/hora actuales (de la reversa)
        msg.set(7, getCurrentTransmissionDateTime());
        msg.set(12, getCurrentTime());
        msg.set(13, getCurrentDate());

        // ✅ CORRECCIÓN: STAN y RRN de la transacción ORIGINAL
        String originalStan = additionalFields != null ? additionalFields.get("originalStan") : null;
        String originalRrn = additionalFields != null ? additionalFields.get("originalRrn") : null;

        if (originalStan == null || originalStan.isEmpty()) {
            logger.warn("⚠️ originalStan no proporcionado en reversa, generando nuevo STAN");
            originalStan = generateStan();
        }

        if (originalRrn == null || originalRrn.isEmpty()) {
            logger.warn("⚠️ originalRrn no proporcionado en reversa, generando nuevo RRN");
            originalRrn = generateRrn();
        }

        msg.set(11, originalStan);
        msg.set(37, originalRrn);
        logger.info("✓ Reversa - STAN original: {}, RRN original: {}", originalStan, originalRrn);

        // Campos que normalmente se copian del original
        msg.set(14, getFieldOrExtract(additionalFields, "14", () -> extractExpiryFromTrack2(request.getTrack2())));
        msg.set(18, getFieldOrDefault(additionalFields, "18", "5999"));
        msg.set(19, getFieldOrDefault(additionalFields, "19", "068"));
        msg.set(22, getFieldOrDefault(additionalFields, "22", "051"));
        msg.set(25, getFieldOrDefault(additionalFields, "25", "00"));
        msg.set(32, getFieldOrDefault(additionalFields, "32", "409911"));

        // Track2 (puede ser opcional en reversas según implementación)
        if (request.getTrack2() != null && !request.getTrack2().trim().isEmpty()) {
            msg.set(35, request.getTrack2());
        }

        msg.set(41, request.getTerminalId());
        msg.set(42, request.getCardAcceptorId());
        msg.set(43, getFieldOrDefault(additionalFields, "43", request.getCardAcceptorName()));
        msg.set(49, getFieldOrDefault(additionalFields, "49", request.getCurrencyCode()));

        // **CAMPO CRÍTICO DE REVERSA: DE 90 - Original Data Elements**
        String originalDataElements = buildOriginalDataElements(additionalFields);
        if (originalDataElements != null) {
            msg.set(90, originalDataElements);
            logger.debug("Campo 90 (Original Data Elements): {}", originalDataElements);
        }

        // Agregar todos los campos adicionales que no están ya seteados
        if (additionalFields != null && !additionalFields.isEmpty()) {
            for (Map.Entry<String, String> entry : additionalFields.entrySet()) {
                try {
                    int fieldNumber = Integer.parseInt(entry.getKey());

                    // No sobrescribir campos críticos
                    if (fieldNumber != 7 && fieldNumber != 12 && fieldNumber != 13
                            && fieldNumber != 11 && fieldNumber != 37) {  // ← AGREGAR 11 y 37
                        if (!msg.hasField(fieldNumber)) {
                            msg.set(fieldNumber, entry.getValue());
                            logger.debug("Campo adicional {} agregado: {}", fieldNumber, entry.getValue());
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignorar claves no numéricas como "originalStan", "originalRrn"
                    logger.debug("Campo adicional no numérico ignorado: {}", entry.getKey());
                }
            }
        }

        return msg;
    }

    @Override
    public ValidationResult validateRequest(TransactionRequest request) {
        ValidationResult result = new ValidationResult();

        // Validaciones comunes
        validatePanFormat(request.getPan(), result);
        validateAmountFormat(request.getAmount(), result);
        validateTerminalFormat(request.getTerminalId(), result);
        validateCardAcceptorFormat(request.getCardAcceptorId(), result);

        // **VALIDACIONES ESPECÍFICAS DE REVERSA**
        validateReversalSpecificFields(request, result);

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
        return false; // Reversas normalmente NO requieren PIN
    }

    @Override
    public String[] getRequiredFields() {
        // Para reversas, los campos requeridos pueden variar
        // Como mínimo: PAN, amount, terminalId, cardAcceptorId
        return new String[]{"pan", "amount", "terminalId", "cardAcceptorId"};
    }

    // ============================================================================
    // MÉTODOS ESPECÍFICOS DE REVERSA
    // ============================================================================

    /**
     * Construye el campo DE 90 (Original Data Elements)
     * Formato típico: OriginalMTI + OriginalSTAN + OriginalDateTime + OriginalAcquiringInstID
     */
    private String buildOriginalDataElements(Map<String, String> additionalFields) {
        if (additionalFields == null) {
            return null;
        }

        // Intentar construir DE 90 desde campos individuales
        String originalMTI = additionalFields.get("originalMTI");
        String originalSTAN = additionalFields.get("originalSTAN");
        String originalDateTime = additionalFields.get("originalDateTime");
        String originalAcqInstID = additionalFields.get("originalAcqInstID");

        // Si viene el campo 90 completo, usarlo directamente
        if (additionalFields.containsKey("90")) {
            return additionalFields.get("90");
        }

        // Si vienen los campos individuales, construir el DE 90
        if (originalMTI != null && originalSTAN != null && originalDateTime != null) {
            StringBuilder de90 = new StringBuilder();
            de90.append(originalMTI);                    // 4 dígitos
            de90.append(originalSTAN);                   // 6 dígitos
            de90.append(originalDateTime);               // 10 dígitos (MMddHHmmss)
            if (originalAcqInstID != null) {
                de90.append(originalAcqInstID);          // Variable
            }
            return de90.toString();
        }

        logger.debug("No se pudo construir campo 90 - campos originales no proporcionados");
        return null;
    }

    /**
     * Validaciones específicas de reversa
     */
    private void validateReversalSpecificFields(TransactionRequest request, ValidationResult result) {
        result.addValidation("REVERSAL_SPECIFIC");

        Map<String, String> additionalFields = request.getAdditionalFields();
        if (additionalFields == null) {
            result.addWarning("⚠️ No se proporcionaron campos adicionales para reversa");
            return;
        }

        // Validar campos críticos para correlación (parametrizable)
        validateCriticalFieldsForReversal(additionalFields, result);

        // Validar MTI si viene especificado
        if (additionalFields.containsKey("mti")) {
            String mti = additionalFields.get("mti");
            if (!mti.equals("0400") && !mti.equals("0420")) {
                result.addWarning("⚠️ MTI inválido para reversa: " + mti + " (esperado: 0400 o 0420)");
            }
        }
    }

    /**
     * Valida campos críticos para correlación de reversa
     * Campos típicos: DE 3, 11, 13, 37, 41 (parametrizable)
     */
    private void validateCriticalFieldsForReversal(Map<String, String> additionalFields, ValidationResult result) {
        // Estos campos ayudan a identificar la transacción original
        boolean hasOriginalSTAN = additionalFields.containsKey("originalSTAN") || additionalFields.containsKey("11");
        boolean hasOriginalRRN = additionalFields.containsKey("originalRRN") || additionalFields.containsKey("37");
        boolean hasOriginalDateTime = additionalFields.containsKey("originalDateTime") || additionalFields.containsKey("7");

        if (!hasOriginalSTAN && !hasOriginalRRN) {
            result.addWarning("⚠️ Se recomienda incluir STAN o RRN original para mejor correlación");
        }

        // Validar formato de campos originales si existen
        if (additionalFields.containsKey("originalSTAN")) {
            String originalSTAN = additionalFields.get("originalSTAN");
            if (!originalSTAN.matches("\\d{6}")) {
                result.addError("STAN original debe ser 6 dígitos");
            }
        }

        if (additionalFields.containsKey("originalRRN")) {
            String originalRRN = additionalFields.get("originalRRN");
            if (!originalRRN.matches("\\d{12}")) {
                result.addWarning("⚠️ RRN original debería ser 12 dígitos");
            }
        }
    }

    // ============================================================================
    // MÉTODOS DE VALIDACIÓN (copiados de PurchaseStrategy)
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

        if (!isValidLuhn(pan)) {
            result.addWarning("⚠️ PAN no pasa validación Luhn - útil para testing del core");
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

        try {
            long amountValue = Long.parseLong(amount);
            if (amountValue > 100000000) {
                result.addWarning("⚠️ Monto alto - útil para testing de límites del core");
            }
            if (amountValue <= 0) {
                result.addWarning("⚠️ Monto cero/negativo - útil para testing del core");
            }
        } catch (NumberFormatException e) {
            // Ya validado arriba
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

    // ============================================================================
    // MÉTODOS UTILITARIOS (copiados de PurchaseStrategy)
    // ============================================================================

    private boolean isValidLuhn(String pan) {
        int sum = 0;
        boolean alternate = false;
        for (int i = pan.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(pan.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private String formatAmount(String amount) {
        return String.format("%012d", Long.parseLong(amount));
    }

    private String extractExpiryFromTrack2(String track2) {
        if (track2 == null || track2.isEmpty()) {
            return "2709"; // Default
        }

        try {
            int equalIndex = track2.indexOf('=');
            if (equalIndex > 0 && track2.length() > equalIndex + 4) {
                return track2.substring(equalIndex + 1, equalIndex + 5);
            }
        } catch (Exception e) {
            // Si falla, usar fecha por defecto
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

    /**
     * Obtiene campo de additionalFields o usa valor por defecto
     */
    private String getFieldOrDefault(Map<String, String> additionalFields, String fieldNumber, String defaultValue) {
        if (additionalFields != null && additionalFields.containsKey(fieldNumber)) {
            String value = additionalFields.get(fieldNumber);
            logger.debug("Usando campo {} desde additionalFields: {}", fieldNumber, value);
            return value;
        }
        return defaultValue;
    }

    /**
     * Obtiene campo de additionalFields o genera usando función
     */
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

    /**
     * Obtiene campo de additionalFields o extrae de otro campo
     */
    private String getFieldOrExtract(Map<String, String> additionalFields, String fieldNumber, java.util.function.Supplier<String> extractor) {
        if (additionalFields != null && additionalFields.containsKey(fieldNumber)) {
            String value = additionalFields.get(fieldNumber);
            logger.debug("Usando campo {} desde additionalFields: {}", fieldNumber, value);
            return value;
        }
        return extractor.get();
    }
}