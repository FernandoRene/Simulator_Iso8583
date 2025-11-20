package com.iso8583.simulator.core.transaction;

/**
 * Enum para tipos de transacción soportados
 * ACTUALIZADO: Processing codes corregidos según documentación ISO 8583
 */
public enum TransactionType {
    BALANCE_INQUIRY("Balance Inquiry", "301099", "0200"),
    CASH_ADVANCE("Cash Advance", "011099", "0200"),
    PURCHASE("Purchase", "000000", "0200"),        // CORREGIDO: era "001099"
    TRANSFER("Transfer", "400040", "0200"),         // CORREGIDO: era "401099"
    AUTHORIZATION("Authorization", "000000", "0100"), // CORREGIDO: era "001099"
    REVERSAL("Reversal", "901099", "0420");

    private final String displayName;
    private final String processingCode;
    private final String mti;

    TransactionType(String displayName, String processingCode, String mti) {
        this.displayName = displayName;
        this.processingCode = processingCode;
        this.mti = mti;
    }

    public String getDisplayName() { return displayName; }
    public String getProcessingCode() { return processingCode; }
    public String getMti() { return mti; }

    /**
     * Obtiene el TransactionType por processing code
     */
    public static TransactionType fromProcessingCode(String processingCode) {
        for (TransactionType type : values()) {
            if (type.getProcessingCode().equals(processingCode)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Verifica si un processing code es válido para transferencias
     */
    public static boolean isTransferCode(String processingCode) {
        return processingCode != null && processingCode.startsWith("400");
    }

    /**
     * Verifica si un processing code es válido para autorizaciones
     */
    public static boolean isAuthorizationCode(String processingCode) {
        return processingCode != null &&
                (processingCode.startsWith("00") || processingCode.startsWith("01"));
    }
}