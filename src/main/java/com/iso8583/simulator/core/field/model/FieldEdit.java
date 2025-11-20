package com.iso8583.simulator.core.field.model;

/**
 * Request para editar un campo en un mensaje ISO8583
 */
public class FieldEdit {

    private int fieldNumber;
    private String newValue;
    private boolean validateOnly; // Solo validar, no aplicar

    // Constructor vacío para Jackson
    public FieldEdit() {}

    public FieldEdit(int fieldNumber, String newValue) {
        this.fieldNumber = fieldNumber;
        this.newValue = newValue;
        this.validateOnly = false;
    }

    // Getters y Setters
    public int getFieldNumber() { return fieldNumber; }
    public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public boolean isValidateOnly() { return validateOnly; }
    public void setValidateOnly(boolean validateOnly) { this.validateOnly = validateOnly; }
}