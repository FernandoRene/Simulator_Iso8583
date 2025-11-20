package com.iso8583.simulator.core.transaction.model;

/**
 * Detalles de un tipo de transacción específico
 * Contiene información sobre processing codes, campos requeridos, etc.
 */
public class TransactionTypeDetails {
    private String type;
    private String[] processingCodes;
    private String[] requiredFields;
    private boolean requiresPIN;

    public TransactionTypeDetails() {}

    public TransactionTypeDetails(String type, String[] processingCodes,
                                  String[] requiredFields, boolean requiresPIN) {
        this.type = type;
        this.processingCodes = processingCodes;
        this.requiredFields = requiredFields;
        this.requiresPIN = requiresPIN;
    }

    // Getters y setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getProcessingCodes() {
        return processingCodes;
    }

    public void setProcessingCodes(String[] processingCodes) {
        this.processingCodes = processingCodes;
    }

    public String[] getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(String[] requiredFields) {
        this.requiredFields = requiredFields;
    }

    public boolean isRequiresPIN() {
        return requiresPIN;
    }

    public void setRequiresPIN(boolean requiresPIN) {
        this.requiresPIN = requiresPIN;
    }
}