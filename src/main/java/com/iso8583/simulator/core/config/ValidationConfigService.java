package com.iso8583.simulator.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "validation")
public class ValidationConfigService {

    private TerminalValidation terminalValidation = new TerminalValidation();
    private BusinessValidation businessValidation = new BusinessValidation();

    public static class TerminalValidation {
        private boolean iso8583FormatValidation = true;
        private boolean fieldLengthValidation = true;
        private boolean requiredFieldsValidation = true;
        private boolean characterSetValidation = true;

        // Getters y setters
        public boolean isIso8583FormatValidation() { return iso8583FormatValidation; }
        public void setIso8583FormatValidation(boolean iso8583FormatValidation) { this.iso8583FormatValidation = iso8583FormatValidation; }

        public boolean isFieldLengthValidation() { return fieldLengthValidation; }
        public void setFieldLengthValidation(boolean fieldLengthValidation) { this.fieldLengthValidation = fieldLengthValidation; }

        public boolean isRequiredFieldsValidation() { return requiredFieldsValidation; }
        public void setRequiredFieldsValidation(boolean requiredFieldsValidation) { this.requiredFieldsValidation = requiredFieldsValidation; }

        public boolean isCharacterSetValidation() { return characterSetValidation; }
        public void setCharacterSetValidation(boolean characterSetValidation) { this.characterSetValidation = characterSetValidation; }
    }

    public static class BusinessValidation {
        private boolean panLuhnValidation = false;      // ❌ NO validar Luhn
        private boolean amountLimitsValidation = false; // ❌ NO validar límites
        private boolean cardStatusValidation = false;   // ❌ NO validar estado

        // Getters y setters
        public boolean isPanLuhnValidation() { return panLuhnValidation; }
        public void setPanLuhnValidation(boolean panLuhnValidation) { this.panLuhnValidation = panLuhnValidation; }

        public boolean isAmountLimitsValidation() { return amountLimitsValidation; }
        public void setAmountLimitsValidation(boolean amountLimitsValidation) { this.amountLimitsValidation = amountLimitsValidation; }

        public boolean isCardStatusValidation() { return cardStatusValidation; }
        public void setCardStatusValidation(boolean cardStatusValidation) { this.cardStatusValidation = cardStatusValidation; }
    }

    // Getters principales
    public TerminalValidation getTerminalValidation() { return terminalValidation; }
    public BusinessValidation getBusinessValidation() { return businessValidation; }

    // Métodos de conveniencia
    public boolean isIso8583FormatValidationEnabled() {
        return terminalValidation.isIso8583FormatValidation();
    }

    public boolean isPanLuhnValidationEnabled() {
        return businessValidation.isPanLuhnValidation();
    }

    public boolean isAmountLimitsValidationEnabled() {
        return businessValidation.isAmountLimitsValidation();
    }


}