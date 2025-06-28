package com.iso8583.simulator.core.enums;

public enum FieldGenerationType {
    STATIC("Static value from configuration"),
    DYNAMIC("Generated dynamically based on current time/date"),
    SEQUENTIAL("Sequential numbering with optional reset"),
    TEMPLATE("Composite field using template with variables"),
    REFERENCE("Reference to another field's value"),
    CSV_OVERRIDE("Value from CSV data takes precedence");

    private final String description;

    FieldGenerationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}