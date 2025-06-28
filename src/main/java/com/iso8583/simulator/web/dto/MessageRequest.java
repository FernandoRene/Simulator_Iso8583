package com.iso8583.simulator.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Request para enviar mensaje ISO8583")
public class MessageRequest {

    @JsonProperty("messageType")
    @Schema(description = "Tipo de mensaje ISO8583", example = "FINANCIAL_REQUEST_0200")
    private String messageType;

    @JsonProperty("fields")
    @Schema(description = "Campos del mensaje ISO8583")
    private Map<String, String> fields;

    @JsonProperty("mockResponse")
    @Schema(description = "Si true, genera respuesta mock sin enviar al autorizador", defaultValue = "false")
    private boolean mockResponse = false;

    @JsonProperty("timeout")
    @Schema(description = "Timeout en milisegundos", defaultValue = "30000")
    private Long timeout = 30000L;

    // Constructors
    public MessageRequest() {}

    public MessageRequest(String messageType, Map<String, String> fields) {
        this.messageType = messageType;
        this.fields = fields;
    }

    // Getters and Setters
    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public boolean isMockResponse() {
        return mockResponse;
    }

    public void setMockResponse(boolean mockResponse) {
        this.mockResponse = mockResponse;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "MessageRequest{" +
                "messageType='" + messageType + '\'' +
                ", fields=" + fields +
                ", mockResponse=" + mockResponse +
                ", timeout=" + timeout +
                '}';
    }
}