package com.iso8583.simulator.simulator;

import com.iso8583.simulator.core.enums.ResponseCode;
import com.iso8583.simulator.core.message.ISO8583MessageBuilder;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Component
public class ResponseGenerator {

    private final Random random = new Random();

    public ISOMsg generateResponse(ISOMsg requestMessage, String responseCode) throws ISOException {
        ISOMsg response = new ISOMsg();
        response.setPackager(requestMessage.getPackager());

        // Set response MTI
        String requestMTI = requestMessage.getMTI();
        String responseMTI = getResponseMTI(requestMTI);
        response.setMTI(responseMTI);

        // Copy relevant fields from request
        copyFieldsFromRequest(requestMessage, response);

        // Set response code
        response.set(39, responseCode);

        // Set response-specific fields
        setResponseSpecificFields(response, responseCode);

        return response;
    }

    public ISOMsg generateRandomResponse(ISOMsg requestMessage) throws ISOException {
        ResponseCode[] commonCodes = {
                ResponseCode.APPROVED,
                ResponseCode.INSUFFICIENT_FUNDS,
                ResponseCode.EXPIRED_CARD,
                ResponseCode.INVALID_CARD_NUMBER,
                ResponseCode.DO_NOT_HONOR
        };

        ResponseCode selectedCode = commonCodes[random.nextInt(commonCodes.length)];
        return generateResponse(requestMessage, selectedCode.getCode());
    }

    public ISOMsg generateSuccessResponse(ISOMsg requestMessage) throws ISOException {
        return generateResponse(requestMessage, ResponseCode.APPROVED.getCode());
    }

    public ISOMsg generateErrorResponse(ISOMsg requestMessage, ResponseCode errorCode) throws ISOException {
        return generateResponse(requestMessage, errorCode.getCode());
    }

    private String getResponseMTI(String requestMTI) {
        if (requestMTI == null || requestMTI.length() != 4) {
            return "0110"; // Default response
        }

        char[] mti = requestMTI.toCharArray();
        mti[2] = '1'; // Change third digit to 1 for response
        return new String(mti);
    }

    private void copyFieldsFromRequest(ISOMsg request, ISOMsg response) throws ISOException {
        // Fields to copy from request to response
        int[] fieldsToCopy = {2, 3, 7, 11, 12, 13, 14, 18, 22, 25, 32, 37, 41, 42, 43, 49, 102};

        for (int fieldNumber : fieldsToCopy) {
            if (request.hasField(fieldNumber)) {
                response.set(fieldNumber, request.getString(fieldNumber));
            }
        }
    }

    private void setResponseSpecificFields(ISOMsg response, String responseCode) throws ISOException {
        // Set action code (field 39) - already set above

        // For balance inquiry responses, add balance information
        if (response.hasField(3)) {
            String processingCode = response.getString(3);
            if ("301099".equals(processingCode)) { // Balance inquiry
                if (ResponseCode.APPROVED.getCode().equals(responseCode)) {
                    // Add mock balance information
                    response.set(54, "00068100000500000068200001000000"); // Account balances
                }
            }
        }

        // Set response timestamp if not present
        if (!response.hasField(7)) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmmss");
            response.set(7, sdf.format(new Date()));
        }
    }
}