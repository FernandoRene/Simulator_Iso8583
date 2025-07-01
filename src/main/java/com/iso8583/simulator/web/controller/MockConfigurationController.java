package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.simulator.MessageSimulator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mock-config")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Mock Configuration", description = "APIs para personalizar respuestas del simulador mock")
public class MockConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(MockConfigurationController.class);

    @Autowired
    private MessageSimulator messageSimulator;

    // ================================
    // CONFIGURACIÓN DE CÓDIGOS DE RESPUESTA
    // ================================

    @Operation(summary = "Configurar código de respuesta por PAN",
            description = "Configura un código de respuesta específico para un PAN determinado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuración aplicada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    @PostMapping("/response-code/pan")
    public ResponseEntity<Map<String, Object>> setResponseCodeForPan(
            @Parameter(description = "PAN (enmascarado en logs por seguridad)")
            @RequestParam String pan,
            @Parameter(description = "Código de respuesta ISO8583 (ej: 51, 54, 55)")
            @RequestParam String responseCode) {

        try {
            if (pan == null || pan.length() < 12) {
                throw new IllegalArgumentException("PAN debe tener al menos 12 dígitos");
            }

            if (responseCode == null || responseCode.length() != 2) {
                throw new IllegalArgumentException("Response Code debe tener exactamente 2 dígitos");
            }

            messageSimulator.setResponseCodeForPan(pan, responseCode);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Código de respuesta configurado para PAN");
            result.put("pan", maskPan(pan));
            result.put("responseCode", responseCode);
            result.put("description", getResponseCodeDescription(responseCode));
            result.put("timestamp", LocalDateTime.now());

            logger.info("Configurado response code {} para PAN {}...{}",
                    responseCode, pan.substring(0, 6), pan.substring(pan.length()-4));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error configurando response code para PAN: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error configurando código de respuesta");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(summary = "Configurar código de respuesta por Terminal ID",
            description = "Configura un código de respuesta específico para un Terminal ID")
    @PostMapping("/response-code/terminal")
    public ResponseEntity<Map<String, Object>> setResponseCodeForTerminal(
            @Parameter(description = "Terminal ID")
            @RequestParam String terminalId,
            @Parameter(description = "Código de respuesta ISO8583")
            @RequestParam String responseCode) {

        try {
            if (terminalId == null || terminalId.trim().isEmpty()) {
                throw new IllegalArgumentException("Terminal ID no puede estar vacío");
            }

            if (responseCode == null || responseCode.length() != 2) {
                throw new IllegalArgumentException("Response Code debe tener exactamente 2 dígitos");
            }

            messageSimulator.setResponseCodeForTerminal(terminalId, responseCode);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Código de respuesta configurado para Terminal");
            result.put("terminalId", terminalId);
            result.put("responseCode", responseCode);
            result.put("description", getResponseCodeDescription(responseCode));
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error configurando response code para terminal: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error configurando código de respuesta");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(summary = "Configurar código de respuesta por Processing Code",
            description = "Configura un código de respuesta específico para un Processing Code")
    @PostMapping("/response-code/processing-code")
    public ResponseEntity<Map<String, Object>> setResponseCodeForProcessingCode(
            @Parameter(description = "Processing Code (ej: 301099 para consulta de saldo)")
            @RequestParam String processingCode,
            @Parameter(description = "Código de respuesta ISO8583")
            @RequestParam String responseCode) {

        try {
            if (processingCode == null || processingCode.length() != 6) {
                throw new IllegalArgumentException("Processing Code debe tener exactamente 6 dígitos");
            }

            if (responseCode == null || responseCode.length() != 2) {
                throw new IllegalArgumentException("Response Code debe tener exactamente 2 dígitos");
            }

            messageSimulator.setResponseCodeForProcessingCode(processingCode, responseCode);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Código de respuesta configurado para Processing Code");
            result.put("processingCode", processingCode);
            result.put("responseCode", responseCode);
            result.put("description", getResponseCodeDescription(responseCode));
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error configurando response code para processing code: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error configurando código de respuesta");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ================================
    // CONFIGURACIÓN AVANZADA
    // ================================

    @Operation(summary = "Configurar tasa de éxito personalizada",
            description = "Configura el porcentaje de transacciones que serán aprobadas")
    @PostMapping("/success-rate")
    public ResponseEntity<Map<String, Object>> setSuccessRate(
            @Parameter(description = "Tasa de éxito entre 0.0 y 1.0 (ej: 0.8 = 80%)")
            @RequestParam double successRate) {

        try {
            if (successRate < 0.0 || successRate > 1.0) {
                throw new IllegalArgumentException("Success rate debe estar entre 0.0 y 1.0");
            }

            messageSimulator.setCustomSuccessRate(successRate);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Tasa de éxito configurada");
            result.put("successRate", successRate);
            result.put("successPercentage", successRate * 100 + "%");
            result.put("timestamp", LocalDateTime.now());

            logger.info("Configurada tasa de éxito personalizada: {}%", successRate * 100);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error configurando tasa de éxito: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error configurando tasa de éxito");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(summary = "Habilitar/Deshabilitar códigos personalizados",
            description = "Activa o desactiva el uso de códigos de respuesta personalizados")
    @PostMapping("/custom-codes/toggle")
    public ResponseEntity<Map<String, Object>> toggleCustomCodes(
            @Parameter(description = "true para habilitar, false para deshabilitar")
            @RequestParam boolean enabled) {

        try {
            messageSimulator.setUseCustomResponseCodes(enabled);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Códigos personalizados " + (enabled ? "habilitados" : "deshabilitados"));
            result.put("enabled", enabled);
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error cambiando estado de códigos personalizados: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error cambiando configuración");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ================================
    // CONSULTA Y GESTIÓN
    // ================================

    @Operation(summary = "Obtener configuraciones actuales",
            description = "Obtiene todas las configuraciones de respuesta personalizadas")
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentConfigurations() {

        try {
            Map<String, Object> configurations = messageSimulator.getCustomConfigurations();
            configurations.put("timestamp", LocalDateTime.now());
            configurations.put("success", true);

            return ResponseEntity.ok(configurations);

        } catch (Exception e) {
            logger.error("Error obteniendo configuraciones: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error obteniendo configuraciones");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Limpiar todas las configuraciones",
            description = "Elimina todas las configuraciones personalizadas y restaura valores por defecto")
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearConfigurations() {

        try {
            messageSimulator.clearCustomConfigurations();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Todas las configuraciones personalizadas han sido limpiadas");
            result.put("timestamp", LocalDateTime.now());

            logger.info("Configuraciones personalizadas del mock limpiadas");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error limpiando configuraciones: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error limpiando configuraciones");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ================================
    // ENDPOINTS DE EJEMPLO Y TESTING
    // ================================

    @Operation(summary = "Configurar escenarios de prueba predefinidos",
            description = "Configura escenarios de prueba comunes con códigos de respuesta específicos")
    @PostMapping("/test-scenarios")
    public ResponseEntity<Map<String, Object>> setupTestScenarios() {

        try {
            // Limpiar configuraciones existentes
            messageSimulator.clearCustomConfigurations();

            // Configurar escenarios de prueba
            messageSimulator.setResponseCodeForPan("4000000000000051", "51"); // Insufficient funds
            messageSimulator.setResponseCodeForPan("4000000000000054", "54"); // Expired card
            messageSimulator.setResponseCodeForPan("4000000000000055", "55"); // Incorrect PIN
            messageSimulator.setResponseCodeForPan("4000000000000062", "62"); // Restricted card
            messageSimulator.setResponseCodeForPan("4000000000000057", "57"); // Transaction not permitted

            messageSimulator.setResponseCodeForTerminal("TESTTERM", "91"); // Issuer inoperative
            messageSimulator.setResponseCodeForTerminal("ERRORTERM", "96"); // System malfunction

            messageSimulator.setUseCustomResponseCodes(true);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Escenarios de prueba configurados exitosamente");
            result.put("timestamp", LocalDateTime.now());

            // Listar escenarios configurados
            Map<String, String> scenarios = new HashMap<>();
            scenarios.put("4000000000000051", "51 - Insufficient funds");
            scenarios.put("4000000000000054", "54 - Expired card");
            scenarios.put("4000000000000055", "55 - Incorrect PIN");
            scenarios.put("4000000000000062", "62 - Restricted card");
            scenarios.put("4000000000000057", "57 - Transaction not permitted");
            scenarios.put("TESTTERM", "91 - Issuer inoperative");
            scenarios.put("ERRORTERM", "96 - System malfunction");

            result.put("configuredScenarios", scenarios);

            logger.info("Escenarios de prueba configurados en el mock");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error configurando escenarios de prueba: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error configurando escenarios de prueba");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Obtener códigos de respuesta disponibles",
            description = "Lista todos los códigos de respuesta ISO8583 disponibles con sus descripciones")
    @GetMapping("/response-codes")
    public ResponseEntity<Map<String, Object>> getAvailableResponseCodes() {

        try {
            Map<String, String> responseCodes = new HashMap<>();
            responseCodes.put("00", "Approved");
            responseCodes.put("01", "Refer to card issuer");
            responseCodes.put("02", "Refer to card issuer, special condition");
            responseCodes.put("03", "Invalid merchant");
            responseCodes.put("04", "Pick up card");
            responseCodes.put("05", "Do not honor");
            responseCodes.put("06", "Error");
            responseCodes.put("07", "Pick up card, special condition");
            responseCodes.put("12", "Invalid transaction");
            responseCodes.put("13", "Invalid amount");
            responseCodes.put("14", "Invalid card number");
            responseCodes.put("15", "No such issuer");
            responseCodes.put("30", "Format error");
            responseCodes.put("41", "Lost card, pick up");
            responseCodes.put("43", "Stolen card, pick up");
            responseCodes.put("51", "Insufficient funds");
            responseCodes.put("54", "Expired card");
            responseCodes.put("55", "Incorrect PIN");
            responseCodes.put("57", "Transaction not permitted to cardholder");
            responseCodes.put("58", "Transaction not permitted to terminal");
            responseCodes.put("61", "Exceeds withdrawal amount limit");
            responseCodes.put("62", "Restricted card");
            responseCodes.put("65", "Exceeds withdrawal frequency limit");
            responseCodes.put("75", "Allowable number of PIN tries exceeded");
            responseCodes.put("76", "Invalid/nonexistent account specified");
            responseCodes.put("77", "Inconsistent with previous transaction");
            responseCodes.put("78", "Blocked, first used");
            responseCodes.put("91", "Issuer or switch inoperative");
            responseCodes.put("92", "Financial institution cannot be found");
            responseCodes.put("94", "Duplicate transmission");
            responseCodes.put("96", "System malfunction");

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("responseCodes", responseCodes);
            result.put("totalCodes", responseCodes.size());
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error obteniendo códigos de respuesta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // MÉTODOS AUXILIARES
    // ================================

    private String getResponseCodeDescription(String responseCode) {
        switch (responseCode) {
            case "00": return "Approved";
            case "01": return "Refer to card issuer";
            case "02": return "Refer to card issuer, special condition";
            case "03": return "Invalid merchant";
            case "04": return "Pick up card";
            case "05": return "Do not honor";
            case "06": return "Error";
            case "07": return "Pick up card, special condition";
            case "12": return "Invalid transaction";
            case "13": return "Invalid amount";
            case "14": return "Invalid card number";
            case "15": return "No such issuer";
            case "30": return "Format error";
            case "41": return "Lost card, pick up";
            case "43": return "Stolen card, pick up";
            case "51": return "Insufficient funds";
            case "54": return "Expired card";
            case "55": return "Incorrect PIN";
            case "57": return "Transaction not permitted to cardholder";
            case "58": return "Transaction not permitted to terminal";
            case "61": return "Exceeds withdrawal amount limit";
            case "62": return "Restricted card";
            case "65": return "Exceeds withdrawal frequency limit";
            case "75": return "Allowable number of PIN tries exceeded";
            case "76": return "Invalid/nonexistent account specified";
            case "77": return "Inconsistent with previous transaction";
            case "78": return "Blocked, first used";
            case "91": return "Issuer or switch inoperative";
            case "92": return "Financial institution cannot be found";
            case "94": return "Duplicate transmission";
            case "96": return "System malfunction";
            default: return "Unknown response code";
        }
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) {
            return pan;
        }
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}