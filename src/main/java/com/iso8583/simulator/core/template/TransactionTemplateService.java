package com.iso8583.simulator.core.template;

import com.iso8583.simulator.core.template.model.TemplateCategory;
import com.iso8583.simulator.core.template.model.TemplateField;
import com.iso8583.simulator.core.template.model.TemplateField.FieldBehavior;
import com.iso8583.simulator.core.template.model.TransactionTemplate;
import com.iso8583.simulator.core.field.FieldGeneratorService;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de templates de transacciones
 * CRUD completo y aplicación de templates a mensajes
 */
@Service
public class TransactionTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTemplateService.class);

    @Autowired
    private TemplateStorageService storageService;

    @Autowired
    private FieldGeneratorService generatorService;

    /**
     * Crea un nuevo template de usuario
     */
    public TransactionTemplate createTemplate(TransactionTemplate template) throws Exception {
        // Generar ID si no existe
        if (template.getId() == null || template.getId().isEmpty()) {
            template.setId(generateTemplateId(template.getName()));
        }

        // Verificar que no exista
        if (storageService.existsUserTemplate(template.getId())) {
            throw new IllegalArgumentException("Ya existe un template con ID: " + template.getId());
        }

        // Establecer timestamps
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setPredefined(false);

        // Guardar
        storageService.saveUserTemplate(template);

        logger.info("Template creado: {} ({})", template.getName(), template.getId());
        return template;
    }

    /**
     * Obtiene un template por ID (usuario o predefinido)
     */
    public Optional<TransactionTemplate> getTemplate(String templateId) {
        // Buscar primero en templates de usuario
        Optional<TransactionTemplate> userTemplate = storageService.loadUserTemplate(templateId);
        if (userTemplate.isPresent()) {
            return userTemplate;
        }

        // Buscar en templates predefinidos
        return storageService.loadPredefinedTemplate(templateId);
    }

    /**
     * Obtiene todos los templates (usuario + predefinidos)
     */
    public List<TransactionTemplate> getAllTemplates() {
        List<TransactionTemplate> allTemplates = new ArrayList<>();

        // Cargar predefinidos
        allTemplates.addAll(storageService.loadAllPredefinedTemplates());

        // Cargar de usuario
        allTemplates.addAll(storageService.loadAllUserTemplates());

        // Ordenar por nombre
        allTemplates.sort(Comparator.comparing(TransactionTemplate::getName));

        return allTemplates;
    }

    /**
     * Obtiene templates por categoría
     */
    public List<TransactionTemplate> getTemplatesByCategory(TemplateCategory category) {
        return getAllTemplates().stream()
                .filter(t -> t.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene templates por tipo de transacción
     */
    public List<TransactionTemplate> getTemplatesByTransactionType(String transactionType) {
        return getAllTemplates().stream()
                .filter(t -> transactionType.equalsIgnoreCase(t.getTransactionType()))
                .collect(Collectors.toList());
    }

    /**
     * Actualiza un template de usuario
     */
    public TransactionTemplate updateTemplate(String templateId, TransactionTemplate updatedTemplate)
            throws Exception {

        Optional<TransactionTemplate> existingOpt = storageService.loadUserTemplate(templateId);

        if (!existingOpt.isPresent()) {
            throw new IllegalArgumentException("Template no encontrado: " + templateId);
        }

        TransactionTemplate existing = existingOpt.get();

        if (existing.isPredefined()) {
            throw new IllegalArgumentException("No se pueden modificar templates predefinidos");
        }

        // Actualizar campos
        updatedTemplate.setId(templateId);
        updatedTemplate.setCreatedAt(existing.getCreatedAt());
        updatedTemplate.setUpdatedAt(LocalDateTime.now());
        updatedTemplate.setUsageCount(existing.getUsageCount());
        updatedTemplate.setPredefined(false);

        // Guardar
        storageService.saveUserTemplate(updatedTemplate);

        logger.info("Template actualizado: {}", templateId);
        return updatedTemplate;
    }

    /**
     * Elimina un template de usuario
     */
    public boolean deleteTemplate(String templateId) {
        Optional<TransactionTemplate> template = storageService.loadUserTemplate(templateId);

        if (!template.isPresent()) {
            logger.warn("Intento de eliminar template inexistente: {}", templateId);
            return false;
        }

        if (template.get().isPredefined()) {
            throw new IllegalArgumentException("No se pueden eliminar templates predefinidos");
        }

        boolean deleted = storageService.deleteUserTemplate(templateId);

        if (deleted) {
            logger.info("Template eliminado: {}", templateId);
        }

        return deleted;
    }

    /**
     * Aplica un template a un mensaje ISO8583
     */
    public ISOMsg applyTemplate(String templateId, Map<String, String> userInputs)
            throws Exception {

        Optional<TransactionTemplate> templateOpt = getTemplate(templateId);

        if (!templateOpt.isPresent()) {
            throw new IllegalArgumentException("Template no encontrado: " + templateId);
        }

        TransactionTemplate template = templateOpt.get();
        ISOMsg message = new ISOMsg();

        // Aplicar campos del template
        for (Map.Entry<Integer, TemplateField> entry : template.getFields().entrySet()) {
            int fieldNumber = entry.getKey();
            TemplateField templateField = entry.getValue();

            String value = resolveFieldValue(templateField, userInputs);

            if (value != null) {
                message.set(fieldNumber, value);
            }
        }

        // Incrementar contador de uso
        template.incrementUsage();
        if (!template.isPredefined()) {
            storageService.saveUserTemplate(template);
        }

        logger.info("Template aplicado: {} (uso #{})", template.getName(), template.getUsageCount());
        return message;
    }

    /**
     * Resuelve el valor de un campo según su comportamiento
     */
    private String resolveFieldValue(TemplateField field, Map<String, String> userInputs) {
        switch (field.getBehavior()) {
            case STATIC:
                return field.getValue();

            case AUTO_GENERATE:
                return generateFieldValue(field);

            case USER_INPUT:
                String userValue = userInputs.get(String.valueOf(field.getFieldNumber()));
                if (userValue == null || userValue.isEmpty()) {
                    logger.warn("Campo {} requiere input de usuario pero no fue proporcionado",
                            field.getFieldNumber());
                    return field.getValue(); // Usar default si existe
                }
                return userValue;

            case OPTIONAL:
                String optionalValue = userInputs.get(String.valueOf(field.getFieldNumber()));
                return optionalValue != null ? optionalValue : field.getValue();

            default:
                return field.getValue();
        }
    }

    /**
     * Genera valor automáticamente según la regla del campo
     */
    private String generateFieldValue(TemplateField field) {
        int fieldNumber = field.getFieldNumber();

        switch (fieldNumber) {
            case 7:  return generatorService.generateTransmissionDateTime();
            case 11: return generatorService.generateStan(
                    com.iso8583.simulator.core.field.model.FieldGenerationRequest.GenerationMode.SEQUENTIAL);
            case 12: return generatorService.generateLocalTime();
            case 13: return generatorService.generateLocalDate();
            case 15: return generatorService.generateSettlementDate();
            case 37: return generatorService.generateRrn(
                    com.iso8583.simulator.core.field.model.FieldGenerationRequest.GenerationMode.SEQUENTIAL);
            default: return field.getValue();
        }
    }

    /**
     * Clona un template (crear copia con nuevo ID)
     */
    public TransactionTemplate cloneTemplate(String templateId, String newName) throws Exception {
        Optional<TransactionTemplate> originalOpt = getTemplate(templateId);

        if (!originalOpt.isPresent()) {
            throw new IllegalArgumentException("Template no encontrado: " + templateId);
        }

        TransactionTemplate original = originalOpt.get();
        TransactionTemplate clone = new TransactionTemplate();

        // Copiar propiedades
        clone.setId(generateTemplateId(newName));
        clone.setName(newName);
        clone.setDescription("Copia de: " + original.getDescription());
        clone.setCategory(original.getCategory());
        clone.setTransactionType(original.getTransactionType());
        clone.setProcessingCode(original.getProcessingCode());
        clone.setFields(new HashMap<>(original.getFields()));
        clone.setMetadata(new HashMap<>(original.getMetadata()));
        clone.setPredefined(false);
        clone.setAuthor("System");

        // Guardar
        storageService.saveUserTemplate(clone);

        logger.info("Template clonado: {} -> {}", original.getName(), newName);
        return clone;
    }

    /**
     * Exporta un template a JSON
     */
    public String exportTemplate(String templateId) throws Exception {
        Optional<TransactionTemplate> template = getTemplate(templateId);

        if (!template.isPresent()) {
            throw new IllegalArgumentException("Template no encontrado: " + templateId);
        }

        return storageService.exportTemplate(template.get());
    }

    /**
     * Importa un template desde JSON
     */
    public TransactionTemplate importTemplate(String json) throws Exception {
        TransactionTemplate template = storageService.importTemplate(json);

        // Generar nuevo ID para evitar conflictos
        template.setId(generateTemplateId(template.getName() + "_imported"));
        template.setPredefined(false);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        // Guardar
        storageService.saveUserTemplate(template);

        logger.info("Template importado: {}", template.getName());
        return template;
    }

    /**
     * Genera un ID único para template
     */
    private String generateTemplateId(String name) {
        String baseId = name.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        String id = baseId;
        int counter = 1;

        while (storageService.existsUserTemplate(id)) {
            id = baseId + "_" + counter;
            counter++;
        }

        return id;
    }
}