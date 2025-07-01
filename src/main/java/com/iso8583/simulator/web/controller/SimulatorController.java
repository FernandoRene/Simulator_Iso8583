package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.core.config.SimulatorConfiguration;
import com.iso8583.simulator.core.enums.SimulatorMode;
import com.iso8583.simulator.web.dto.MessageRequest;
import com.iso8583.simulator.web.dto.MessageResponse;
import com.iso8583.simulator.web.service.SimulatorService;
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
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/simulator")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "ISO8583 Simulator", description = "APIs para simulación de mensajes ISO8583 con soporte Mock/Real")
public class SimulatorController {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorController.class);

    @Autowired
    private SimulatorService simulatorService;

    @Autowired
    private SimulatorConfiguration config;

    // ================================
    // ENDPOINTS PRINCIPALES
    // ================================

    @Operation(summary = "Enviar mensaje ISO8583",
            description = "Envía un mensaje ISO8583 usando el modo configurado (Mock o Real)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensaje enviado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<MessageResponse>> sendMessage(
            @RequestBody MessageRequest request) {

        logger.info("Received message request: {} - Mode: {}", request.getMessageType(), config.getMode());

        return simulatorService.sendMessage(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Error processing message: {}", throwable.getMessage(), throwable);
                    MessageResponse errorResponse = new MessageResponse(false);
                    errorResponse.setErrorMessage("Error interno: " + throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    @Operation(summary = "Forzar mensaje mock",
            description = "Genera una respuesta mock independientemente del modo configurado")
    @PostMapping("/mock")
    public CompletableFuture<ResponseEntity<MessageResponse>> generateMockMessage(
            @RequestBody MessageRequest request) {

        logger.info("Generating forced mock message: {}", request.getMessageType());

        // Temporalmente cambiar a modo mock para esta petición
        SimulatorMode originalMode = config.getMode();
        config.setMode(SimulatorMode.MOCK);

        return simulatorService.sendMessage(request)
                .thenApply(response -> {
                    // Restaurar modo original
                    config.setMode(originalMode);
                    response.setMockMode(true);
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    // Restaurar modo original en caso de error
                    config.setMode(originalMode);
                    logger.error("Error generating mock message: {}", throwable.getMessage(), throwable);
                    MessageResponse errorResponse = new MessageResponse(false);
                    errorResponse.setErrorMessage("Error generando mock: " + throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    // ================================
    // GESTIÓN DE MODOS
    // ================================

    @Operation(summary = "Obtener modo actual",
            description = "Obtiene el modo de operación actual del simulador")
    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> getCurrentMode() {

        try {
            Map<String, Object> modeInfo = new HashMap<>();
            modeInfo.put("currentMode", config.getMode().getCode());
            modeInfo.put("description", config.getMode().getDescription());
            modeInfo.put("dynamicChangeEnabled", config.isDynamicModeChange());
            modeInfo.put("availableModes", SimulatorMode.values());
            modeInfo.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(modeInfo);
        } catch (Exception e) {
            logger.error("Error getting current mode: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Cambiar modo del simulador",
            description = "Cambia el modo de operación del simulador (mock/real/hybrid)")
    @PostMapping("/mode/{mode}")
    public ResponseEntity<Map<String, Object>> changeMode(
            @Parameter(description = "Nuevo modo (mock, real, hybrid)")
            @PathVariable String mode) {

        try {
            SimulatorMode newMode = SimulatorMode.fromCode(mode);
            boolean success = simulatorService.changeMode(newMode);

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("previousMode", config.getMode().getCode());
            result.put("newMode", newMode.getCode());
            result.put("timestamp", LocalDateTime.now());

            if (success) {
                result.put("message", "Modo cambiado exitosamente");
                return ResponseEntity.ok(result);
            } else {
                result.put("message", "No se pudo cambiar el modo");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

        } catch (IllegalArgumentException e) {
            logger.error("Invalid mode requested: {}", mode);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Modo inválido: " + mode);
            error.put("validModes", new String[]{"mock", "real", "hybrid"});
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error changing mode: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // ESTADO Y CONEXIÓN
    // ================================

    @Operation(summary = "Estado del simulador",
            description = "Obtiene el estado completo del simulador incluyendo modo y conexión")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSimulatorStatus() {

        try {
            Map<String, Object> status = simulatorService.getSimulatorStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting simulator status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Estado de conexión",
            description = "Obtiene el estado actual de la conexión según el modo configurado")
    @GetMapping("/connection/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {

        logger.debug("Getting connection status for mode: {}", config.getMode());

        try {
            Map<String, Object> status = new HashMap<>();
            status.put("mode", config.getMode().getCode());

            if (config.isRealMode()) {
                status.put("connected", false); // TODO: Implementar check real
                status.put("host", config.getSwitch().getHost());
                status.put("port", config.getSwitch().getPort());
                status.put("type", "real");
                status.put("message", "Conexión real no implementada aún");
            } else {
                status.put("connected", true);
                status.put("host", "mock-simulator");
                status.put("port", "N/A");
                status.put("type", "mock");
                status.put("responseTime", "45ms");
            }

            status.put("lastChecked", LocalDateTime.now());
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error getting connection status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Probar conexión",
            description = "Prueba la conectividad según el modo actual del simulador")
    @PostMapping("/connection/test")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> testConnection() {

        logger.info("Testing connection in mode: {}", config.getMode());

        return simulatorService.testConnection()
                .thenApply(result -> {
                    if ((Boolean) result.get("success")) {
                        return ResponseEntity.ok(result);
                    } else {
                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Error testing connection: {}", throwable.getMessage(), throwable);
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", throwable.getMessage());
                    error.put("timestamp", LocalDateTime.now());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                });
    }

    // ================================
    // ESTADÍSTICAS Y MÉTRICAS
    // ================================

    @Operation(summary = "Estadísticas del simulador",
            description = "Obtiene estadísticas y métricas detalladas del simulador")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSimulatorStats() {

        logger.debug("Getting simulator statistics");

        try {
            Map<String, Object> fullStats = simulatorService.getSimulatorStatus();
            Map<String, Object> stats = (Map<String, Object>) fullStats.get("stats");

            // Agregar información adicional
            stats.put("mode", config.getMode().getCode());
            stats.put("modeDescription", config.getMode().getDescription());
            stats.put("uptime", System.currentTimeMillis());
            stats.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting simulator stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Reiniciar estadísticas",
            description = "Reinicia las estadísticas del simulador")
    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, Object>> resetStats() {

        try {
            // TODO: Implementar reset de estadísticas en el service
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Estadísticas reiniciadas");
            result.put("timestamp", LocalDateTime.now());

            logger.info("Simulator statistics reset");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error resetting stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // CONFIGURACIÓN Y TEMPLATES
    // ================================

    @Operation(summary = "Obtener configuración actual",
            description = "Obtiene la configuración completa del simulador")
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {

        try {
            Map<String, Object> configuration = new HashMap<>();
            configuration.put("mode", config.getMode().getCode());
            configuration.put("dynamicModeChange", config.isDynamicModeChange());

            // Configuración del switch (sin información sensible)
            Map<String, Object> switchConfig = new HashMap<>();
            switchConfig.put("host", config.getSwitch().getHost());
            switchConfig.put("port", config.getSwitch().getPort());
            switchConfig.put("timeout", config.getSwitch().getTimeout());
            switchConfig.put("enabled", config.getSwitch().isEnabled());
            configuration.put("switch", switchConfig);

            // Configuración mock
            Map<String, Object> mockConfig = new HashMap<>();
            mockConfig.put("enabled", config.getMock().isEnabled());
            mockConfig.put("minResponseTime", config.getMock().getMinResponseTime());
            mockConfig.put("maxResponseTime", config.getMock().getMaxResponseTime());
            mockConfig.put("successRate", config.getMock().getSuccessRate());
            configuration.put("mock", mockConfig);

            configuration.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(configuration);
        } catch (Exception e) {
            logger.error("Error getting configuration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Health check",
            description = "Verifica que el simulador esté funcionando correctamente")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {

        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("service", "ISO8583 Simulator");
            health.put("version", "1.0.0");
            health.put("mode", config.getMode().getCode());

            // Check básico según el modo
            if (config.isRealMode()) {
                health.put("connectionType", "real");
                health.put("authorizerHost", config.getSwitch().getHost());
                health.put("authorizerPort", config.getSwitch().getPort());
            } else {
                health.put("connectionType", "mock");
            }

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    @Operation(summary = "Obtener tipos de mensaje disponibles",
            description = "Lista los tipos de mensaje ISO8583 soportados")
    @GetMapping("/message-types")
    public ResponseEntity<Map<String, Object>> getMessageTypes() {

        try {
            Map<String, Object> messageTypes = new HashMap<>();
            messageTypes.put("FINANCIAL_REQUEST_0200", "Solicitud financiera");
            messageTypes.put("FINANCIAL_RESPONSE_0210", "Respuesta financiera");
            messageTypes.put("REVERSAL_REQUEST_0400", "Solicitud de reverso");
            messageTypes.put("REVERSAL_RESPONSE_0410", "Respuesta de reverso");
            messageTypes.put("NETWORK_REQUEST_0800", "Solicitud de red");
            messageTypes.put("NETWORK_RESPONSE_0810", "Respuesta de red");

            Map<String, Object> response = new HashMap<>();
            response.put("messageTypes", messageTypes);
            response.put("count", messageTypes.size());
            response.put("currentMode", config.getMode().getCode());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting message types: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Obtener template de mensaje",
            description = "Obtiene un template con campos predefinidos para un tipo de mensaje")
    @GetMapping("/message-template/{messageType}")
    public ResponseEntity<Map<String, Object>> getMessageTemplate(
            @Parameter(description = "Tipo de mensaje")
            @PathVariable String messageType) {

        try {
            Map<String, String> template = new HashMap<>();

            switch (messageType.toUpperCase()) {
                case "FINANCIAL_REQUEST_0200":
                case "0200":
                    template.put("2", "4000000000000002");  // PAN
                    template.put("3", "000000");             // Processing Code
                    template.put("4", "000000001000");       // Transaction Amount
                    template.put("7", "0101120000");         // Transmission Date/Time
                    template.put("11", "000001");            // System Trace Audit Number
                    template.put("12", "120000");            // Local Transaction Time
                    template.put("13", "0101");              // Local Transaction Date
                    template.put("32", "123456");            // Acquiring Institution ID
                    template.put("37", "000000000001");      // Retrieval Reference Number
                    template.put("41", "TERM0001");          // Terminal ID
                    template.put("42", "MERCHANT001");       // Merchant ID
                    break;
                case "REVERSAL_REQUEST_0400":
                case "0400":
                    template.put("2", "4000000000000002");
                    template.put("3", "000000");
                    template.put("4", "000000001000");
                    template.put("7", "0101120000");
                    template.put("11", "000002");
                    template.put("32", "123456");
                    template.put("37", "000000000001");
                    template.put("90", "0200000001120000000001000001"); // Original Data Elements
                    break;
                case "NETWORK_REQUEST_0800":
                case "0800":
                    template.put("7", "0101120000");
                    template.put("11", "000003");
                    template.put("70", "301");  // Network Management Information Code
                    break;
                default:
                    // Template vacío para tipos desconocidos
                    break;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("messageType", messageType);
            response.put("fields", template);
            response.put("mode", config.getMode().getCode());
            response.put("fieldsCount", template.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting message template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // ENDPOINTS DE UTILIDAD
    // ================================

    @Operation(summary = "Información del sistema",
            description = "Obtiene información general del sistema y configuración")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {

        try {
            Map<String, Object> info = new HashMap<>();
            info.put("applicationName", "ISO8583 Simulator");
            info.put("version", "1.0.0");
            info.put("mode", config.getMode().getCode());
            info.put("modeDescription", config.getMode().getDescription());
            info.put("profiles", System.getProperty("spring.profiles.active", "default"));
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("serverPort", 8081);
            info.put("timestamp", LocalDateTime.now());

            // Capacidades según el modo
            Map<String, Boolean> capabilities = new HashMap<>();
            capabilities.put("mockSimulation", config.getMode().isMockEnabled());
            capabilities.put("realConnection", config.getMode().isRealEnabled());
            capabilities.put("dynamicModeSwitch", config.isDynamicModeChange());
            info.put("capabilities", capabilities);

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            logger.error("Error getting system info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}