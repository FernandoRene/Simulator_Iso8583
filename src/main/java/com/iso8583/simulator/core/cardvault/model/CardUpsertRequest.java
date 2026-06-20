package com.iso8583.simulator.core.cardvault.model;

/**
 * Cuerpo de la petición POST /api/v1/card-vault/cards.
 *
 * Permite crear una tarjeta nueva o, si el PAN ya existe en otro
 * emisor/BIN, moverla a la ubicación indicada. Si issuerId/bin no se
 * especifican, se aplican valores por defecto (issuer "DEFAULT", bin
 * derivado de los primeros 6 dígitos del PAN).
 */
public class CardUpsertRequest {

    private String issuerId;
    private String issuerName;
    private String issuerDescription;
    private String bin;
    private String binDescription;
    private CardRecord card;

    public CardUpsertRequest() {
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

    public String getIssuerDescription() {
        return issuerDescription;
    }

    public void setIssuerDescription(String issuerDescription) {
        this.issuerDescription = issuerDescription;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getBinDescription() {
        return binDescription;
    }

    public void setBinDescription(String binDescription) {
        this.binDescription = binDescription;
    }

    public CardRecord getCard() {
        return card;
    }

    public void setCard(CardRecord card) {
        this.card = card;
    }
}
