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
 * Estrategia para transacciones de Cash Advance (Avance de efectivo)
 * Processing Code: 011099
 */
@Component
public class CashAdvanceStrategy implements TransactionStrategy {

    @Override
    public String getTransactionType() {
        return "CASH_ADVANCE";
    }

    @Override
    public String[] getProcessingCodes() {
        return new String[]{"011099", "010000", "011000"};
    }

    @Override
    public ISOMsg buildMessage(TransactionRequest request) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200"); // Financial Transaction Request

        // Campos obligatorios
        msg.set(2, request.getPan());
        msg.set(3, "011099"); // Processing Code para Cash Advance
        msg.set(4, formatAmount(request.getAmount()));
        msg.set(7, getCurrentTransmissionDateTime());
        msg.set(11, generateStan());
        msg.set(12, getCurrentTime());
        msg.set(13, getCurrentDate());
        msg.set(14, extractExpiryFromTrack2(request.getTrack2()));
        msg.set(15, getCurrentDate()); // Settlement date
        msg.set(18, "6011"); // Merchant Category Code para Cash Advance
        msg.set(19, "068"); // Acquiring Institution Country Code (Bolivia)
        msg.set(22, "051"); // POS Entry Mode (Chip + PIN)
        msg.set(25, "02"); // POS Condition Code (Normal)
        msg.set(32, "409911"); // Acquiring Institution ID
        msg.set(35, request.getTrack2());
        msg.set(37, generateRrn());
        msg.set(41, request.getTerminalId());
        msg.set(42, request.getCardAcceptorId());
        msg.set(43, request.getCardAcceptorName());
        msg.set(49, request.getCurrencyCode());

        // Campos opcionales
        if (request.getAccount() != null) {
            msg.set(102, request.getAccount());
        }

        return msg;
    }

    @Override
    public ValidationResult validateRequest(TransactionRequest request) {
        ValidationResult result = new ValidationResult();

        // Validaciones SOLO de formato (como terminal real)
        validatePanFormat(request.getPan(), result);
        validateAmountFormat(request.getAmount(), result);
        validateTrack2Format(request.getTrack2(), result);
        validateTerminalFormat(request.getTerminalId(), result);
        validateCardAcceptorFormat(request.getCardAcceptorId(), result);

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
        return false; // Cash Advance puede ser sin PIN en algunos casos
    }

    @Override
    public String[] getRequiredFields() {
        return new String[]{"pan", "track2", "amount", "terminalId", "cardAcceptorId"};
    }

    // ============================================================================
    // MÉTODOS DE VALIDACIÓN (SOLO FORMATO, NO NEGOCIO)
    // ============================================================================

    private void validatePanFormat(String pan, ValidationResult result) {
        result.addValidation("PAN_FORMAT");

        if (pan == null || pan.trim().isEmpty()) {
            result.addError("PAN es obligatorio");
            return;
        }

        // Solo validar formato, NO Luhn
        if (!pan.matches("\\d{13,19}")) {
            result.addError("PAN debe ser numérico entre 13-19 dígitos");
        }

        // Warning si no pasa Luhn (pero NO rechazar)
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

        // Solo validar formato numérico
        if (!amount.matches("\\d{1,12}")) {
            result.addError("Amount debe ser numérico hasta 12 dígitos");
        }

        // Warning para montos altos (pero NO rechazar)
        try {
            long amountValue = Long.parseLong(amount);
            if (amountValue > 100000000) { // 1M BOB
                result.addWarning("⚠️ Monto alto - útil para testing de límites del core");
            }
            if (amountValue <= 0) {
                result.addWarning("⚠️ Monto cero/negativo - útil para testing del core");
            }
        } catch (NumberFormatException e) {
            // Ya se validó formato arriba
        }
    }

    private void validateTrack2Format(String track2, ValidationResult result) {
        result.addValidation("TRACK2_FORMAT");

        if (track2 == null || track2.trim().isEmpty()) {
            result.addError("Track2 es obligatorio");
            return;
        }

        // Validar formato básico de Track2
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
    // MÉTODOS UTILITARIOS
    // ============================================================================

    private boolean isValidLuhn(String pan) {
        // Implementación del algoritmo Luhn
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
        // Formatear a 12 dígitos con padding de ceros a la izquierda
        return String.format("%012d", Long.parseLong(amount));
    }

    private String extractExpiryFromTrack2(String track2) {
        try {
            // Extraer fecha de vencimiento del Track2 (formato: PAN=YYMM...)
            int equalIndex = track2.indexOf('=');
            if (equalIndex > 0 && track2.length() > equalIndex + 4) {
                return track2.substring(equalIndex + 1, equalIndex + 5);
            }
        } catch (Exception e) {
            // Si falla, usar fecha por defecto
        }
        return "2709"; // Default
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
        // Generar STAN secuencial simple (en producción usar el del ConnectionManager)
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