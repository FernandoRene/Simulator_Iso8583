package com.iso8583.simulator.core.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representa un campo del mensaje ISO8583
 * Modelo educativo con información descriptiva
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ISOField {

    private int fieldNumber;
    private String value;
    private String name;
    private String description;
    private String format;
    private Integer length;
    private String type; // "n", "an", "ans", "b"

    // Constructores
    public ISOField() {}

    public ISOField(int fieldNumber, String value) {
        this.fieldNumber = fieldNumber;
        this.value = value;
    }

    public ISOField(int fieldNumber, String value, String name, String description) {
        this.fieldNumber = fieldNumber;
        this.value = value;
        this.name = name;
        this.description = description;
    }

    // Builder pattern para facilidad de uso
    public static class Builder {
        private ISOField field = new ISOField();

        public Builder fieldNumber(int fieldNumber) {
            field.fieldNumber = fieldNumber;
            return this;
        }

        public Builder value(String value) {
            field.value = value;
            return this;
        }

        public Builder name(String name) {
            field.name = name;
            return this;
        }

        public Builder description(String description) {
            field.description = description;
            return this;
        }

        public Builder format(String format) {
            field.format = format;
            return this;
        }

        public Builder length(Integer length) {
            field.length = length;
            return this;
        }

        public Builder type(String type) {
            field.type = type;
            return this;
        }

        public ISOField build() {
            return field;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters y Setters
    public int getFieldNumber() { return fieldNumber; }
    public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {
        return String.format("Field %d: %s = %s", fieldNumber, name != null ? name : "Unknown", value);
    }
}