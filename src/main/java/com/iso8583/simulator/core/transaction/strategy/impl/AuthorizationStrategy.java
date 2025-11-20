package com.iso8583.simulator.core.transaction.strategy.impl;

import com.iso8583.simulator.core.transaction.strategy.TransactionStrategy;
import com.iso8583.simulator.core.transaction.model.TransactionRequest;
import com.iso8583.simulator.core.transaction.model.TransactionResponse;
import com.iso8583.simulator.core.transaction.model.ValidationResult;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Estrategia para transacciones de Authorization (MTI 0100)
 * Principalmente para transacciones del exterior y ATM externos
 * Processing Codes: 00XXXX (compras), 01XXXX (retiros ATM)
 */
@Component
public class AuthorizationStrategy implements TransactionStrategy {

    // Country code para Bolivia
    private static final String BOLIVIA_COUNTRY_CODE = "068";

    @Override
    public String getTransactionType() {
        return "AUTHORIZATION";
    }

    @Override
    public String[] getProcessingCodes() {
        // Patrones flexibles para autorizaciones
        return new String[]{
                "000000", "001000", "003000", // Compras (overlap con PurchaseStrategy pero diferente MTI)
                "010000", "011000", "012000", // Retiros ATM más comunes
                "013000", "014000", "015000"  // Otros retiros ATM
        };
    }

    @Override
    public ISOMsg buildMessage(TransactionRequest request) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0100"); // Authorization Request

        // Campos obligatorios para autorizaciones
        msg.set(2, request.getPan());
        msg.set(3, determineProcessingCode(request));
        msg.set(4, formatAmount(request.getAmount()));

        // Campo 6 - Cardholder Billing Amount (si es diferente de campo 4)
        if (request.getBillingAmount() != null) {
            msg.set(6, formatAmount(request.getBillingAmount()));
        }

        msg.set(7, getCurrentTransmissionDateTime());
        msg.set(11, generateStan());
        msg.set(12, getCurrentTime());
        msg.set(13, getCurrentDate());
        msg.set(14, extractExpiryFromTrack2(request.getTrack2()));
        msg.set(15, getCurrentDate()); // Settlement date

        // Campo 18 - Merchant Type (variable según adquirente)
        String merchantType = request.getMerchantType();
        msg.set(18, merchantType != null ? merchantType : getDefaultMerchantType(request));

        // Campo 19 - Acquiring Country (mantener original para identificar exterior)
        String acquiringCountry = request.getAcquiringCountry();
        msg.set(19, acquiringCountry != null ? acquiringCountry : BOLIVIA_COUNTRY_CODE);

        // Campo 22 - POS Entry Mode (variable según modo de ingreso)
        String posEntryMode = request.getPosEntryMode();
        msg.set(22, posEntryMode != null ? posEntryMode : getDefaultPosEntryMode(request));

        msg.set(25, "08"); // POS Condition Code (e-commerce por defecto)
        msg.set(32, request.getAcquiringInstitution() != null ? request.getAcquiringInstitution() : "409911");
        msg.set(35, request.getTrack2());
        msg.set(37, generateRrn());
        msg.set(41, request.getTerminalId());
        msg.set(42, request.getCardAcceptorId());
        msg.set(43, request.getCardAcceptorName());

        // Campo 49 - Transaction Currency
        msg.set(49, request.getCurrencyCode() != null ? request.getCurrencyCode() : BOLIVIA_COUNTRY_CODE);

        // Campo 51 - Cardholder Billing Currency (puede ser diferente)
        if (request.getBillingCurrency() != null) {
            msg.set(51, request.getBillingCurrency());
        }

        // Campo 52 - PIN Data (si está presente)
        if (request.getPinData() != null) {
            msg.set(52, request.getPinData());
        }

        // Campo 62 - Private Use Fields (para datos adicionales)
        if (request.getPrivateUseFields() != null) {
            msg.set(62, request.getPrivateUseFields());
        }

        return msg;
    }

    @Override
    public ValidationResult validateRequest(TransactionRequest request) {
        ValidationResult result = new ValidationResult();

        // Validaciones SOLO de formato (como en las estrategias existentes)
        validatePanFormat(request.getPan(), result);
        validateAmountFormat(request.getAmount(), result);
        validateTrack2Format(request.getTrack2(), result);
        validateTerminalFormat(request.getTerminalId(), result);
        validateCardAcceptorFormat(request.getCardAcceptorId(), result);
        validateAuthorizationSpecificFields(request, result);

        return result;
    }

    @Override
    public TransactionResponse processResponse(ISOMsg request, ISOMsg response) {
        try {
            long responseTime = System.currentTimeMillis(); // Simplificado por ahora
            return TransactionResponse.fromISOResponse(request, response, responseTime);
        } catch (ISOException e) {
            return TransactionResponse.systemError("Error procesando respuesta: " + e.getMessage());
        }
    }

    @Override
    public boolean requiresPIN() {
        return false; // Autorizaciones pueden variar, muchas no requieren PIN
    }

    @Override
    public String[] getRequiredFields() {
        return new String[]{"pan", "track2", "amount", "terminalId", "cardAcceptorId"};
    }

    // ============================================================================
    // MÉTODOS ESPECÍFICOS PARA AUTORIZACIONES
    // ============================================================================

    private String determineProcessingCode(TransactionRequest request) {
        String requestedCode = request.getProcessingCode();
        if (requestedCode != null && isValidAuthorizationCode(requestedCode)) {
            return requestedCode;
        }

        // Default a compra si no se especifica
        return "000000";
    }

    private boolean isValidAuthorizationCode(String code) {
        if (code == null) return false;

        // Acepta 00XXXX para compras y 01XXXX para retiros ATM
        return code.startsWith("00") || code.startsWith("01");
    }

    private String getDefaultMerchantType(TransactionRequest request) {
        String processingCode = determineProcessingCode(request);

        if (processingCode.startsWith("01")) {
            return "6011"; // ATM
        } else {
            return "5999"; // Miscellaneous retail
        }
    }

    private String getDefaultPosEntryMode(TransactionRequest request) {
        String processingCode = determineProcessingCode(request);

        if (processingCode.startsWith("01")) {
            return "051"; // Chip + PIN para ATM
        } else {
            return "010"; // Manual entry para compras online
        }
    }

    private void validateAuthorizationSpecificFields(TransactionRequest request, ValidationResult result) {
        result.addValidation("AUTHORIZATION_SPECIFIC");

        // Validar processing code si está presente
        String processingCode = request.getProcessingCode();
        if (processingCode != null && !isValidAuthorizationCode(processingCode)) {
            result.addError("Processing code inválido para autorizaciones: " + processingCode);
        }

        // Validar campos según tipo de transacción
        if (processingCode != null && processingCode.startsWith("01")) {
            // Validaciones específicas para retiros ATM
            validateATMFields(request, result);
        } else {
            // Validaciones específicas para compras
            validatePurchaseFields(request, result);
        }
    }

    private void validateATMFields(TransactionRequest request, ValidationResult result) {
        // Validaciones específicas para retiros ATM
        if (request.getTerminalId() == null || request.getTerminalId().trim().isEmpty()) {
            result.addError("Terminal ID es obligatorio para retiros ATM");
        }
    }

    private void validatePurchaseFields(TransactionRequest request, ValidationResult result) {
        // Validaciones específicas para compras
        if (request.getCardAcceptorName() == null || request.getCardAcceptorName().trim().isEmpty()) {
            result.addWarning("⚠️ Merchant name recomendado para compras");
        }
    }

    // ============================================================================
    // MÉTODOS DE VALIDACIÓN (COPIADOS DE ESTRATEGIAS EXISTENTES)
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

    private void validateTrack2Format(String track2, ValidationResult result) {
        result.addValidation("TRACK2_FORMAT");

        if (track2 == null || track2.trim().isEmpty()) {
            result.addError("Track2 es obligatorio");
            return;
        }

        if (!track2.matches("\\d{13,19}[D=]\\d{4}.*")) {
            result.addWarning("⚠️ Track2 no tiene formato estándar - puede ser testing personalizado");
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
    // MÉTODOS UTILITARIOS (COPIADOS DE ESTRATEGIAS EXISTENTES)
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
}