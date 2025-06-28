package com.iso8583.simulator.core.config;

import com.iso8583.simulator.core.enums.FieldGenerationType;
import java.util.Map;

public class FieldGenerationConfig {
    private FieldGenerationType type;
    private String format;
    private String value;
    private String template;
    private Integer startValue;
    private Integer increment;
    private Boolean resetDaily;
    private Integer maxLength;
    private String description;
    private Integer fieldReference;
    private Map<String, ComponentConfig> components;

    // Constructors
    public FieldGenerationConfig() {}

    public FieldGenerationConfig(FieldGenerationType type, String value) {
        this.type = type;
        this.value = value;
    }

    // Getters and Setters
    public FieldGenerationType getType() { return type; }
    public void setType(FieldGenerationType type) { this.type = type; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public Integer getStartValue() { return startValue; }
    public void setStartValue(Integer startValue) { this.startValue = startValue; }

    public Integer getIncrement() { return increment; }
    public void setIncrement(Integer increment) { this.increment = increment; }

    public Boolean getResetDaily() { return resetDaily; }
    public void setResetDaily(Boolean resetDaily) { this.resetDaily = resetDaily; }

    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getFieldReference() { return fieldReference; }
    public void setFieldReference(Integer fieldReference) { this.fieldReference = fieldReference; }

    public Map<String, ComponentConfig> getComponents() { return components; }
    public void setComponents(Map<String, ComponentConfig> components) { this.components = components; }

    public static class ComponentConfig {
        private FieldGenerationType type;
        private String format;
        private Integer field;

        // Getters and Setters
        public FieldGenerationType getType() { return type; }
        public void setType(FieldGenerationType type) { this.type = type; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public Integer getField() { return field; }
        public void setField(Integer field) { this.field = field; }
    }
}