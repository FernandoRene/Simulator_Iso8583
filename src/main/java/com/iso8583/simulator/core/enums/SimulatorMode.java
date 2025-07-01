package com.iso8583.simulator.core.enums;

/**
 * Modos de operación del simulador
 */
public enum SimulatorMode {
    /**
     * Modo mock - respuestas simuladas sin conexión real
     */
    MOCK("mock", "Modo Mock - Respuestas simuladas"),

    /**
     * Modo real - conexión directa al autorizador
     */
    REAL("real", "Modo Real - Conexión al autorizador"),

    /**
     * Modo híbrido - permite alternar dinámicamente
     */
    HYBRID("hybrid", "Modo Híbrido - Alternancia dinámica");

    private final String code;
    private final String description;

    SimulatorMode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SimulatorMode fromCode(String code) {
        for (SimulatorMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Modo de simulador inválido: " + code);
    }

    public boolean isMockEnabled() {
        return this == MOCK || this == HYBRID;
    }

    public boolean isRealEnabled() {
        return this == REAL || this == HYBRID;
    }
}