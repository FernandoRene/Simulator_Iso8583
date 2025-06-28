package com.iso8583.simulator.core.message;

import com.iso8583.simulator.core.enums.MessageType;
import com.iso8583.simulator.core.enums.ResponseCode;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ISO8583MessageParser {

    private GenericPackager packager;

    public void setPackager(GenericPackager packager) {
        this.packager = packager;
    }

    public ISOMsg parseMessage(byte[] messageData) throws ISOException {
        ISOMsg message = new ISOMsg();
        message.setPackager(packager);
        message.unpack(messageData);
        return message;
    }

    public Map<String, Object> parseToMap(ISOMsg message) throws ISOException {
        Map<String, Object> result = new HashMap<>();

        // Basic message info
        result.put("mti", message.getMTI());
        result.put("messageType", getMessageTypeDescription(message.getMTI()));

        // Parse all fields
        Map<Integer, String> fields = new HashMap<>();
        for (int i = 0; i <= 128; i++) {
            if (message.hasField(i)) {
                fields.put(i, message.getString(i));
            }
        }
        result.put("fields", fields);

        // Parse response code if present
        if (message.hasField(39)) {
            String responseCode = message.getString(39);
            ResponseCode code = ResponseCode.fromCode(responseCode);
            result.put("responseCode", responseCode);
            result.put("responseDescription", code != null ? code.getDescription() : "Unknown response code");
            result.put("isSuccess", code != null && code.isSuccess());
        }

        // Extract common transaction info
        extractTransactionInfo(message, result);

        return result;
    }

    private void extractTransactionInfo(ISOMsg message, Map<String, Object> result) throws ISOException {
        Map<String, String> transactionInfo = new HashMap<>();

        if (message.hasField(2)) transactionInfo.put("pan", maskPan(message.getString(2)));
        if (message.hasField(3)) transactionInfo.put("processingCode", message.getString(3));
        if (message.hasField(4)) transactionInfo.put("amount", message.getString(4));
        if (message.hasField(7)) transactionInfo.put("transmissionDateTime", message.getString(7));
        if (message.hasField(11)) transactionInfo.put("stan", message.getString(11));
        if (message.hasField(12)) transactionInfo.put("localTime", message.getString(12));
        if (message.hasField(13)) transactionInfo.put("localDate", message.getString(13));
        if (message.hasField(37)) transactionInfo.put("rrn", message.getString(37));
        if (message.hasField(41)) transactionInfo.put("terminalId", message.getString(41));
        if (message.hasField(42)) transactionInfo.put("cardAcceptorId", message.getString(42));
        if (message.hasField(43)) transactionInfo.put("cardAcceptorName", message.getString(43));

        result.put("transactionInfo", transactionInfo);
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) {
            return pan;
        }
        // Mask middle digits, keep first 6 and last 4
        String first6 = pan.substring(0, 6);
        String last4 = pan.substring(pan.length() - 4);
        String masked = "*".repeat(pan.length() - 10);
        return first6 + masked + last4;
    }

    private String getMessageTypeDescription(String mti) {
        try {
            MessageType messageType = MessageType.fromMti(mti);
            return messageType.getDescription();
        } catch (IllegalArgumentException e) {
            return "Unknown message type: " + mti;
        }
    }

    public boolean isSuccessResponse(ISOMsg message) throws ISOException {
        if (!message.hasField(39)) {
            return false;
        }

        String responseCode = message.getString(39);
        ResponseCode code = ResponseCode.fromCode(responseCode);
        return code != null && code.isSuccess();
    }

    public String getResponseDescription(ISOMsg message) throws ISOException {
        if (!message.hasField(39)) {
            return "No response code present";
        }

        String responseCode = message.getString(39);
        ResponseCode code = ResponseCode.fromCode(responseCode);
        return code != null ? code.getDescription() : "Unknown response code: " + responseCode;
    }
}