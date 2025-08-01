package com.iso8583.simulator.core.transaction.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos para requests de transacciones
 * Contiene toda la información necesaria para construir un mensaje ISO 8583
 * EXTENDIDO: Campos adicionales para Transfer y Authorization
 */
public class TransactionRequest {
    // Campos básicos existentes
    private String transactionType;
    private String pan;
    private String track2;
    private String amount;
    private String terminalId;
    private String cardAcceptorId;
    private String cardAcceptorName;
    private String currencyCode;
    private String account;
    private String pin;
    private Map<String, String> additionalFields;

    // Campos nuevos para Transfer y Authorization
    private String processingCode;          // Campo 3 - Processing Code
    private String targetAccount;           // Campo 103 - Account Identification 2 (destino)
    private String merchantType;            // Campo 18 - Merchant Type
    private String acquiringCountry;        // Campo 19 - Acquiring Institution Country Code
    private String posEntryMode;            // Campo 22 - POS Entry Mode
    private String acquiringInstitution;    // Campo 32 - Acquiring Institution ID
    private String billingAmount;           // Campo 6 - Cardholder Billing Amount
    private String billingCurrency;         // Campo 51 - Cardholder Billing Currency
    private String pinData;                 // Campo 52 - PIN Data
    private String privateUseFields;        // Campo 62 - Private Use Fields

    public TransactionRequest() {
        this.additionalFields = new HashMap<>();
    }

    // Factory methods existentes
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

    // NUEVOS Factory methods para Transfer y Authorization

    /**
     * Factory method para transferencias
     */
    public static TransactionRequest transfer(String pan, String track2, String amount,
                                              String terminalId, String cardAcceptorId,
                                              String sourceAccount, String targetAccount,
                                              String transferType) {
        TransactionRequest request = new TransactionRequest();
        request.transactionType = "TRANSFER";
        request.pan = pan;
        request.track2 = track2;
        request.amount = amount;
        request.terminalId = terminalId;
        request.cardAcceptorId = cardAcceptorId;
        request.account = sourceAccount;
        request.targetAccount = targetAccount;
        request.currencyCode = "068";
        request.cardAcceptorName = "SIMULADOR TRANSFER";

        // Determinar processing code según tipo de transferencia
        switch (transferType.toUpperCase()) {
            case "ACH":
                request.processingCode = "400020";
                break;
            case "OWN_ACCOUNT":
                request.processingCode = "400040";
                break;
            case "AFFILIATED":
                request.processingCode = "400060";
                break;
            case "NEW_THIRD_PARTY":
                request.processingCode = "400080";
                break;
            default:
                request.processingCode = "400040"; // Default a cuentas propias
        }

        return request;
    }

    /**
     * Factory method para autorizaciones
     */
    public static TransactionRequest authorization(String pan, String track2, String amount,
                                                   String terminalId, String cardAcceptorId,
                                                   String authType) {
        TransactionRequest request = new TransactionRequest();
        request.transactionType = "AUTHORIZATION";
        request.pan = pan;
        request.track2 = track2;
        request.amount = amount;
        request.terminalId = terminalId;
        request.cardAcceptorId = cardAcceptorId;
        request.currencyCode = "068";
        request.cardAcceptorName = "SIMULADOR AUTHORIZATION";

        // Configurar según tipo de autorización
        switch (authType.toUpperCase()) {
            case "PURCHASE":
            case "FOREIGN_PURCHASE":
                request.processingCode = "000000";
                request.merchantType = "5999";
                request.posEntryMode = "010";
                if (authType.equals("FOREIGN_PURCHASE")) {
                    request.acquiringCountry = "840"; // USA por defecto
                }
                break;
            case "ATM_WITHDRAWAL":
            case "EXTERNAL_ATM":
                request.processingCode = "010000";
                request.merchantType = "6011";
                request.posEntryMode = "051";
                break;
            default:
                request.processingCode = "000000"; // Default a compra
                request.merchantType = "5999";
                request.posEntryMode = "010";
        }

        return request;
    }

    /**
     * Factory method para transferencia ACH específica
     */
    public static TransactionRequest achTransfer(String pan, String track2, String amount,
                                                 String terminalId, String cardAcceptorId,
                                                 String sourceAccount, String targetBankCode) {
        TransactionRequest request = transfer(pan, track2, amount, terminalId, cardAcceptorId,
                sourceAccount, targetBankCode, "ACH");
        request.cardAcceptorName = "SIMULADOR ACH TRANSFER";
        return request;
    }

    /**
     * Factory method para compra extranjera específica
     */
    public static TransactionRequest foreignPurchase(String pan, String track2, String amount,
                                                     String terminalId, String cardAcceptorId,
                                                     String countryCode) {
        TransactionRequest request = authorization(pan, track2, amount, terminalId, cardAcceptorId,
                "FOREIGN_PURCHASE");
        request.acquiringCountry = countryCode;
        request.cardAcceptorName = "SIMULADOR FOREIGN PURCHASE";
        return request;
    }

    // ============================================================================
    // GETTERS Y SETTERS - Campos existentes
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

    // ============================================================================
    // GETTERS Y SETTERS - Campos nuevos
    // ============================================================================

    public String getProcessingCode() { return processingCode; }
    public void setProcessingCode(String processingCode) { this.processingCode = processingCode; }

    public String getTargetAccount() { return targetAccount; }
    public void setTargetAccount(String targetAccount) { this.targetAccount = targetAccount; }

    public String getMerchantType() { return merchantType; }
    public void setMerchantType(String merchantType) { this.merchantType = merchantType; }

    public String getAcquiringCountry() { return acquiringCountry; }
    public void setAcquiringCountry(String acquiringCountry) { this.acquiringCountry = acquiringCountry; }

    public String getPosEntryMode() { return posEntryMode; }
    public void setPosEntryMode(String posEntryMode) { this.posEntryMode = posEntryMode; }

    public String getAcquiringInstitution() { return acquiringInstitution; }
    public void setAcquiringInstitution(String acquiringInstitution) { this.acquiringInstitution = acquiringInstitution; }

    public String getBillingAmount() { return billingAmount; }
    public void setBillingAmount(String billingAmount) { this.billingAmount = billingAmount; }

    public String getBillingCurrency() { return billingCurrency; }
    public void setBillingCurrency(String billingCurrency) { this.billingCurrency = billingCurrency; }

    public String getPinData() { return pinData; }
    public void setPinData(String pinData) { this.pinData = pinData; }

    public String getPrivateUseFields() { return privateUseFields; }
    public void setPrivateUseFields(String privateUseFields) { this.privateUseFields = privateUseFields; }

    // ============================================================================
    // MÉTODOS UTILITARIOS
    // ============================================================================

    /**
     * Genera un transaction ID único
     */
    public String getTransactionId() {
        return String.format("%s_%d",
                transactionType != null ? transactionType : "UNKNOWN",
                System.currentTimeMillis());
    }

    /**
     * Verifica si la transacción es de transferencia
     */
    public boolean isTransfer() {
        return "TRANSFER".equals(transactionType);
    }

    /**
     * Verifica si la transacción es de autorización
     */
    public boolean isAuthorization() {
        return "AUTHORIZATION".equals(transactionType);
    }

    /**
     * Obtiene el código de país por defecto
     */
    public String getDefaultCountryCode() {
        return acquiringCountry != null ? acquiringCountry : "068";
    }
}