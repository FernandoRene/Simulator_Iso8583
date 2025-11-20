package com.iso8583.simulator.core.field;

import com.iso8583.simulator.core.field.model.FieldEdit;
import com.iso8583.simulator.core.field.model.FieldMetadata;
import com.iso8583.simulator.core.validation.AdvancedFieldValidator;
import com.iso8583.simulator.core.validation.AdvancedFieldValidator.FieldValidationResult;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Servicio para edición dinámica de campos ISO8583
 * Permite modificar campos con validación en tiempo real
 */
@Service
public class FieldEditorService {

    private static final Logger logger = LoggerFactory.getLogger(FieldEditorService.class);

    @Autowired
    private FieldMetadataService metadataService;

    @Autowired
    private AdvancedFieldValidator validator;

    @Autowired
    private FieldGeneratorService generatorService;

    /**
     * Edita un campo en un mensaje ISO8583
     */
    public FieldEditResult editField(ISOMsg message, FieldEdit edit) throws ISOException {
        FieldEditResult result = new FieldEditResult();
        result.setFieldNumber(edit.getFieldNumber());
        result.setRequestedValue(edit.getNewValue());

        try {
            // Obtener metadata del campo
            FieldMetadata metadata = metadataService.getFieldMetadata(edit.getFieldNumber());
            if (metadata == null) {
                result.addWarning("Campo no tiene metadata definida");
            }

            // Validar el nuevo valor
            FieldValidationResult validationResult = validator.validateField(
                    edit.getFieldNumber(),
                    edit.getNewValue()
            );

            result.setValidationResult(validationResult);

            // Si solo es validación, no aplicar cambios
            if (edit.isValidateOnly()) {
                result.setApplied(false);
                result.setMessage("Validación completada (no aplicado)");
                return result;
            }

            // Si tiene errores críticos, no aplicar
            if (!validationResult.isValid()) {
                result.setApplied(false);
                result.setMessage("Campo no modificado debido a errores de validación");
                result.addError("No se puede aplicar campo con errores de validación");
                return result;
            }

            // Guardar valor anterior
            String previousValue = message.hasField(edit.getFieldNumber())
                    ? message.getString(edit.getFieldNumber())
                    : null;
            result.setPreviousValue(previousValue);

            // Aplicar el cambio
            message.set(edit.getFieldNumber(), edit.getNewValue());
            result.setApplied(true);
            result.setMessage("Campo modificado exitosamente");

            logger.info("Campo {} modificado: [{}] -> [{}]",
                    edit.getFieldNumber(), previousValue, edit.getNewValue());

        } catch (Exception e) {
            result.setApplied(false);
            result.addError("Error inesperado: " + e.getMessage());
            logger.error("Error inesperado editando campo {}: {}",
                    edit.getFieldNumber(), e.getMessage(), e);
        }

        return result;
    }

    /**
     * Edita múltiples campos en batch
     */
    public BatchEditResult editMultipleFields(ISOMsg message, List<FieldEdit> edits) throws ISOException {
        BatchEditResult batchResult = new BatchEditResult();

        for (FieldEdit edit : edits) {
            FieldEditResult fieldResult = editField(message, edit);
            batchResult.addFieldResult(fieldResult);

            if (!fieldResult.isApplied()) {
                batchResult.incrementFailures();
            } else {
                batchResult.incrementSuccesses();
            }
        }

        batchResult.setMessage(String.format(
                "Batch completado: %d éxitos, %d fallos de %d campos",
                batchResult.getSuccessCount(),
                batchResult.getFailureCount(),
                edits.size()
        ));

        logger.info("Batch edit completado: {} éxitos, {} fallos",
                batchResult.getSuccessCount(), batchResult.getFailureCount());

        return batchResult;
    }

    /**
     * Obtiene el valor actual de un campo
     */
    public FieldValueResult getFieldValue(ISOMsg message, int fieldNumber) throws ISOException {
        FieldValueResult result = new FieldValueResult();
        result.setFieldNumber(fieldNumber);

        if (message.hasField(fieldNumber)) {
            String value = message.getString(fieldNumber);
            result.setValue(value);
            result.setPresent(true);

            // Incluir metadata
            FieldMetadata metadata = metadataService.getFieldMetadata(fieldNumber);
            result.setMetadata(metadata);

            // Validar valor actual
            FieldValidationResult validation = validator.validateField(fieldNumber, value);
            result.setValidation(validation);

        } else {
            result.setPresent(false);
            result.setMessage("Campo no presente en el mensaje");
        }

        return result;
    }

    /**
     * Obtiene todos los campos presentes en el mensaje con sus valores
     */
    public Map<Integer, FieldValueResult> getAllFieldsWithValues(ISOMsg message) {
        Map<Integer, FieldValueResult> fields = new TreeMap<>();

        try {
            for (int i = 0; i <= 128; i++) {
                if (message.hasField(i)) {
                    FieldValueResult fieldResult = getFieldValue(message, i);
                    fields.put(i, fieldResult);
                }
            }
        } catch (Exception e) {
            logger.error("Error obteniendo todos los campos: {}", e.getMessage());
        }

        return fields;
    }

    /**
     * Elimina un campo del mensaje
     */
    public FieldDeleteResult deleteField(ISOMsg message, int fieldNumber) throws ISOException {
        FieldDeleteResult result = new FieldDeleteResult();
        result.setFieldNumber(fieldNumber);

        if (!message.hasField(fieldNumber)) {
            result.setDeleted(false);
            result.setMessage("Campo no existe en el mensaje");
            return result;
        }

        String previousValue = message.getString(fieldNumber);
        result.setPreviousValue(previousValue);

        message.unset(fieldNumber);
        result.setDeleted(true);
        result.setMessage("Campo eliminado exitosamente");

        logger.info("Campo {} eliminado. Valor anterior: [{}]", fieldNumber, previousValue);

        return result;
    }

    /**
     * Clona un campo a otro número de campo
     */
    public FieldEditResult cloneField(ISOMsg message, int sourceField, int targetField) throws ISOException {
        FieldEditResult result = new FieldEditResult();
        result.setFieldNumber(targetField);

        if (!message.hasField(sourceField)) {
            result.setApplied(false);
            result.addError("Campo origen " + sourceField + " no existe");
            return result;
        }

        String sourceValue = message.getString(sourceField);
        result.setRequestedValue(sourceValue);

        // Validar en campo destino
        FieldValidationResult validation = validator.validateField(targetField, sourceValue);
        result.setValidationResult(validation);

        if (!validation.isValid()) {
            result.setApplied(false);
            result.setMessage("Valor no es válido para campo destino");
            return result;
        }

        // Aplicar clonación
        message.set(targetField, sourceValue);
        result.setApplied(true);
        result.setMessage(String.format("Campo %d clonado a campo %d", sourceField, targetField));

        logger.info("Campo {} clonado a campo {}: [{}]", sourceField, targetField, sourceValue);

        return result;
    }

    /**
     * Auto-completa campos faltantes comunes
     */
    public BatchEditResult autoCompleteCommonFields(ISOMsg message) {
        BatchEditResult batchResult = new BatchEditResult();

        try {
            Map<Integer, String> generatedFields = generatorService.generateCommonFields();

            for (Map.Entry<Integer, String> entry : generatedFields.entrySet()) {
                int fieldNumber = entry.getKey();
                String value = entry.getValue();

                // Solo agregar si no existe
                if (!message.hasField(fieldNumber)) {
                    FieldEdit edit = new FieldEdit(fieldNumber, value);
                    FieldEditResult fieldResult = editField(message, edit);
                    batchResult.addFieldResult(fieldResult);

                    if (fieldResult.isApplied()) {
                        batchResult.incrementSuccesses();
                    } else {
                        batchResult.incrementFailures();
                    }
                }
            }

            batchResult.setMessage(String.format(
                    "Auto-completado: %d campos agregados",
                    batchResult.getSuccessCount()
            ));

        } catch (Exception e) {
            batchResult.addError("Error en auto-completado: " + e.getMessage());
            logger.error("Error en auto-completado: {}", e.getMessage());
        }

        return batchResult;
    }

    // ============================================================================
    // CLASES DE RESULTADO
    // ============================================================================

    /**
     * Resultado de edición de un campo
     */
    public static class FieldEditResult {
        private int fieldNumber;
        private String requestedValue;
        private String previousValue;
        private boolean applied;
        private String message;
        private FieldValidationResult validationResult;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        // Getters y Setters
        public int getFieldNumber() { return fieldNumber; }
        public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

        public String getRequestedValue() { return requestedValue; }
        public void setRequestedValue(String requestedValue) { this.requestedValue = requestedValue; }

        public String getPreviousValue() { return previousValue; }
        public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

        public boolean isApplied() { return applied; }
        public void setApplied(boolean applied) { this.applied = applied; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public FieldValidationResult getValidationResult() { return validationResult; }
        public void setValidationResult(FieldValidationResult validationResult) {
            this.validationResult = validationResult;
        }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }

        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
    }

    /**
     * Resultado de edición batch
     */
    public static class BatchEditResult {
        private List<FieldEditResult> fieldResults = new ArrayList<>();
        private int successCount = 0;
        private int failureCount = 0;
        private String message;
        private List<String> errors = new ArrayList<>();

        public void addFieldResult(FieldEditResult result) {
            fieldResults.add(result);
        }

        public void incrementSuccesses() { successCount++; }
        public void incrementFailures() { failureCount++; }
        public void addError(String error) { errors.add(error); }

        // Getters y Setters
        public List<FieldEditResult> getFieldResults() { return fieldResults; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * Resultado de obtención de valor de campo
     */
    public static class FieldValueResult {
        private int fieldNumber;
        private String value;
        private boolean present;
        private FieldMetadata metadata;
        private FieldValidationResult validation;
        private String message;
        private List<String> errors = new ArrayList<>();

        // Getters y Setters
        public int getFieldNumber() { return fieldNumber; }
        public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }

        public FieldMetadata getMetadata() { return metadata; }
        public void setMetadata(FieldMetadata metadata) { this.metadata = metadata; }

        public FieldValidationResult getValidation() { return validation; }
        public void setValidation(FieldValidationResult validation) { this.validation = validation; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
    }

    /**
     * Resultado de eliminación de campo
     */
    public static class FieldDeleteResult {
        private int fieldNumber;
        private String previousValue;
        private boolean deleted;
        private String message;
        private List<String> errors = new ArrayList<>();

        // Getters y Setters
        public int getFieldNumber() { return fieldNumber; }
        public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

        public String getPreviousValue() { return previousValue; }
        public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
    }
}