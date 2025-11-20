package com.iso8583.simulator.web.dto;

/**
 * DTO para requests de transferencias
 */
public class TransferRequest {
    private String pan;
    private String track2;
    private String amount;
    private String terminalId;
    private String cardAcceptorId;
    private String cardAcceptorName;
    private String sourceAccount;
    private String targetAccount;
    private String transferType; // ACH, OWN_ACCOUNT, AFFILIATED, NEW_THIRD_PARTY
    private String targetBankCode; // Para ACH
    private String processingCode; // Opcional, se determina automáticamente si no se proporciona

    // Constructor por defecto
    public TransferRequest() {}

    // Constructor completo
    public TransferRequest(String pan, String track2, String amount, String terminalId,
                           String cardAcceptorId, String sourceAccount, String targetAccount,
                           String transferType) {
        this.pan = pan;
        this.track2 = track2;
        this.amount = amount;
        this.terminalId = terminalId;
        this.cardAcceptorId = cardAcceptorId;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.transferType = transferType;
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

    public String getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; }

    public String getTargetAccount() { return targetAccount; }
    public void setTargetAccount(String targetAccount) { this.targetAccount = targetAccount; }

    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }

    public String getTargetBankCode() { return targetBankCode; }
    public void setTargetBankCode(String targetBankCode) { this.targetBankCode = targetBankCode; }

    public String getProcessingCode() { return processingCode; }
    public void setProcessingCode(String processingCode) { this.processingCode = processingCode; }
}