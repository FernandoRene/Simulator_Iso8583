package com.iso8583.simulator.core.cardvault.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representa una tarjeta simulada dentro del Card Vault.
 *
 * El único campo verdaderamente obligatorio es {@code pan}; el resto son
 * opcionales para permitir cargas parciales (alta manual rápida, CSV con
 * columnas vacías, etc.).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardRecord {

    /** Obligatorio. Número de tarjeta (PAN), también usado como identificador único. */
    private String pan;

    /** Track2 completo (PAN=YYMMSSS...), útil para armar field 35. */
    private String track2;

    /** Fecha de vencimiento en formato YYMM (igual convención que field 14). */
    private String expirationDate;

    /** CVV2 (referencial, no viaja en la mensajería ISO8583 estándar). */
    private String cvv2;

    /**
     * Bloque de PIN (field 52), como string hexadecimal (ej. 16 chars = 8 bytes).
     * No se calcula ni se desencripta: se almacena y se envía/recibe tal cual.
     */
    private String pinBlock;

    /** Saldo disponible simulado (en la unidad mínima de la moneda, ej. centavos). Útil para modo Emisor. */
    private Long balance;

    /** Código de moneda asociado al saldo (ej. "068"). */
    private String currencyCode;

    /** Estado simulado de la tarjeta. Por defecto ACTIVE. */
    private CardStatus status = CardStatus.ACTIVE;

    /** Nombre del tarjetahabiente (opcional, solo referencial). */
    private String cardholderName;

    /** Notas libres para identificar el propósito de la tarjeta de prueba. */
    private String notes;

    public CardRecord() {
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getTrack2() {
        return track2;
    }

    public void setTrack2(String track2) {
        this.track2 = track2;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getCvv2() {
        return cvv2;
    }

    public void setCvv2(String cvv2) {
        this.cvv2 = cvv2;
    }

    public String getPinBlock() {
        return pinBlock;
    }

    public void setPinBlock(String pinBlock) {
        this.pinBlock = pinBlock;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
