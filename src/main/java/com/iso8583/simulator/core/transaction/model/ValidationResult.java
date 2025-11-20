package com.iso8583.simulator.core.transaction.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private List<String> appliedValidations;

    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.appliedValidations = new ArrayList<>();
        this.valid = true;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public void addValidation(String validation) {
        this.appliedValidations.add(validation);
    }

    // Getters y setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getAppliedValidations() { return appliedValidations; }
    public void setAppliedValidations(List<String> appliedValidations) { this.appliedValidations = appliedValidations; }
}