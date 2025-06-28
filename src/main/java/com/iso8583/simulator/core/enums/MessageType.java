package com.iso8583.simulator.core.enums;

public enum MessageType {
    AUTHORIZATION_REQUEST("0100", "Authorization Request"),
    AUTHORIZATION_RESPONSE("0110", "Authorization Response"),
    FINANCIAL_REQUEST("0200", "Financial Transaction Request"),
    FINANCIAL_RESPONSE("0210", "Financial Transaction Response"),
    REVERSAL_REQUEST("0400", "Reversal Request"),
    REVERSAL_RESPONSE("0410", "Reversal Response"),
    NETWORK_MANAGEMENT("0800", "Network Management Request"),
    NETWORK_MANAGEMENT_RESPONSE("0810", "Network Management Response");

    private final String mti;
    private final String description;

    MessageType(String mti, String description) {
        this.mti = mti;
        this.description = description;
    }

    public String getMti() {
        return mti;
    }

    public String getDescription() {
        return description;
    }

    public static MessageType fromMti(String mti) {
        for (MessageType type : values()) {
            if (type.mti.equals(mti)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MTI: " + mti);
    }
}