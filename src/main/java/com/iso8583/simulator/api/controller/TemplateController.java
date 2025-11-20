package com.iso8583.simulator.api.controller;

import com.iso8583.simulator.core.template.TransactionTemplateService;
import com.iso8583.simulator.core.template.model.TemplateCategory;
import com.iso8583.simulator.core.template.model.TransactionTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller para gestión de templates de transacciones
 */
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Transaction Templates", description = "Endpoints para gestión de templates de transacciones ISO8583")
public class TemplateController {

    @Autowired
    private TransactionTemplateService templateService;

    /**
     * Obtiene todos los templates disponibles
     */
    @GetMapping
    @Operation(summary = "Listar todos los templates",
            description = "Retorna todos los templates disponibles (predefinidos y de usuario)")
    public ResponseEntity<List<TransactionTemplate>> getAllTemplates() {
        List<TransactionTemplate> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Obtiene un template específico por ID
     */
    @GetMapping("/{templateId}")
    @Operation(summary = "Obtener template por ID")
    public ResponseEntity<?> getTemplate(
            @Parameter(description = "ID del template")
            @PathVariable String templateId) {

        Optional<TransactionTemplate> template = templateService.getTemplate(templateId);

        if (!template.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Template no encontrado: " + templateId));
        }

        return ResponseEntity.ok(template.get());
    }

    /**
     * Obtiene templates por categoría
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Obtener templates por categoría")
    public ResponseEntity<List<TransactionTemplate>> getTemplatesByCategory(
            @Parameter(description = "Categoría del template")
            @PathVariable TemplateCategory category) {

        List<TransactionTemplate> templates = templateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(templates);
    }

    /**
     * Obtiene templates por tipo de transacción
     */
    @GetMapping("/type/{transactionType}")
    @Operation(summary = "Obtener templates por tipo de transacción")
    public ResponseEntity<List<TransactionTemplate>> getTemplatesByType(
            @Parameter(description = "Tipo de transacción (PURCHASE, CASH_ADVANCE, etc)")
            @PathVariable String transactionType) {

        List<TransactionTemplate> templates = templateService.getTemplatesByTransactionType(transactionType);
        return ResponseEntity.ok(templates);
    }

    /**
     * Crea un nuevo template de usuario
     */
    @PostMapping
    @Operation(summary = "Crear nuevo template",
            description = "Crea un template personalizado de usuario")
    public ResponseEntity<?> createTemplate(@RequestBody TransactionTemplate template) {
        try {
            TransactionTemplate created = templateService.createTemplate(template);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creando template: " + e.getMessage()));
        }
    }

    /**
     * Actualiza un template existente
     */
    @PutMapping("/{templateId}")
    @Operation(summary = "Actualizar template",
            description = "Actualiza un template de usuario (no predefinidos)")
    public ResponseEntity<?> updateTemplate(
            @PathVariable String templateId,
            @RequestBody TransactionTemplate template) {

        try {
            TransactionTemplate updated = templateService.updateTemplate(templateId, template);
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error actualizando template: " + e.getMessage()));
        }
    }

    /**
     * Elimina un template de usuario
     */
    @DeleteMapping("/{templateId}")
    @Operation(summary = "Eliminar template",
            description = "Elimina un template de usuario (no predefinidos)")
    public ResponseEntity<?> deleteTemplate(@PathVariable String templateId) {
        try {
            boolean deleted = templateService.deleteTemplate(templateId);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "message", "Template eliminado exitosamente",
                        "templateId", templateId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Template no encontrado: " + templateId));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error eliminando template: " + e.getMessage()));
        }
    }

    /**
     * Aplica un template para generar un mensaje ISO8583
     */
    @PostMapping("/{templateId}/apply")
    @Operation(summary = "Aplicar template",
            description = "Genera un mensaje ISO8583 basado en un template")
    public ResponseEntity<?> applyTemplate(
            @PathVariable String templateId,
            @RequestBody(required = false) ApplyTemplateRequest request) {

        try {
            Map<String, String> userInputs = request != null ? request.getUserInputs() : Map.of();
            ISOMsg message = templateService.applyTemplate(templateId, userInputs);

            // Convertir mensaje a formato legible
            TemplateApplicationResult result = new TemplateApplicationResult();
            result.setTemplateId(templateId);
            result.setSuccess(true);
            result.setMessage("Template aplicado exitosamente");
            result.setGeneratedFields(extractFields(message));

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error aplicando template: " + e.getMessage()));
        }
    }

    /**
     * Clona un template existente
     */
    @PostMapping("/{templateId}/clone")
    @Operation(summary = "Clonar template",
            description = "Crea una copia de un template existente")
    public ResponseEntity<?> cloneTemplate(
            @PathVariable String templateId,
            @RequestBody CloneTemplateRequest request) {

        try {
            TransactionTemplate cloned = templateService.cloneTemplate(
                    templateId,
                    request.getNewName()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(cloned);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error clonando template: " + e.getMessage()));
        }
    }

    /**
     * Exporta un template a JSON
     */
    @GetMapping("/{templateId}/export")
    @Operation(summary = "Exportar template",
            description = "Exporta un template en formato JSON")
    public ResponseEntity<?> exportTemplate(@PathVariable String templateId) {
        try {
            String json = templateService.exportTemplate(templateId);
            return ResponseEntity.ok(Map.of(
                    "templateId", templateId,
                    "json", json
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error exportando template: " + e.getMessage()));
        }
    }

    /**
     * Importa un template desde JSON
     */
    @PostMapping("/import")
    @Operation(summary = "Importar template",
            description = "Importa un template desde formato JSON")
    public ResponseEntity<?> importTemplate(@RequestBody ImportTemplateRequest request) {
        try {
            TransactionTemplate imported = templateService.importTemplate(request.getJson());
            return ResponseEntity.status(HttpStatus.CREATED).body(imported);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error importando template: " + e.getMessage()));
        }
    }

    /**
     * Obtiene estadísticas de uso de templates
     */
    @GetMapping("/stats")
    @Operation(summary = "Estadísticas de templates",
            description = "Retorna estadísticas de uso de templates")
    public ResponseEntity<TemplateStats> getTemplateStats() {
        List<TransactionTemplate> allTemplates = templateService.getAllTemplates();

        TemplateStats stats = new TemplateStats();
        stats.setTotalTemplates(allTemplates.size());
        stats.setPredefinedTemplates((int) allTemplates.stream()
                .filter(TransactionTemplate::isPredefined).count());
        stats.setUserTemplates((int) allTemplates.stream()
                .filter(t -> !t.isPredefined()).count());
        stats.setTotalUsage(allTemplates.stream()
                .mapToInt(TransactionTemplate::getUsageCount).sum());
        stats.setMostUsedTemplate(allTemplates.stream()
                .max((t1, t2) -> Integer.compare(t1.getUsageCount(), t2.getUsageCount()))
                .map(TransactionTemplate::getName)
                .orElse("N/A"));

        return ResponseEntity.ok(stats);
    }

    // ============================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================

    /**
     * Extrae campos de un mensaje ISO8583
     */
    private Map<Integer, String> extractFields(ISOMsg message) {
        Map<Integer, String> fields = new java.util.TreeMap<>();

        try {
            for (int i = 0; i <= 128; i++) {
                if (message.hasField(i)) {
                    fields.put(i, message.getString(i));
                }
            }
        } catch (Exception e) {
            // Ignorar errores de extracción
        }

        return fields;
    }

    // ============================================================================
    // CLASES DE REQUEST/RESPONSE
    // ============================================================================

    /**
     * Request para aplicar template
     */
    public static class ApplyTemplateRequest {
        private Map<String, String> userInputs;

        public Map<String, String> getUserInputs() {
            return userInputs != null ? userInputs : Map.of();
        }

        public void setUserInputs(Map<String, String> userInputs) {
            this.userInputs = userInputs;
        }
    }

    /**
     * Resultado de aplicar template
     */
    public static class TemplateApplicationResult {
        private String templateId;
        private boolean success;
        private String message;
        private Map<Integer, String> generatedFields;

        // Getters y Setters
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<Integer, String> getGeneratedFields() { return generatedFields; }
        public void setGeneratedFields(Map<Integer, String> generatedFields) {
            this.generatedFields = generatedFields;
        }
    }

    /**
     * Request para clonar template
     */
    public static class CloneTemplateRequest {
        private String newName;

        public String getNewName() { return newName; }
        public void setNewName(String newName) { this.newName = newName; }
    }

    /**
     * Request para importar template
     */
    public static class ImportTemplateRequest {
        private String json;

        public String getJson() { return json; }
        public void setJson(String json) { this.json = json; }
    }

    /**
     * Estadísticas de templates
     */
    public static class TemplateStats {
        private int totalTemplates;
        private int predefinedTemplates;
        private int userTemplates;
        private int totalUsage;
        private String mostUsedTemplate;

        // Getters y Setters
        public int getTotalTemplates() { return totalTemplates; }
        public void setTotalTemplates(int totalTemplates) { this.totalTemplates = totalTemplates; }

        public int getPredefinedTemplates() { return predefinedTemplates; }
        public void setPredefinedTemplates(int predefinedTemplates) {
            this.predefinedTemplates = predefinedTemplates;
        }

        public int getUserTemplates() { return userTemplates; }
        public void setUserTemplates(int userTemplates) { this.userTemplates = userTemplates; }

        public int getTotalUsage() { return totalUsage; }
        public void setTotalUsage(int totalUsage) { this.totalUsage = totalUsage; }

        public String getMostUsedTemplate() { return mostUsedTemplate; }
        public void setMostUsedTemplate(String mostUsedTemplate) {
            this.mostUsedTemplate = mostUsedTemplate;
        }
    }
}