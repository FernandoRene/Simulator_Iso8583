package com.iso8583.simulator.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO para respuestas de conexión con información de modo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionResponse {

    private boolean success;
    private String mode;                    // "MOCK" o "REAL"
    private boolean tcpConnectionRequired;  // false en MOCK, true en REAL
    private String message;
    private String simulatorType;           // "MessageSimulator" o "Real Authorizer"
    private String authorizer;              // "Local Mock" o "172.16.1.211:5105"
    private String socketInfo;              // null en MOCK
    private Boolean channelConnected;       // null en MOCK, true/false en REAL
    private String channelType;             // null en MOCK, "PSEUDO-MUX" en REAL
    private Long timestamp;

    // Constructor vacío
    public ConnectionResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isTcpConnectionRequired() {
        return tcpConnectionRequired;
    }

    public void setTcpConnectionRequired(boolean tcpConnectionRequired) {
        this.tcpConnectionRequired = tcpConnectionRequired;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSimulatorType() {
        return simulatorType;
    }

    public void setSimulatorType(String simulatorType) {
        this.simulatorType = simulatorType;
    }

    public String getAuthorizer() {
        return authorizer;
    }

    public void setAuthorizer(String authorizer) {
        this.authorizer = authorizer;
    }

    public String getSocketInfo() {
        return socketInfo;
    }

    public void setSocketInfo(String socketInfo) {
        this.socketInfo = socketInfo;
    }

    public Boolean getChannelConnected() {
        return channelConnected;
    }

    public void setChannelConnected(Boolean channelConnected) {
        this.channelConnected = channelConnected;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}