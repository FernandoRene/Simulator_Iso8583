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
 * Estrategia para transacciones de Balance Inquiry (Consulta de saldo)
 * Processing Code: 301099
 */
@Component
public class BalanceInquiryStrategy implements TransactionStrategy {

    @Override
    public String getTransactionType() {
        return "BALANCE_INQUIRY";
    }

    @Override
    public String[] getProcessingCodes() {
        return new String[]{"301099", "300000", "301000"};
    }

    @Override
    public ISOMsg buildMessage(TransactionRequest request) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200"); // Financial Transaction Request

        // Campos obligatorios para Balance Inquiry
        msg.set(2, request.getPan());
        msg.set(3, "301099"); // Processing Code para Balance Inquiry
        msg.set(4, "000000000000"); // Amount siempre cero para consulta de saldo
        msg.set(7, getCurrentTransmissionDateTime());
        msg.set(11, generateStan());
        msg.set(12, getCurrentTime());
        msg.set(13, getCurrentDate());
        msg.set(14, extractExpiryFromTrack2(request.getTrack2()));
        msg.set(15, getCurrentDate()); // Settlement date
        msg.set(18, "6011"); // Merchant Category Code para consultas
        msg.set(19, "068"); // Acquiring Institution Country Code (Bolivia)
        msg.set(22, "051"); // POS Entry Mode (Chip + PIN)
        msg.set(25, "00"); // POS Condition Code (Normal)
        msg.set(32, "409911"); // Acquiring Institution ID
        msg.set(35, request.getTrack2());
        msg.set(37, generateRrn());
        msg.set(41, request.getTerminalId());
        msg.set(42, request.getCardAcceptorId());
        msg.set(43, request.getCardAcceptorName());
        msg.set(49, request.getCurrencyCode());

        // Campo específico para balance inquiry
        if (request.getAccount() != null && !request.getAccount().trim().isEmpty()) {
            msg.set(102, request.getAccount()); // Account Identification
        }

        return msg;
    }

    @Override
    public ValidationResult validateRequest(TransactionRequest request) {
        ValidationResult result = new ValidationResult();

        // Validaciones SOLO de formato (como en las otras estrategias)
        validatePanFormat(request.getPan(), result);
        validateTrack2Format(request.getTrack2(), result);
        validateTerminalFormat(request.getTerminalId(), result);
        validateCardAcceptorFormat(request.getCardAcceptorId(), result);
        validateBalanceInquirySpecificFields(request, result);

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
        return true; // Balance inquiry normalmente requiere PIN
    }

    @Override
    public String[] getRequiredFields() {
        return new String[]{"pan", "track2", "terminalId", "cardAcceptorId"};
    }

    // ============================================================================
    // MÉTODOS ESPECÍFICOS PARA BALANCE INQUIRY
    // ============================================================================

    private void validateBalanceInquirySpecificFields(TransactionRequest request, ValidationResult result) {
        result.addValidation("BALANCE_INQUIRY_SPECIFIC");

        // Para balance inquiry el monto debe ser cero
        if (request.getAmount() != null && !request.getAmount().equals("000000000000")) {
            result.addWarning("⚠️ Monto será ignorado para consulta de saldo");
        }

        // Account es opcional pero recomendado
        if (request.getAccount() == null || request.getAccount().trim().isEmpty()) {
            result.addWarning("⚠️ Account ID no especificado - se consultará cuenta por defecto");
        }
    }

    // ============================================================================
    // MÉTODOS DE VALIDACIÓN (COPIADOS DE OTRAS ESTRATEGIAS)
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
    // MÉTODOS UTILITARIOS (COPIADOS DE OTRAS ESTRATEGIAS)
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