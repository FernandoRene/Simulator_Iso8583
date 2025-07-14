package com.iso8583.simulator.core.transaction.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos para requests de transacciones
 * Contiene toda la información necesaria para construir un mensaje ISO 8583
 */
public class TransactionRequest {
    private String transactionType;
    private String pan;
    private String track2;
    private String amount;
    private String terminalId;
    private String cardAcceptorId;
    private String cardAcceptorName;
    private String currencyCode;
    private String account;
    private String pin; // Opcional
    private Map<String, String> additionalFields;

    public TransactionRequest() {
        this.additionalFields = new HashMap<>();
    }

    // Factory methods para crear requests específicos
    public static TransactionRequest cashAdvance(String pan, String track2, String amount,
                                                 String terminalId, String cardAcceptorId) {
        TransactionRequest request = new TransactionRequest();
        request.transactionType = "CASH_ADVANCE";
        request.pan = pan;
        request.track2 = track2;
        request.amount = amount;
        request.terminalId = terminalId;
        request.cardAcceptorId = cardAcceptorId;
        request.currencyCode = "068"; // Default BOB
        request.cardAcceptorName = "SIMULADOR CASH ADVANCE";
        return request;
    }

    public static TransactionRequest purchase(String pan, String track2, String amount,
                                              String terminalId, String cardAcceptorId) {
        TransactionRequest request = new TransactionRequest();
        request.transactionType = "PURCHASE";
        request.pan = pan;
        request.track2 = track2;
        request.amount = amount;
        request.terminalId = terminalId;
        request.cardAcceptorId = cardAcceptorId;
        request.currencyCode = "068"; // Default BOB
        request.cardAcceptorName = "SIMULADOR PURCHASE";
        return request;
    }

    public static TransactionRequest balanceInquiry(String pan, String track2,
                                                    String terminalId, String cardAcceptorId, String account) {
        TransactionRequest request = new TransactionRequest();
        request.transactionType = "BALANCE_INQUIRY";
        request.pan = pan;
        request.track2 = track2;
        request.amount = "000000000000"; // Sin monto para balance inquiry
        request.terminalId = terminalId;
        request.cardAcceptorId = cardAcceptorId;
        request.account = account;
        request.currencyCode = "068";
        request.cardAcceptorName = "SIMULADOR BALANCE INQ";
        return request;
    }

    // ============================================================================
    // GETTERS Y SETTERS
    // ============================================================================

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getTrack2() { return track2; }
    public void setTrack2(String track2) { this.track2 = track2; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

    public String getCardAcceptorId() { return cardAcceptorId; }
    public void setCardAcceptorId(String cardAcceptorId) { this.cardAcceptorId = cardAcceptorId; }

    public String getCardAcceptorName() { return cardAcceptorName; }
    public void setCardAcceptorName(String cardAcceptorName) { this.cardAcceptorName = cardAcceptorName; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public Map<String, String> getAdditionalFields() { return additionalFields; }
    public void setAdditionalFields(Map<String, String> additionalFields) { this.additionalFields = additionalFields; }
}