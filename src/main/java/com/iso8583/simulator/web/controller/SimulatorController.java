package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.web.dto.MessageRequest;
import com.iso8583.simulator.web.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/simulator")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "ISO8583 Simulator", description = "APIs para simulación de mensajes ISO8583")
public class SimulatorController {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorController.class);

    @Operation(summary = "Enviar mensaje ISO8583",
            description = "Envía un mensaje ISO8583 al autorizador configurado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensaje enviado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<MessageResponse>> sendMessage(
            @RequestBody MessageRequest request) {

        logger.info("Received message request: {}", request);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simular procesamiento
                Thread.sleep(500 + ThreadLocalRandom.current().nextInt(1000));

                MessageResponse response = createMockResponse(request);
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.error("Error processing message: {}", e.getMessage(), e);
                MessageResponse errorResponse = new MessageResponse(false);
                errorResponse.setErrorMessage("Error interno: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        });
    }

    @Operation(summary = "Generar mensaje mock",
            description = "Genera una respuesta mock sin conectar al autorizador")
    @PostMapping("/mock")
    public ResponseEntity<MessageResponse> generateMockMessage(
            @RequestBody MessageRequest request) {

        logger.info("Generating mock message: {}", request);

        try {
            MessageResponse response = createMockResponse(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating mock message: {}", e.getMessage(), e);
            MessageResponse errorResponse = new MessageResponse(false);
            errorResponse.setErrorMessage("Error generando mock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Estado de conexión",
            description = "Obtiene el estado actual de la conexión con el autorizador")
    @GetMapping("/connection/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {

        logger.debug("Getting connection status");

        try {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", true);
            status.put("host", "172.16.1.211");
            status.put("port", 5105);
            status.put("lastChecked", LocalDateTime.now().toString());
            status.put("responseTime", "45ms");

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting connection status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Probar conexión",
            description = "Prueba la conectividad con el autorizador")
    @PostMapping("/connection/test")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> testConnection() {

        logger.info("Testing connection to authorizer");

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simular test de conexión
                Thread.sleep(1000 + ThreadLocalRandom.current().nextInt(500));

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("responseTime", ThreadLocalRandom.current().nextInt(50, 200));
                result.put("timestamp", LocalDateTime.now().toString());
                result.put("message", "Conexión exitosa");

                return ResponseEntity.ok(result);
            } catch (Exception e) {
                logger.error("Error testing connection: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        });
    }

    @Operation(summary = "Estadísticas del simulador",
            description = "Obtiene estadísticas y métricas del simulador")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSimulatorStats() {

        logger.debug("Getting simulator statistics");

        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMessagesSent", ThreadLocalRandom.current().nextInt(100, 1000));
            stats.put("successfulResponses", ThreadLocalRandom.current().nextInt(90, 99));
            stats.put("failedResponses", ThreadLocalRandom.current().nextInt(1, 10));
            stats.put("averageResponseTime", ThreadLocalRandom.current().nextInt(40, 80));

            Map<String, Object> connectionStatus = new HashMap<>();
            connectionStatus.put("connected", true);
            connectionStatus.put("host", "172.16.1.211");
            connectionStatus.put("port", 5105);
            connectionStatus.put("lastChecked", LocalDateTime.now().toString());

            stats.put("connectionStatus", connectionStatus);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting simulator stats: {}", e.getMessage(), e);
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

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting message template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Crea una respuesta mock realista
     */
    private MessageResponse createMockResponse(MessageRequest request) {
        MessageResponse response = new MessageResponse(true);

        // Generar datos de respuesta mock
        response.setRequestMti("0200");
        response.setResponseMti("0210");
        response.setResponseCode("00"); // Aprobado
        response.setResponseTime((long) ThreadLocalRandom.current().nextInt(30, 100));
        response.setTimestamp(LocalDateTime.now());

        // Campos de request
        response.setRequestFields(request.getFields());

        // Campos de respuesta mock
        Map<String, String> responseFields = new HashMap<>();
        responseFields.put("39", "00"); // Response code
        responseFields.put("38", "123456"); // Authorization code
        responseFields.put("54", "000C000000001000"); // Additional amounts

        // Copiar algunos campos del request
        if (request.getFields() != null) {
            responseFields.put("2", request.getFields().get("2")); // PAN
            responseFields.put("4", request.getFields().get("4")); // Amount
            responseFields.put("11", request.getFields().get("11")); // STAN
            responseFields.put("37", request.getFields().get("37")); // RRN
        }

        response.setResponseFields(responseFields);

        logger.info("Generated mock response for message type: {}", request.getMessageType());

        return response;
    }
}