package com.iso8583.simulator.core.field.model;

import java.util.List;

/**
 * Metadata completa de un campo ISO8583
 * Incluye información educativa y de validación
 */
public class FieldMetadata {

    private int fieldNumber;
    private String name;
    private String description;
    private FieldType type;
    private int maxLength;
    private boolean required;
    private List<String> validationRules;
    private String example;
    private String educationalNote;
    private List<String> allowedValues; // Para campos con valores fijos

    // Constructor
    public FieldMetadata(int fieldNumber, String name, String description,
                         FieldType type, int maxLength) {
        this.fieldNumber = fieldNumber;
        this.name = name;
        this.description = description;
        this.type = type;
        this.maxLength = maxLength;
        this.required = false;
    }

    // Getters y Setters
    public int getFieldNumber() { return fieldNumber; }
    public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public FieldType getType() { return type; }
    public void setType(FieldType type) { this.type = type; }

    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public List<String> getValidationRules() { return validationRules; }
    public void setValidationRules(List<String> validationRules) {
        this.validationRules = validationRules;
    }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String getEducationalNote() { return educationalNote; }
    public void setEducationalNote(String educationalNote) {
        this.educationalNote = educationalNote;
    }

    public List<String> getAllowedValues() { return allowedValues; }
    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    /**
     * Tipos de campo ISO8583
     */
    public enum FieldType {
        NUMERIC("n", "Solo dígitos numéricos"),
        ALPHA("a", "Solo caracteres alfabéticos"),
        ALPHANUMERIC("an", "Caracteres alfanuméricos"),
        SPECIAL("ans", "Alfanumérico + caracteres especiales"),
        BINARY("b", "Datos binarios");

        private final String code;
        private final String description;

        FieldType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
}