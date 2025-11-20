package com.iso8583.simulator.core.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Resultado del parsing de un mensaje ISO8583
 * Incluye información educativa y de debugging
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParseResult {

    private boolean success;
    private String mti;
    private Map<Integer, ISOField> fields;
    private List<String> errors;
    private List<String> warnings;
    private String rawMessage;
    private LocalDateTime parsedAt;
    private ParsingMetadata metadata;

    // Constructores
    public ParseResult() {
        this.fields = new LinkedHashMap<>();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.parsedAt = LocalDateTime.now();
        this.metadata = new ParsingMetadata();
    }

    public ParseResult(boolean success) {
        this();
        this.success = success;
    }

    // Métodos de conveniencia
    public void addField(ISOField field) {
        this.fields.put(field.getFieldNumber(), field);
    }

    public void addError(String error) {
        this.errors.add(error);
        this.success = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public ISOField getField(int fieldNumber) {
        return fields.get(fieldNumber);
    }

    public String getFieldValue(int fieldNumber) {
        ISOField field = fields.get(fieldNumber);
        return field != null ? field.getValue() : null;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public int getFieldCount() {
        return fields != null ? fields.size() : 0;
    }

    // Clase interna para metadata
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParsingMetadata {
        private String packagerType;
        private String messageClass;
        private String messageFunction;
        private String messageOrigin;
        private long parsingTimeMs;

        // Getters y Setters
        public String getPackagerType() { return packagerType; }
        public void setPackagerType(String packagerType) { this.packagerType = packagerType; }

        public String getMessageClass() { return messageClass; }
        public void setMessageClass(String messageClass) { this.messageClass = messageClass; }

        public String getMessageFunction() { return messageFunction; }
        public void setMessageFunction(String messageFunction) { this.messageFunction = messageFunction; }

        public String getMessageOrigin() { return messageOrigin; }
        public void setMessageOrigin(String messageOrigin) { this.messageOrigin = messageOrigin; }

        public long getParsingTimeMs() { return parsingTimeMs; }
        public void setParsingTimeMs(long parsingTimeMs) { this.parsingTimeMs = parsingTimeMs; }
    }

    // Getters y Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMti() { return mti; }
    public void setMti(String mti) { this.mti = mti; }

    public Map<Integer, ISOField> getFields() { return fields; }
    public void setFields(Map<Integer, ISOField> fields) { this.fields = fields; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }

    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }

    public ParsingMetadata getMetadata() { return metadata; }
    public void setMetadata(ParsingMetadata metadata) { this.metadata = metadata; }
}