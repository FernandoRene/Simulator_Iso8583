package com.iso8583.simulator.core.message;

import com.iso8583.simulator.core.config.FieldGenerationConfig;
import com.iso8583.simulator.core.enums.MessageType;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ISO8583MessageBuilder {

    @Autowired
    private FieldGenerator fieldGenerator;

    private GenericPackager packager;
    private Map<Integer, FieldGenerationConfig> fieldConfigs;

    public ISO8583MessageBuilder() {
        this.fieldConfigs = new HashMap<>();
    }

    public void setPackager(GenericPackager packager) {
        this.packager = packager;
    }

    public void setFieldConfigs(Map<Integer, FieldGenerationConfig> fieldConfigs) {
        this.fieldConfigs = fieldConfigs;
    }

    public ISOMsg buildMessage(MessageType messageType, Map<String, String> csvData) throws ISOException {
        return buildMessage(messageType.getMti(), csvData);
    }

    public ISOMsg buildMessage(String mti, Map<String, String> csvData) throws ISOException {
        ISOMsg message = new ISOMsg();
        message.setPackager(packager);
        message.setMTI(mti);

        Map<String, String> messageContext = new HashMap<>();

        // Generate and set fields
        for (Map.Entry<Integer, FieldGenerationConfig> entry : fieldConfigs.entrySet()) {
            Integer fieldNumber = entry.getKey();
            FieldGenerationConfig config = entry.getValue();

            String fieldValue = fieldGenerator.generateField(fieldNumber, config, csvData, messageContext);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                message.set(fieldNumber, fieldValue);
                messageContext.put("field_" + fieldNumber, fieldValue);
            }
        }

        // Set CSV data directly for unmapped fields
        setDirectCsvFields(message, csvData);

        return message;
    }

    private void setDirectCsvFields(ISOMsg message, Map<String, String> csvData) throws ISOException {
        // Map CSV fields to ISO8583 fields based on your JMeter configuration
        if (csvData.containsKey("PAN")) {
            message.set(2, csvData.get("PAN"));
        }
        if (csvData.containsKey("TRACK2")) {
            message.set(35, csvData.get("TRACK2"));
        }
        if (csvData.containsKey("TERMINAL_ID")) {
            message.set(41, csvData.get("TERMINAL_ID"));
        }
        if (csvData.containsKey("CARD_ACCEPTOR_ID")) {
            message.set(42, csvData.get("CARD_ACCEPTOR_ID"));
        }
        if (csvData.containsKey("CARD_ACCEPTOR_NAME")) {
            message.set(43, csvData.get("CARD_ACCEPTOR_NAME"));
        }
        if (csvData.containsKey("CUENTA")) {
            message.set(102, csvData.get("CUENTA"));
        }
    }

    public ISOMsg buildBalanceInquiryMessage(Map<String, String> csvData) throws ISOException {
        // Build a message similar to your JMeter configuration
        ISOMsg message = buildMessage(MessageType.FINANCIAL_REQUEST, csvData);

        // Set specific fields for balance inquiry
        message.set(1, "1110001000111110011001001000000100101000111000001001000000000000000000000000000000000000000000000000010000000000000000000000000");
        message.set(3, "301099"); // Processing code for balance inquiry
        message.set(18, "6011");  // MCC
        message.set(19, "068");   // Country code
        message.set(22, "051");   // POS entry mode
        message.set(25, "02");    // POS condition code
        message.set(32, "409911"); // Acquiring institution ID
        message.set(49, "068");   // Currency code
        message.set(52, "A40E451FBDD1128B"); // PIN data

        return message;
    }

    public ISOMsg buildCustomMessage(String mti, Map<Integer, String> fields) throws ISOException {
        ISOMsg message = new ISOMsg();
        message.setPackager(packager);
        message.setMTI(mti);

        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            message.set(entry.getKey(), entry.getValue());
        }

        return message;
    }
}