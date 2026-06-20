package com.iso8583.simulator.core.cardvault.model;

/**
 * DTO de respuesta: una tarjeta junto con la información de dónde está
 * ubicada dentro del Card Vault (emisor y BIN). Útil para que el frontend
 * muestre contexto sin tener que recorrer todo el árbol.
 */
public class CardLocation {

    private String issuerId;
    private String issuerName;
    private String bin;
    private CardRecord card;

    public CardLocation() {
    }

    public CardLocation(String issuerId, String issuerName, String bin, CardRecord card) {
        this.issuerId = issuerId;
        this.issuerName = issuerName;
        this.bin = bin;
        this.card = card;
    }

    public String getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public CardRecord getCard() {
        return card;
    }

    public void setCard(CardRecord card) {
        this.card = card;
    }
}
