package com.iso8583.simulator.core.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enum para tipos de transacción soportados
 */
public enum TransactionType {
    BALANCE_INQUIRY("Balance Inquiry", "301099", "0200"),
    CASH_ADVANCE("Cash Advance", "011099", "0200"),
    PURCHASE("Purchase", "001099", "0200"),
    TRANSFER("Transfer", "401099", "0200"),
    AUTHORIZATION("Authorization", "001099", "0100"),
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
}