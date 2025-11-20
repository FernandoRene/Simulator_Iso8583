package com.iso8583.simulator.api.controller;

import com.iso8583.simulator.core.field.FieldEditorService;
import com.iso8583.simulator.core.field.FieldEditorService.*;
import com.iso8583.simulator.core.field.FieldGeneratorService;
import com.iso8583.simulator.core.field.FieldMetadataService;
import com.iso8583.simulator.core.field.model.FieldEdit;
import com.iso8583.simulator.core.field.model.FieldGenerationRequest;
import com.iso8583.simulator.core.field.model.FieldMetadata;
import com.iso8583.simulator.core.validation.AdvancedFieldValidator;
import com.iso8583.simulator.core.validation.AdvancedFieldValidator.FieldValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller para edición dinámica de campos ISO8583
 */
@RestController
@RequestMapping("/api/v1/fields")
@Tag(name = "Field Editor", description = "Endpoints para edición y validación de campos ISO8583")
public class FieldEditorController {

    @Autowired
    private FieldEditorService editorService;

    @Autowired
    private FieldMetadataService metadataService;

    @Autowired
    private FieldGeneratorService generatorService;

    @Autowired
    private AdvancedFieldValidator validator;

    /**
     * Obtiene metadata de todos los campos ISO8583
     */
    @GetMapping("/metadata")
    @Operation(summary = "Obtener metadata de todos los campos",
            description = "Retorna catálogo completo de campos ISO8583 con descripciones educativas")
    public ResponseEntity<Map<Integer, FieldMetadata>> getAllFieldsMetadata() {
        Map<Integer, FieldMetadata> metadata = metadataService.getAllFieldsMetadata();
        return ResponseEntity.ok(metadata);
    }

    /**
     * Obtiene metadata de un campo específico
     */
    @GetMapping("/metadata/{fieldNumber}")
    @Operation(summary = "Obtener metadata de un campo específico")
    public ResponseEntity<?> getFieldMetadata(
            @Parameter(description = "Número de campo (0-128)")
            @PathVariable int fieldNumber) {

        FieldMetadata metadata = metadataService.getFieldMetadata(fieldNumber);

        if (metadata == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Campo no encontrado en catálogo"));
        }

        return ResponseEntity.ok(metadata);
    }

    /**
     * Obtiene lista de campos requeridos para un tipo de transacción
     */
    @GetMapping("/required")
    @Operation(summary = "Obtener campos requeridos para un tipo de transacción")
    public ResponseEntity<List<Integer>> getRequiredFields(
            @Parameter(description = "Processing Code (ej: 000000 para Purchase)")
            @RequestParam(required = false) String processingCode) {

        List<Integer> requiredFields = metadataService.getRequiredFieldsForTransaction(processingCode);
        return ResponseEntity.ok(requiredFields);
    }

    /**
     * Valida un campo individual
     */
    @PostMapping("/validate")
    @Operation(summary = "Validar un campo individual",
            description = "Valida formato, longitud y reglas específicas del campo")
    public ResponseEntity<FieldValidationResult> validateField(
            @RequestBody FieldValidationRequest request) {

        FieldValidationResult result = validator.validateField(
                request.getFieldNumber(),
                request.getValue()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Edita un campo en un mensaje (mock para demostración)
     */
    @PostMapping("/edit")
    @Operation(summary = "Editar un campo",
            description = "Modifica el valor de un campo con validación")
    public ResponseEntity<FieldEditResult> editField(
            @RequestBody FieldEditRequest request) {

        try {
            // Crear mensaje de ejemplo para demostración
            ISOMsg message = createSampleMessage();

            FieldEdit edit = new FieldEdit(
                    request.getFieldNumber(),
                    request.getNewValue()
            );
            edit.setValidateOnly(request.isValidateOnly());

            FieldEditResult result = editorService.editField(message, edit);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            FieldEditResult errorResult = new FieldEditResult();
            errorResult.setApplied(false);
            errorResult.addError("Error editando campo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Edita múltiples campos en batch
     */
    @PostMapping("/edit/batch")
    @Operation(summary = "Editar múltiples campos",
            description = "Modifica varios campos en una sola operación")
    public ResponseEntity<BatchEditResult> editMultipleFields(
            @RequestBody List<FieldEdit> edits) {

        try {
            ISOMsg message = createSampleMessage();
            BatchEditResult result = editorService.editMultipleFields(message, edits);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            BatchEditResult errorResult = new BatchEditResult();
            errorResult.addError("Error en batch edit: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Genera campos automáticamente
     */
    @PostMapping("/generate")
    @Operation(summary = "Generar campos automáticamente",
            description = "Auto-genera valores para campos comunes (STAN, RRN, timestamps, etc)")
    public ResponseEntity<Map<Integer, String>> generateFields(
            @RequestBody FieldGenerationRequest request) {

        try {
            ISOMsg message = new ISOMsg();
            generatorService.generateFields(message, request);

            // Extraer campos generados
            Map<Integer, String> generatedFields = new java.util.HashMap<>();
            for (Integer fieldNumber : request.getFieldsToGenerate()) {
                if (message.hasField(fieldNumber)) {
                    generatedFields.put(fieldNumber, message.getString(fieldNumber));
                }
            }

            return ResponseEntity.ok(generatedFields);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(-1, "Error generando campos: " + e.getMessage()));
        }
    }

    /**
     * Genera campos comunes automáticamente
     */
    @PostMapping("/generate/common")
    @Operation(summary = "Generar campos comunes",
            description = "Auto-genera campos típicos: STAN, RRN, timestamps")
    public ResponseEntity<Map<Integer, String>> generateCommonFields() {
        Map<Integer, String> commonFields = generatorService.generateCommonFields();
        return ResponseEntity.ok(commonFields);
    }

    /**
     * Auto-completa campos faltantes en un mensaje
     */
    @PostMapping("/autocomplete")
    @Operation(summary = "Auto-completar campos faltantes",
            description = "Agrega automáticamente campos comunes que faltan en el mensaje")
    public ResponseEntity<BatchEditResult> autoCompleteFields() {
        try {
            ISOMsg message = createSampleMessage();
            BatchEditResult result = editorService.autoCompleteCommonFields(message);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            BatchEditResult errorResult = new BatchEditResult();
            errorResult.addError("Error en auto-completado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Obtiene el valor actual de un campo
     */
    @GetMapping("/value/{fieldNumber}")
    @Operation(summary = "Obtener valor de un campo")
    public ResponseEntity<FieldValueResult> getFieldValue(
            @PathVariable int fieldNumber) {

        try {
            ISOMsg message = createSampleMessage();
            FieldValueResult result = editorService.getFieldValue(message, fieldNumber);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            FieldValueResult errorResult = new FieldValueResult();
            errorResult.setFieldNumber(fieldNumber);
            errorResult.addError("Error obteniendo valor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Resetea el contador de STAN
     */
    @PostMapping("/generate/stan/reset")
    @Operation(summary = "Resetear contador de STAN",
            description = "Reinicia la secuencia de STAN a 1")
    public ResponseEntity<Map<String, String>> resetStanSequence() {
        generatorService.resetStanSequence();
        return ResponseEntity.ok(Map.of(
                "message", "Secuencia STAN reseteada a 1",
                "currentStan", String.format("%06d", generatorService.getCurrentStan())
        ));
    }

    // ============================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================

    /**
     * Crea un mensaje de ejemplo para demostración
     */
    private ISOMsg createSampleMessage() throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(2, "4218283014136073");
        msg.set(3, "000000");
        msg.set(4, "000000009800");
        msg.set(7, generatorService.generateTransmissionDateTime());
        msg.set(11, generatorService.generateStan(FieldGenerationRequest.GenerationMode.SEQUENTIAL));
        msg.set(12, generatorService.generateLocalTime());
        msg.set(13, generatorService.generateLocalDate());
        msg.set(41, "00000001");
        msg.set(42, "000000000000001");
        msg.set(49, "068");
        return msg;
    }

    // ============================================================================
    // CLASES DE REQUEST
    // ============================================================================

    /**
     * Request para validación de campo
     */
    public static class FieldValidationRequest {
        private int fieldNumber;
        private String value;

        public int getFieldNumber() { return fieldNumber; }
        public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    /**
     * Request para edición de campo
     */
    public static class FieldEditRequest {
        private int fieldNumber;
        private String newValue;
        private boolean validateOnly;

        public int getFieldNumber() { return fieldNumber; }
        public void setFieldNumber(int fieldNumber) { this.fieldNumber = fieldNumber; }

        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }

        public boolean isValidateOnly() { return validateOnly; }
        public void setValidateOnly(boolean validateOnly) { this.validateOnly = validateOnly; }
    }
}