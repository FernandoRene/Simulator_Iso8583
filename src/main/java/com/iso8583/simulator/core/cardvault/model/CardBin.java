package com.iso8583.simulator.core.cardvault.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa tarjetas bajo un BIN (rango de numeración) dentro de un emisor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardBin {

    /** BIN (típicamente los primeros 6 dígitos del PAN). */
    private String bin;

    /** Descripción libre (ej. "Tarjetas de débito"). */
    private String description;

    private List<CardRecord> cards = new ArrayList<>();

    public CardBin() {
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CardRecord> getCards() {
        return cards;
    }

    public void setCards(List<CardRecord> cards) {
        this.cards = (cards != null) ? cards : new ArrayList<>();
    }
}
