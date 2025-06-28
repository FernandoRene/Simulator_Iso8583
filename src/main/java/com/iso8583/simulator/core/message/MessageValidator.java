package com.iso8583.simulator.core.message;

import com.iso8583.simulator.core.config.SwitchConfiguration.ValidationConfig;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class MessageValidator {

    public ValidationResult validateMessage(ISOMsg message, ValidationConfig config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Validate MTI
            if (message.getMTI() == null || message.getMTI().isEmpty()) {
                errors.add("MTI is required");
            } else if (message.getMTI().length() != 4) {
                errors.add("MTI must be 4 digits");
            }

            // Validate required fields
            if (config.getRequiredFields() != null) {
                for (Integer requiredField : config.getRequiredFields()) {
                    if (!message.hasField(requiredField)) {
                        errors.add("Required field " + requiredField + " is missing");
                    }
                }
            }

            // Validate field formats
            validateFieldFormats(message, errors, warnings);

            // Validate business rules
            validateBusinessRules(message, errors, warnings);

        } catch (ISOException e) {
            errors.add("ISO parsing error: " + e.getMessage());
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private void validateFieldFormats(ISOMsg message, List<String> errors, List<String> warnings) throws ISOException {
        // Validate PAN (field 2)
        if (message.hasField(2)) {
            String pan = message.getString(2);
            if (!isNumeric(pan) || pan.length() < 13 || pan.length() > 19) {
                errors.add("Field 2 (PAN) must be numeric and between 13-19 digits");
            }
        }

        // Validate processing code (field 3)
        if (message.hasField(3)) {
            String processingCode = message.getString(3);
            if (!isNumeric(processingCode) || processingCode.length() != 6) {
                errors.add("Field 3 (Processing Code) must be 6 numeric digits");
            }
        }

        // Validate amount (field 4)
        if (message.hasField(4)) {
            String amount = message.getString(4);
            if (!isNumeric(amount) || amount.length() != 12) {
                errors.add("Field 4 (Amount) must be 12 numeric digits");
            }
        }

        // Validate STAN (field 11)
        if (message.hasField(11)) {
            String stan = message.getString(11);
            if (!isNumeric(stan) || stan.length() != 6) {
                errors.add("Field 11 (STAN) must be 6 numeric digits");
            }
        }

        // Validate terminal ID (field 41)
        if (message.hasField(41)) {
            String terminalId = message.getString(41);
            if (terminalId.length() != 8) {
                warnings.add("Field 41 (Terminal ID) should be 8 characters");
            }
        }
    }

    private void validateBusinessRules(ISOMsg message, List<String> errors, List<String> warnings) throws ISOException {
        // Validate message type consistency
        String mti = message.getMTI();

        if (mti.startsWith("02")) { // Financial transaction
            if (!message.hasField(4)) {
                errors.add("Financial transactions must include amount (field 4)");
            }
        }

        if (mti.endsWith("00")) { // Request message
            if (message.hasField(39)) {
                warnings.add("Request messages should not include response code (field 39)");
            }
        }

        // Validate date/time fields consistency
        if (message.hasField(7) && message.hasField(12) && message.hasField(13)) {
            // Could add logic to validate date/time consistency
        }
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("\\d+");
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }

        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
}