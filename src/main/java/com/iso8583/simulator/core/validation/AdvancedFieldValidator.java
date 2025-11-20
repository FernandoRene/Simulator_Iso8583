package com.iso8583.simulator.core.validation;

import com.iso8583.simulator.core.field.FieldMetadataService;
import com.iso8583.simulator.core.field.model.FieldMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Validador avanzado de campos ISO8583
 * Soporta reglas configurables y validaciones
 */
@Service
public class AdvancedFieldValidator {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedFieldValidator.class);

    @Autowired
    private FieldMetadataService metadataService;

    /**
     * Valida un campo individual contra su metadata y reglas
     */
    public FieldValidationResult validateField(int fieldNumber, String value) {
        FieldValidationResult result = new FieldValidationResult(fieldNumber);

        if (value == null || value.trim().isEmpty()) {
            result.addError("Valor vacío o nulo");
            return result;
        }

        FieldMetadata metadata = metadataService.getFieldMetadata(fieldNumber);
        if (metadata == null) {
            result.addWarning("Campo no tiene metadata definida");
            return result;
        }

        // Validar tipo de campo
        validateFieldType(value, metadata, result);

        // Validar longitud
        validateLength(value, metadata, result);

        // Validar valores permitidos
        validateAllowedValues(value, metadata, result);

        // Validaciones específicas por campo
        applySpecialValidations(fieldNumber, value, result);

        return result;
    }

    /**
     * Valida el tipo de dato del campo
     */
    private void validateFieldType(String value, FieldMetadata metadata, FieldValidationResult result) {
        FieldMetadata.FieldType type = metadata.getType();

        switch (type) {
            case NUMERIC:
                if (!value.matches("\\d+")) {
                    result.addError("Campo debe ser numérico pero contiene: " +
                            value.replaceAll("\\d", ""));
                    result.addHint("Solo se permiten dígitos 0-9");
                }
                break;

            case ALPHA:
                if (!value.matches("[a-zA-Z]+")) {
                    result.addError("Campo debe ser alfabético");
                    result.addHint("Solo se permiten letras A-Z");
                }
                break;

            case ALPHANUMERIC:
                if (!value.matches("[a-zA-Z0-9]+")) {
                    result.addError("Campo debe ser alfanumérico");
                    result.addHint("Solo se permiten letras y números");
                }
                break;

            case SPECIAL:
                if (!value.matches("[a-zA-Z0-9\\s!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+")) {
                    result.addWarning("Campo contiene caracteres especiales inusuales");
                }
                break;
        }
    }

    /**
     * Valida la longitud del campo
     */
    private void validateLength(String value, FieldMetadata metadata, FieldValidationResult result) {
        int length = value.length();
        int maxLength = metadata.getMaxLength();

        if (length > maxLength) {
            result.addError(String.format("Longitud %d excede máximo permitido %d",
                    length, maxLength));
            result.addHint("Recortar valor a " + maxLength + " caracteres");
        }

        // Validar longitud fija para campos específicos
        if (isFixedLengthField(metadata.getFieldNumber())) {
            if (length != maxLength) {
                result.addError(String.format("Campo debe tener exactamente %d caracteres, tiene %d",
                        maxLength, length));
            }
        }
    }

    /**
     * Valida contra lista de valores permitidos
     */
    private void validateAllowedValues(String value, FieldMetadata metadata, FieldValidationResult result) {
        List<String> allowedValues = metadata.getAllowedValues();

        if (allowedValues != null && !allowedValues.isEmpty()) {
            if (!allowedValues.contains(value)) {
                result.addError("Valor no permitido: " + value);
                result.addHint("Valores permitidos: " + String.join(", ", allowedValues));
            }
        }
    }

    /**
     * Aplica validaciones especiales según el campo
     */
    private void applySpecialValidations(int fieldNumber, String value, FieldValidationResult result) {
        switch (fieldNumber) {
            case 2: // PAN
                validatePan(value, result);
                break;

            case 11: // STAN
                validateStan(value, result);
                break;

            case 37: // RRN
                validateRrn(value, result);
                break;

            case 4: // Amount
                validateAmount(value, result);
                break;

            case 7: // Transmission DateTime
                validateDateTime(value, result);
                break;

            case 12: // Local Time
                validateTime(value, result);
                break;

            case 13: // Local Date
                validateDate(value, result);
                break;
        }
    }

    /**
     * Valida PAN con algoritmo Luhn
     */
    private void validatePan(String pan, FieldValidationResult result) {
        if (pan.length() < 13 || pan.length() > 19) {
            result.addError("PAN debe tener entre 13 y 19 dígitos");
            return;
        }

        if (!isValidLuhn(pan)) {
            result.addWarning("PAN no pasa validación Luhn");
            result.addHint("Útil para testing, pero en producción debería pasar Luhn");
        } else {
            result.addSuccess("PAN válido según algoritmo Luhn");
        }

        // Identificar emisor por BIN
        String bin = pan.substring(0, 6);
        result.addInfo("BIN detectado: " + bin + " (" + identifyCardBrand(bin) + ")");
    }

    /**
     * Valida STAN (6 dígitos, 000001-999999)
     */
    private void validateStan(String stan, FieldValidationResult result) {
        if (stan.length() != 6) {
            result.addError("STAN debe tener exactamente 6 dígitos");
            return;
        }

        try {
            int stanValue = Integer.parseInt(stan);
            if (stanValue < 1 || stanValue > 999999) {
                result.addError("STAN debe estar entre 000001 y 999999");
            } else {
                result.addSuccess("STAN válido");
            }
        } catch (NumberFormatException e) {
            result.addError("STAN debe ser numérico");
        }
    }

    /**
     * Valida RRN (12 caracteres típicamente)
     */
    private void validateRrn(String rrn, FieldValidationResult result) {
        if (rrn.length() != 12) {
            result.addWarning("RRN típicamente tiene 12 caracteres, tiene " + rrn.length());
        }

        // Validar formato Julian Date + STAN
        if (rrn.matches("\\d{5}\\d{6}")) {
            String julianPart = rrn.substring(0, 5);
            String stanPart = rrn.substring(5);
            result.addInfo("Formato detectado: Julian(" + julianPart + ") + STAN(" + stanPart + ")");
        }
    }

    /**
     * Valida monto (12 dígitos en centavos)
     */
    private void validateAmount(String amount, FieldValidationResult result) {
        if (amount.length() != 12) {
            result.addError("Amount debe tener exactamente 12 dígitos");
            return;
        }

        try {
            long amountValue = Long.parseLong(amount);

            if (amountValue == 0) {
                result.addWarning("Monto cero - válido para consultas pero inusual para transacciones");
            }

            if (amountValue > 999999999999L) {
                result.addError("Monto excede máximo permitido");
            }

            // Convertir a formato legible
            double amountInCurrency = amountValue / 100.0;
            result.addInfo(String.format("Monto equivalente: %.2f", amountInCurrency));

        } catch (NumberFormatException e) {
            result.addError("Amount debe ser numérico");
        }
    }

    /**
     * Valida formato de fecha y hora (MMDDhhmmss)
     */
    private void validateDateTime(String dateTime, FieldValidationResult result) {
        if (dateTime.length() != 10) {
            result.addError("DateTime debe tener 10 dígitos (MMDDhhmmss)");
            return;
        }

        try {
            int month = Integer.parseInt(dateTime.substring(0, 2));
            int day = Integer.parseInt(dateTime.substring(2, 4));
            int hour = Integer.parseInt(dateTime.substring(4, 6));
            int minute = Integer.parseInt(dateTime.substring(6, 8));
            int second = Integer.parseInt(dateTime.substring(8, 10));

            if (month < 1 || month > 12) {
                result.addError("Mes inválido: " + month);
            }
            if (day < 1 || day > 31) {
                result.addError("Día inválido: " + day);
            }
            if (hour < 0 || hour > 23) {
                result.addError("Hora inválida: " + hour);
            }
            if (minute < 0 || minute > 59) {
                result.addError("Minuto inválido: " + minute);
            }
            if (second < 0 || second > 59) {
                result.addError("Segundo inválido: " + second);
            }

            if (result.isValid()) {
                result.addSuccess(String.format("DateTime válido: %02d/%02d %02d:%02d:%02d",
                        month, day, hour, minute, second));
            }

        } catch (NumberFormatException e) {
            result.addError("DateTime debe ser numérico");
        }
    }

    /**
     * Valida formato de hora (hhmmss)
     */
    private void validateTime(String time, FieldValidationResult result) {
        if (time.length() != 6) {
            result.addError("Time debe tener 6 dígitos (hhmmss)");
            return;
        }

        try {
            int hour = Integer.parseInt(time.substring(0, 2));
            int minute = Integer.parseInt(time.substring(2, 4));
            int second = Integer.parseInt(time.substring(4, 6));

            if (hour < 0 || hour > 23) result.addError("Hora inválida: " + hour);
            if (minute < 0 || minute > 59) result.addError("Minuto inválido: " + minute);
            if (second < 0 || second > 59) result.addError("Segundo inválido: " + second);

            if (result.isValid()) {
                result.addSuccess(String.format("Time válido: %02d:%02d:%02d", hour, minute, second));
            }
        } catch (NumberFormatException e) {
            result.addError("Time debe ser numérico");
        }
    }

    /**
     * Valida formato de fecha (MMDD)
     */
    private void validateDate(String date, FieldValidationResult result) {
        if (date.length() != 4) {
            result.addError("Date debe tener 4 dígitos (MMDD)");
            return;
        }

        try {
            int month = Integer.parseInt(date.substring(0, 2));
            int day = Integer.parseInt(date.substring(2, 4));

            if (month < 1 || month > 12) result.addError("Mes inválido: " + month);
            if (day < 1 || day > 31) result.addError("Día inválido: " + day);

            if (result.isValid()) {
                result.addSuccess(String.format("Date válido: %02d/%02d", month, day));
            }
        } catch (NumberFormatException e) {
            result.addError("Date debe ser numérico");
        }
    }

    /**
     * Valida usando algoritmo Luhn
     */
    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Identifica marca de tarjeta por BIN
     */
    private String identifyCardBrand(String bin) {
        if (bin.startsWith("4")) return "Visa";
        if (bin.startsWith("5")) return "Mastercard";
        if (bin.startsWith("3")) return "American Express";
        if (bin.startsWith("6")) return "Discover";
        return "Unknown";
    }

    /**
     * Determina si un campo tiene longitud fija
     */
    private boolean isFixedLengthField(int fieldNumber) {
        // Campos con longitud fija en ISO8583
        return fieldNumber == 0 || fieldNumber == 11 || fieldNumber == 12 ||
                fieldNumber == 13 || fieldNumber == 4 || fieldNumber == 7;
    }

    /**
     * Clase para resultado de validación de campo
     */
    public static class FieldValidationResult {
        private final int fieldNumber;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> successes = new ArrayList<>();
        private final List<String> hints = new ArrayList<>();
        private final List<String> info = new ArrayList<>();

        public FieldValidationResult(int fieldNumber) {
            this.fieldNumber = fieldNumber;
        }

        public void addError(String error) { errors.add(error); }
        public void addWarning(String warning) { warnings.add(warning); }
        public void addSuccess(String success) { successes.add(success); }
        public void addHint(String hint) { hints.add(hint); }
        public void addInfo(String info) { this.info.add(info); }

        public boolean isValid() { return errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }

        public int getFieldNumber() { return fieldNumber; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getSuccesses() { return successes; }
        public List<String> getHints() { return hints; }
        public List<String> getInfo() { return info; }
    }
}