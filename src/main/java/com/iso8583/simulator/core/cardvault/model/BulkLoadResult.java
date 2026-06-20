package com.iso8583.simulator.core.cardvault.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de una operación de carga masiva (JSON o CSV) sobre el Card Vault.
 */
public class BulkLoadResult {

    private int created;
    private int updated;
    private List<String> errors = new ArrayList<>();

    public void incrementCreated() {
        created++;
    }

    public void incrementUpdated() {
        updated++;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getTotalProcessed() {
        return created + updated;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
