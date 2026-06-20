package com.iso8583.simulator.core.cardvault.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una entidad/emisor genérico (ej. un banco del sistema o una
 * procesadora) bajo el cual se agrupan BINs y tarjetas.
 *
 * No hay nada específico de una entidad en particular: el "id" es un texto
 * libre definido por quien administra el Card Vault (ej. "BANCO_DEMO",
 * "PROCESADORA_X"). Si no se especifica, se usa "DEFAULT".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardIssuer {

    private String id;
    private String name;
    private String description;
    private List<CardBin> bins = new ArrayList<>();

    public CardIssuer() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CardBin> getBins() {
        return bins;
    }

    public void setBins(List<CardBin> bins) {
        this.bins = (bins != null) ? bins : new ArrayList<>();
    }
}
