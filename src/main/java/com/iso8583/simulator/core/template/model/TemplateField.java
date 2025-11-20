package com.iso8583.simulator.core.template.model;

/**
 * Representa un campo dentro de un template
 * Incluye valor y configuración de generación
 */
public class TemplateField {

    private int fieldNumber;
    private String value;
    private FieldBehavior behavior;
    private String generationRule; // Regla para auto-generar
    private boolean editable; // Usuario puede modificar o es fijo
    private String description; // Descripción específica del template

    // Constructor
    public TemplateField() {
        this.behavior = FieldBehavior.STATIC;
        this.editable = true;
    }

    public TemplateField(int fieldNumber, String value) {
        this();
        this.fieldNumber = fieldNumber;
        this.value = value;
    }

    public TemplateField(int fieldNumber, String value, FieldBehavior behavior) {
        this(fieldNumber, value);
        this.behavior = behavior;
    }

    // Getters y Setters
    public int getFieldNumber() { return fieldNumber; }
    public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public FieldBehavior getBehavior() { return behavior; }
    public void setBehavior(FieldBehavior behavior) { this.behavior = behavior; }

    public String getGenerationRule() { return generationRule; }
    public void setGenerationRule(String generationRule) {
        this.generationRule = generationRule;
    }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Comportamiento del campo en el template
     */
    public enum FieldBehavior {
        STATIC,          // Valor fijo del template
        AUTO_GENERATE,   // Auto-generar cada vez (STAN, timestamp, etc)
        USER_INPUT,      // Usuario debe proporcionar valor
        OPTIONAL         // Campo opcional
    }
}