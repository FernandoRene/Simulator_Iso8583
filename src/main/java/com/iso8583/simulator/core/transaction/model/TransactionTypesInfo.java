package com.iso8583.simulator.core.transaction.model;

import java.util.List;
import java.util.Map;

/**
 * Información sobre los tipos de transacciones soportados
 * Incluye lista de tipos y detalles específicos de cada uno
 */
public class TransactionTypesInfo {
    private List<String> supportedTypes;
    private Map<String, TransactionTypeDetails> details;

    public TransactionTypesInfo() {}

    public TransactionTypesInfo(List<String> supportedTypes, Map<String, TransactionTypeDetails> details) {
        this.supportedTypes = supportedTypes;
        this.details = details;
    }

    // Getters y setters
    public List<String> getSupportedTypes() {
        return supportedTypes;
    }

    public void setSupportedTypes(List<String> supportedTypes) {
        this.supportedTypes = supportedTypes;
    }

    public Map<String, TransactionTypeDetails> getDetails() {
        return details;
    }

    public void setDetails(Map<String, TransactionTypeDetails> details) {
        this.details = details;
    }
}