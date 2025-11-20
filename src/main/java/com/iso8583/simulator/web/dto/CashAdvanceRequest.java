package com.iso8583.simulator.web.dto;

/**
 * DTO para requests de Cash Advance desde el frontend
 */
public class CashAdvanceRequest {
    private String pan;
    private String track2;
    private String amount;
    private String terminalId;
    private String cardAcceptorId;
    private String cardAcceptorName;
    private String currencyCode = "068";

    // Constructors
    public CashAdvanceRequest() {}

    public CashAdvanceRequest(String pan, String track2, String amount,
                              String terminalId, String cardAcceptorId) {
        this.pan = pan;
        this.track2 = track2;
        this.amount = amount;
        this.terminalId = terminalId;
        this.cardAcceptorId = cardAcceptorId;
        this.cardAcceptorName = "SIMULADOR CASH ADV";
    }

    // Getters y setters
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
}