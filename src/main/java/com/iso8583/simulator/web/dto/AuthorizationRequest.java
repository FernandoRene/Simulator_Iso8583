package com.iso8583.simulator.web.dto;

/**
 * DTO para requests de autorizaciones
 */
public class AuthorizationRequest {
    private String pan;
    private String track2;
    private String amount;
    private String terminalId;
    private String cardAcceptorId;
    private String cardAcceptorName;
    private String authorizationType; // PURCHASE, ATM_WITHDRAWAL, FOREIGN_PURCHASE, EXTERNAL_ATM
    private String processingCode; // Opcional, se determina automáticamente
    private String merchantType; // Campo 18
    private String acquiringCountry; // Campo 19
    private String posEntryMode; // Campo 22
    private String billingAmount; // Campo 6
    private String billingCurrency; // Campo 51
    private String privateUseFields; // Campo 62

    // Constructor por defecto
    public AuthorizationRequest() {}

    // Constructor básico
    public AuthorizationRequest(String pan, String track2, String amount, String terminalId,
                                String cardAcceptorId, String authorizationType) {
        this.pan = pan;
        this.track2 = track2;
        this.amount = amount;
        this.terminalId = terminalId;
        this.cardAcceptorId = cardAcceptorId;
        this.authorizationType = authorizationType;
    }

    // Getters y Setters
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

    public String getAuthorizationType() { return authorizationType; }
    public void setAuthorizationType(String authorizationType) { this.authorizationType = authorizationType; }

    public String getProcessingCode() { return processingCode; }
    public void setProcessingCode(String processingCode) { this.processingCode = processingCode; }

    public String getMerchantType() { return merchantType; }
    public void setMerchantType(String merchantType) { this.merchantType = merchantType; }

    public String getAcquiringCountry() { return acquiringCountry; }
    public void setAcquiringCountry(String acquiringCountry) { this.acquiringCountry = acquiringCountry; }

    public String getPosEntryMode() { return posEntryMode; }
    public void setPosEntryMode(String posEntryMode) { this.posEntryMode = posEntryMode; }

    public String getBillingAmount() { return billingAmount; }
    public void setBillingAmount(String billingAmount) { this.billingAmount = billingAmount; }

    public String getBillingCurrency() { return billingCurrency; }
    public void setBillingCurrency(String billingCurrency) { this.billingCurrency = billingCurrency; }

    public String getPrivateUseFields() { return privateUseFields; }
    public void setPrivateUseFields(String privateUseFields) { this.privateUseFields = privateUseFields; }
}