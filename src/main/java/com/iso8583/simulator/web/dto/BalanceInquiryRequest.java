package com.iso8583.simulator.web.dto;


public class BalanceInquiryRequest {
    private String pan;
    private String track2;
    private String terminalId;
    private String cardAcceptorId;
    private String account;

    // Constructores, getters y setters
    public BalanceInquiryRequest() {}

    public BalanceInquiryRequest(String pan, String track2, String terminalId,
                                 String cardAcceptorId, String account) {
        this.pan = pan;
        this.track2 = track2;
        this.terminalId = terminalId;
        this.cardAcceptorId = cardAcceptorId;
        this.account = account;
    }

    // Getters y Setters
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getTrack2() { return track2; }
    public void setTrack2(String track2) { this.track2 = track2; }

    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

    public String getCardAcceptorId() { return cardAcceptorId; }
    public void setCardAcceptorId(String cardAcceptorId) { this.cardAcceptorId = cardAcceptorId; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
}