package com.iso8583.simulator.core.validation;

import java.util.List;

/**
 * Representa una regla de validación configurable
 */
public class ValidationRule {

    private String ruleName;
    private String description;
    private RuleType type;
    private String pattern; // Para regex
    private Integer minValue;
    private Integer maxValue;
    private Integer exactLength;
    private Integer minLength;
    private Integer maxLength;
    private List<String> allowedValues;
    private boolean required;
    private String errorMessage;
    private String educationalHint;

    // Constructor
    public ValidationRule(String ruleName, RuleType type) {
        this.ruleName = ruleName;
        this.type = type;
        this.required = false;
    }

    // Getters y Setters
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RuleType getType() { return type; }
    public void setType(RuleType type) { this.type = type; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public Integer getMinValue() { return minValue; }
    public void setMinValue(Integer minValue) { this.minValue = minValue; }

    public Integer getMaxValue() { return maxValue; }
    public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }

    public Integer getExactLength() { return exactLength; }
    public void setExactLength(Integer exactLength) { this.exactLength = exactLength; }

    public Integer getMinLength() { return minLength; }
    public void setMinLength(Integer minLength) { this.minLength = minLength; }

    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

    public List<String> getAllowedValues() { return allowedValues; }
    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getEducationalHint() { return educationalHint; }
    public void setEducationalHint(String educationalHint) {
        this.educationalHint = educationalHint;
    }

    /**
     * Tipos de reglas de validación
     */
    public enum RuleType {
        REGEX,          // Validación por expresión regular
        LENGTH,         // Validación de longitud
        RANGE,          // Validación de rango numérico
        LUHN,           // Validación de algoritmo Luhn
        ENUM,           // Validación contra lista de valores permitidos
        NUMERIC,        // Solo números
        ALPHA,          // Solo letras
        ALPHANUMERIC,   // Letras y números
        CUSTOM          // Validación personalizada
    }
}