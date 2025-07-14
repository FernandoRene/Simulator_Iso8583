package com.iso8583.simulator.core.transaction.model;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionResponse {
    private String mti;
    private String stan;
    private String responseCode;
    private String responseMessage;
    private String approvalCode;
    private String rrn;
    private long responseTime;
    private LocalDateTime timestamp;
    private Map<String, String> fields;
    private boolean successful;

    // Inicializar listas para evitar NullPointerException
    private List<String> validationErrors = new ArrayList<>();
    private List<String> validationWarnings = new ArrayList<>();

    public static TransactionResponse fromISOResponse(ISOMsg request, ISOMsg response, long responseTime) throws ISOException {
        TransactionResponse transResponse = new TransactionResponse();
        transResponse.mti = response.getMTI();
        transResponse.stan = response.getString(11);
        transResponse.responseCode = response.getString(39);
        transResponse.approvalCode = response.getString(38);
        transResponse.rrn = response.getString(37);
        transResponse.responseTime = responseTime;
        transResponse.timestamp = LocalDateTime.now();

        // Determinar si fue exitosa
        String code = transResponse.responseCode;
        transResponse.successful = "00".equals(code) || "000".equals(code);

        // Mensaje descriptivo
        transResponse.responseMessage = getResponseMessage(code);

        // Extraer todos los campos para debugging
        Map<String, String> allFields = new HashMap<>();
        for (int i = 1; i <= 128; i++) {
            if (response.hasField(i)) {
                allFields.put(String.valueOf(i), response.getString(i));
            }
        }
        transResponse.fields = allFields;

        return transResponse;
    }

    private static String getResponseMessage(String code) {
        switch (code) {
            case "00": case "000": return "Aprobada";
            case "05": return "No autorizada";
            case "14": return "Número de tarjeta inválido";
            case "51": return "Fondos insuficientes";
            case "54": return "Tarjeta vencida";
            case "55": return "PIN incorrecto";
            case "61": return "Monto excede límite";
            case "96": return "Error del sistema";
            default: return "Código: " + code;
        }
    }

    public static TransactionResponse validationError(ValidationResult validation) {
        TransactionResponse response = new TransactionResponse();
        response.successful = false;
        response.responseCode = "30"; // Format error
        response.responseMessage = "Error de validación: " + String.join(", ", validation.getErrors());
        response.timestamp = LocalDateTime.now();
        response.validationErrors = validation.getErrors();
        response.validationWarnings = validation.getWarnings();
        return response;
    }

    public static TransactionResponse systemError(String errorMessage) {
        TransactionResponse response = new TransactionResponse();
        response.successful = false;
        response.responseCode = "96"; // System error
        response.responseMessage = "Error del sistema: " + errorMessage;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    // ============================================================================
    // GETTERS Y SETTERS COMPLETOS
    // ============================================================================

    public String getMti() { return mti; }
    public void setMti(String mti) { this.mti = mti; }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    public String getApprovalCode() { return approvalCode; }
    public void setApprovalCode(String approvalCode) { this.approvalCode = approvalCode; }

    public String getRrn() { return rrn; }
    public void setRrn(String rrn) { this.rrn = rrn; }

    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getFields() { return fields; }
    public void setFields(Map<String, String> fields) { this.fields = fields; }

    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }

    public List<String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }

    public List<String> getValidationWarnings() { return validationWarnings; }
    public void setValidationWarnings(List<String> validationWarnings) { this.validationWarnings = validationWarnings; }
}