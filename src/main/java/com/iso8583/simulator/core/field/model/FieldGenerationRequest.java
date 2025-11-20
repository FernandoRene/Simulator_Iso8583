package com.iso8583.simulator.core.field.model;

import java.util.List;

/**
 * Request para generar campos automáticamente
 */
public class FieldGenerationRequest {

    private List<Integer> fieldsToGenerate;
    private GenerationMode mode;
    private String baseValue; // Para generación basada en valor

    // Constructor
    public FieldGenerationRequest() {
        this.mode = GenerationMode.STANDARD;
    }

    // Getters y Setters
    public List<Integer> getFieldsToGenerate() { return fieldsToGenerate; }
    public void setFieldsToGenerate(List<Integer> fieldsToGenerate) {
        this.fieldsToGenerate = fieldsToGenerate;
    }

    public GenerationMode getMode() { return mode; }
    public void setMode(GenerationMode mode) { this.mode = mode; }

    public String getBaseValue() { return baseValue; }
    public void setBaseValue(String baseValue) { this.baseValue = baseValue; }

    /**
     * Modos de generación
     */
    public enum GenerationMode {
        STANDARD,    // Valores estándar según especificación
        RANDOM,      // Valores aleatorios válidos
        SEQUENTIAL,  // Valores secuenciales (STAN, RRN)
        CUSTOM       // Basado en baseValue
    }
}