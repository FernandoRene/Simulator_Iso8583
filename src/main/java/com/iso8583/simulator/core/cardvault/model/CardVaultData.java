package com.iso8583.simulator.core.cardvault.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Estructura raíz del Card Vault: lista de emisores, cada uno con sus BINs
 * y tarjetas. Este es el objeto que se persiste como JSON en el archivo
 * externo y el que se usa para exportar/importar.
 */
public class CardVaultData {

    private List<CardIssuer> issuers = new ArrayList<>();

    public CardVaultData() {
    }

    public List<CardIssuer> getIssuers() {
        return issuers;
    }

    public void setIssuers(List<CardIssuer> issuers) {
        this.issuers = (issuers != null) ? issuers : new ArrayList<>();
    }
}
