package com.iso8583.simulator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para requests de parsing
 */
@Schema(description = "Request para decodificar mensajes ISO8583")
public class ParseRequest {

    @Schema(description = "Mensaje ISO8583 en formato hexadecimal",
            example = "02003220000000000000161234567890123456=25121011234567890")
    private String message;

    @Schema(description = "Incluir información educativa en la respuesta",
            example = "true")
    private boolean includeEducationalInfo = true;

    // Constructores
    public ParseRequest() {}

    public ParseRequest(String message) {
        this.message = message;
    }

    // Getters y Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isIncludeEducationalInfo() { return includeEducationalInfo; }
    public void setIncludeEducationalInfo(boolean includeEducationalInfo) {
        this.includeEducationalInfo = includeEducationalInfo;
    }
}