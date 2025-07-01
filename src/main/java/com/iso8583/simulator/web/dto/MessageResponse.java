package com.iso8583.simulator.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Respuesta del simulador ISO8583 con soporte Mock/Real")
public class MessageResponse {

    @JsonProperty("success")
    @Schema(description = "Indica si la operación fue exitosa")
    private boolean success;

    @JsonProperty("errorMessage")
    @Schema(description = "Mensaje de error si la operación falló")
    private String errorMessage;

    @JsonProperty("requestMti")
    @Schema(description = "MTI del mensaje de request")
    private String requestMti;

    @JsonProperty("responseMti")
    @Schema(description = "MTI del mensaje de respuesta")
    private String responseMti;

    @JsonProperty("requestFields")
    @Schema(description = "Campos del mensaje de request")
    private Map<String, String> requestFields;

    @JsonProperty("responseFields")
    @Schema(description = "Campos del mensaje de respuesta")
    private Map<String, String> responseFields;

    @JsonProperty("responseCode")
    @Schema(description = "Código de respuesta del autorizador")
    private String responseCode;

    @JsonProperty("responseTime")
    @Schema(description = "Tiempo de respuesta en milisegundos")
    private Long responseTime;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Timestamp de la operación")
    private LocalDateTime timestamp;

    // ========================================
    // NUEVOS CAMPOS PARA SOPORTE MOCK/REAL
    // ========================================

    @JsonProperty("mockMode")
    @Schema(description = "Indica si la respuesta fue generada en modo mock")
    private boolean mockMode;

    @JsonProperty("connectionType")
    @Schema(description = "Tipo de conexión utilizada: 'mock' o 'real'")
    private String connectionType;

    // Constructors
    public MessageResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public MessageResponse(boolean success) {
        this();
        this.success = success;
    }

    // Getters and Setters EXISTENTES
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRequestMti() {
        return requestMti;
    }

    public void setRequestMti(String requestMti) {
        this.requestMti = requestMti;
    }

    public String getResponseMti() {
        return responseMti;
    }

    public void setResponseMti(String responseMti) {
        this.responseMti = responseMti;
    }

    public Map<String, String> getRequestFields() {
        return requestFields;
    }

    public void setRequestFields(Map<String, String> requestFields) {
        this.requestFields = requestFields;
    }

    public Map<String, String> getResponseFields() {
        return responseFields;
    }

    public void setResponseFields(Map<String, String> responseFields) {
        this.responseFields = responseFields;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // ========================================
    // NUEVOS GETTERS/SETTERS PARA MOCK/REAL
    // ========================================

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
        // Automaticamente actualizar connectionType
        this.connectionType = mockMode ? "mock" : "real";
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    // ========================================
    // MÉTODOS DE CONVENIENCIA
    // ========================================

    public boolean isApproved() {
        return "00".equals(responseCode);
    }

    public boolean isDeclined() {
        return responseCode != null && !"00".equals(responseCode);
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "MessageResponse{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", requestMti='" + requestMti + '\'' +
                ", responseMti='" + responseMti + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", responseTime=" + responseTime +
                ", mockMode=" + mockMode +
                ", connectionType='" + connectionType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}