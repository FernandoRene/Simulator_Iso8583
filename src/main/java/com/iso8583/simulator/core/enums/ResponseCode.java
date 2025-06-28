package com.iso8583.simulator.core.enums;

public enum ResponseCode {
    APPROVED("00", "Approved or completed successfully"),
    REFER_TO_CARD_ISSUER("01", "Refer to card issuer"),
    REFER_TO_CARD_ISSUER_SPECIAL("02", "Refer to card issuer, special condition"),
    INVALID_MERCHANT("03", "Invalid merchant"),
    PICK_UP_CARD("04", "Pick up card"),
    DO_NOT_HONOR("05", "Do not honor"),
    ERROR("06", "Error"),
    PICK_UP_CARD_SPECIAL("07", "Pick up card, special condition"),
    HONOR_WITH_ID("08", "Honor with identification"),
    REQUEST_IN_PROGRESS("09", "Request in progress"),
    INVALID_TRANSACTION("12", "Invalid transaction"),
    INVALID_AMOUNT("13", "Invalid amount"),
    INVALID_CARD_NUMBER("14", "Invalid card number"),
    NO_SUCH_ISSUER("15", "No such issuer"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    NO_CHECKING_ACCOUNT("52", "No checking account"),
    NO_SAVINGS_ACCOUNT("53", "No savings account"),
    EXPIRED_CARD("54", "Expired card"),
    INCORRECT_PIN("55", "Incorrect PIN"),
    NO_CARD_RECORD("56", "No card record"),
    TRANSACTION_NOT_PERMITTED_TO_CARDHOLDER("57", "Transaction not permitted to cardholder"),
    TRANSACTION_NOT_PERMITTED_TO_TERMINAL("58", "Transaction not permitted to terminal"),
    SUSPECTED_FRAUD("59", "Suspected fraud"),
    CONTACT_ACQUIRER("60", "Contact acquirer"),
    EXCEEDS_WITHDRAWAL_AMOUNT_LIMIT("61", "Exceeds withdrawal amount limit"),
    RESTRICTED_CARD("62", "Restricted card"),
    SECURITY_VIOLATION("63", "Security violation"),
    ORIGINAL_AMOUNT_INCORRECT("64", "Original amount incorrect"),
    EXCEEDS_WITHDRAWAL_FREQUENCY_LIMIT("65", "Exceeds withdrawal frequency limit"),
    ALLOWABLE_PIN_TRIES_EXCEEDED("75", "Allowable number of PIN tries exceeded"),
    UNABLE_TO_LOCATE_RECORD("76", "Unable to locate record on file"),
    INCONSISTENT_WITH_REPEAT("77", "Previous message located for a repeat or reversal, but repeat or reversal data are inconsistent with original message"),
    BLOCKED_FIRST_USE("78", "Blocked, first use"),
    INVALID_CRYPTOGRAM("82", "Invalid cryptogram"),
    UNACCEPTABLE_PIN("83", "Unacceptable PIN"),
    CRYPTOGRAPHIC_FAILURE("84", "Cryptographic failure"),
    ISSUER_SYSTEM_INOPERATIVE("91", "Issuer system inoperative"),
    FINANCIAL_INSTITUTION_NOT_FOUND("92", "Financial institution or intermediate network facility cannot be found for routing"),
    TRANSACTION_CANNOT_BE_COMPLETED("93", "Transaction cannot be completed"),
    DUPLICATE_TRANSMISSION("94", "Duplicate transmission"),
    RECONCILE_ERROR("95", "Reconcile error"),
    SYSTEM_MALFUNCTION("96", "System malfunction"),
    RECONCILIATION_CUTOFF("97", "Reconciliation cutoff is in process"),
    MAC_ERROR("98", "MAC error"),
    RESERVED_FOR_NATIONAL_USE("99", "Reserved for national use");

    private final String code;
    private final String description;

    ResponseCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ResponseCode fromCode(String code) {
        for (ResponseCode responseCode : values()) {
            if (responseCode.code.equals(code)) {
                return responseCode;
            }
        }
        return null; // Return null for unknown codes
    }

    public boolean isSuccess() {
        return "00".equals(code);
    }
}