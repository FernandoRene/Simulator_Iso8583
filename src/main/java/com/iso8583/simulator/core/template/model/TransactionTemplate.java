package com.iso8583.simulator.core.template.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo para templates de transacciones ISO8583
 * Permite guardar configuraciones predefinidas para reutilizar
 */
public class TransactionTemplate {

    private String id;
    private String name;
    private String description;
    private TemplateCategory category;
    private String transactionType;
    private String processingCode;
    private Map<Integer, TemplateField> fields;
    private Map<String, String> metadata;
    private boolean predefined; // Template del sistema vs usuario
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String author;
    private int usageCount;

    // Constructor
    public TransactionTemplate() {
        this.fields = new HashMap<>();
        this.metadata = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.usageCount = 0;
        this.predefined = false;
    }

    public TransactionTemplate(String id, String name, String description) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TemplateCategory getCategory() { return category; }
    public void setCategory(TemplateCategory category) { this.category = category; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getProcessingCode() { return processingCode; }
    public void setProcessingCode(String processingCode) {
        this.processingCode = processingCode;
    }

    public Map<Integer, TemplateField> getFields() { return fields; }
    public void setFields(Map<Integer, TemplateField> fields) { this.fields = fields; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public boolean isPredefined() { return predefined; }
    public void setPredefined(boolean predefined) { this.predefined = predefined; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    /**
     * Incrementa contador de uso
     */
    public void incrementUsage() {
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Agrega un campo al template
     */
    public void addField(int fieldNumber, TemplateField field) {
        this.fields.put(fieldNumber, field);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Remueve un campo del template
     */
    public void removeField(int fieldNumber) {
        this.fields.remove(fieldNumber);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Agrega metadata
     */
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
}