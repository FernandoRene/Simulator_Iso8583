package com.iso8583.simulator.core.template.model;

/**
 * Categorías para organizar templates
 */
public enum TemplateCategory {

    PURCHASE("Purchase", "Compras y pagos"),
    CASH_ADVANCE("Cash Advance", "Avances de efectivo y retiros ATM"),
    BALANCE_INQUIRY("Balance Inquiry", "Consultas de saldo"),
    TRANSFER("Transfer", "Transferencias entre cuentas"),
    AUTHORIZATION("Authorization", "Autorizaciones del exterior"),
    REVERSAL("Reversal", "Reversas y anulaciones"),
    CUSTOM("Custom", "Templates personalizados"),
    TESTING("Testing", "Templates para pruebas");

    private final String displayName;
    private final String description;

    TemplateCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}