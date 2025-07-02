package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.core.connection.ConnectionManager;
import com.iso8583.simulator.core.config.SimulatorConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
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
@RequestMapping("/api/v1/real-connection")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Real Connection Manager", description = "APIs para gestión de conexión real al autorizador usando ASCIIChannel")
public class RealConnectionController {

    private static final Logger logger = LoggerFactory.getLogger(RealConnectionController.class);

    @Autowired
    private ConnectionManager connectionManager;

    @Autowired
    private SimulatorConfiguration config;

    // ================================
    // GESTIÓN DE CONEXIÓN
    // ================================

    @Operation(summary = "Conectar al autorizador",
            description = "Establece conexión ASCIIChannel con el autorizador real 172.16.1.211:5105")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conexión establecida exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error estableciendo conexión")
    })
    @PostMapping("/connect")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> connect() {

        logger.info("Solicitud de conexión ASCIIChannel al autorizador {}:{}",
                config.getSwitch().getHost(), config.getSwitch().getPort());

        return connectionManager.connect()
                .thenApply(success -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", success);
                    result.put("timestamp", LocalDateTime.now());
                    result.put("host", config.getSwitch().getHost());
                    result.put("port", config.getSwitch().getPort());
                    result.put("channelType", "ASCIIChannel");

                    if (success) {
                        result.put("message", "Conexión ASCIIChannel establecida exitosamente");
                        result.put("status", "connected");
                        return ResponseEntity.ok(result);
                    } else {
                        result.put("message", "No se pudo establecer la conexión ASCIIChannel");
                        result.put("error", connectionManager.getLastError());
                        result.put("status", "disconnected");
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Error en solicitud de conexión: {}", throwable.getMessage(), throwable);
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Error interno conectando");
                    error.put("error", throwable.getMessage());
                    error.put("timestamp", LocalDateTime.now());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                });
    }

    @Operation(summary = "Desconectar del autorizador",
            description = "Cierra la conexión ASCIIChannel con el autorizador")
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {

        try {
            logger.info("Solicitud de desconexión del autorizador");

            connectionManager.disconnect();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Desconectado exitosamente");
            result.put("status", "disconnected");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error desconectando: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error desconectando");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Estado de conexión real",
            description = "Obtiene el estado detallado de la conexión ASCIIChannel con el autorizador")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {

        try {
            ConnectionManager.ConnectionStatus status = connectionManager.getConnectionStatus();

            Map<String, Object> result = new HashMap<>();
            result.put("connected", status.isConnected());
            result.put("channelConnected", status.isChannelConnected());
            result.put("host", status.getHost());
            result.put("port", status.getPort());
            result.put("channelType", status.getChannelType());
            result.put("lastConnectionAttempt", status.getLastConnectionAttempt());
            result.put("lastError", status.getLastError());
            result.put("socketInfo", status.getSocketInfo());
            result.put("timestamp", LocalDateTime.now());
            result.put("currentMode", config.getMode().getCode());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error obteniendo estado de conexión: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // TESTING DE CONEXIÓN
    // ================================

    @Operation(summary = "Probar conexión",
            description = "Prueba la conexión enviando un mensaje de network management (0800)")
    @PostMapping("/test")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> testConnection() {

        logger.info("Probando conexión al autorizador con mensaje 0800");

        return connectionManager.testConnection()
                .thenApply(success -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", success);
                    result.put("timestamp", LocalDateTime.now());
                    result.put("testType", "network_management_0800");
                    result.put("host", config.getSwitch().getHost());
                    result.put("port", config.getSwitch().getPort());

                    if (success) {
                        result.put("message", "Test de conexión exitoso - Mensaje 0800 procesado");
                    } else {
                        result.put("message", "Test de conexión falló");
                        result.put("error", connectionManager.getLastError());
                    }

                    return ResponseEntity.ok(result);
                })
                .exceptionally(throwable -> {
                    logger.error("Error probando conexión: {}", throwable.getMessage(), throwable);
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Error en test de conexión");
                    error.put("error", throwable.getMessage());
                    error.put("timestamp", LocalDateTime.now());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                });
    }

    // ================================
    // TESTING DE TRANSACCIONES ESPECÍFICAS
    // ================================

    @Operation(summary = "Consulta de saldo",
            description = "Envía una consulta de saldo (0200) como la configurada en JMX")
    @PostMapping("/balance-inquiry")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> balanceInquiry(
            @RequestBody BalanceInquiryRequest request) {

        logger.info("Enviando consulta de saldo - PAN: {}...{}",
                request.getPan().substring(0, 6),
                request.getPan().substring(request.getPan().length()-4));

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connectionManager.isConnected()) {
                    throw new RuntimeException("No hay conexión activa con el autorizador");
                }

                // Crear mensaje de consulta de saldo como en JMX
                ISOMsg balanceMessage = connectionManager.createBalanceInquiryMessageWithoutPIN(
                        request.getPan(),
                        request.getTrack2(),
                        request.getTerminalId(),
                        request.getCardAcceptorId(),
                        request.getAccount()
                );

                logger.debug("Enviando consulta de saldo: STAN {}", balanceMessage.getString(11));

                // Enviar al autorizador
                ISOMsg response = connectionManager.sendMessage(balanceMessage).get();

                // Procesar respuesta
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Consulta de saldo procesada exitosamente");
                result.put("requestMti", balanceMessage.getMTI());
                result.put("responseMti", response.getMTI());
                result.put("responseCode", response.getString(39));
                result.put("stan", response.getString(11));
                result.put("rrn", response.getString(37));
                result.put("timestamp", LocalDateTime.now());

                // Campos adicionales de respuesta
                Map<String, String> responseFields = new HashMap<>();
                for (int i = 1; i <= 128; i++) {
                    if (response.hasField(i)) {
                        responseFields.put(String.valueOf(i), response.getString(i));
                    }
                }
                result.put("responseFields", responseFields);

                // Interpretar respuesta
                String responseCode = response.getString(39);
                if ("00".equals(responseCode)) {
                    result.put("transactionStatus", "APPROVED");
                    result.put("balanceInfo", extractBalanceInfo(response));
                } else {
                    result.put("transactionStatus", "DECLINED");
                    result.put("declineReason", getDeclineReason(responseCode));
                }

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                logger.error("Error enviando consulta de saldo: {}", e.getMessage(), e);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Error enviando consulta de saldo");
                error.put("error", e.getMessage());
                error.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        });
    }

    @Operation(summary = "Enviar transacción personalizada",
            description = "Envía una transacción personalizada al autorizador")
    @PostMapping("/send-custom")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendCustomTransaction(
            @RequestBody Map<String, Object> requestData) {

        logger.info("Enviando transacción personalizada al autorizador");

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connectionManager.isConnected()) {
                    throw new RuntimeException("No hay conexión activa con el autorizador");
                }

                // Crear mensaje desde los datos recibidos
                ISOMsg customMessage = createCustomMessage(requestData);

                logger.debug("Enviando mensaje personalizado MTI: {} - STAN: {}",
                        customMessage.getMTI(), customMessage.getString(11));

                // Enviar al autorizador
                ISOMsg response = connectionManager.sendMessage(customMessage).get();

                // Procesar respuesta
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Mensaje personalizado enviado exitosamente");
                result.put("requestMti", customMessage.getMTI());
                result.put("responseMti", response.getMTI());
                result.put("responseCode", response.getString(39));
                result.put("stan", response.getString(11));
                result.put("timestamp", LocalDateTime.now());

                // Campos de respuesta
                Map<String, String> responseFields = new HashMap<>();
                for (int i = 1; i <= 128; i++) {
                    if (response.hasField(i)) {
                        responseFields.put(String.valueOf(i), response.getString(i));
                    }
                }
                result.put("responseFields", responseFields);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                logger.error("Error enviando mensaje personalizado: {}", e.getMessage(), e);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Error enviando mensaje personalizado");
                error.put("error", e.getMessage());
                error.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        });
    }

    @Operation(summary = "Simular JMX Test",
            description = "Simula exactamente el test configurado en JMX con datos predefinidos")
    @PostMapping("/jmx-simulation")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> simulateJmxTest() {

        logger.info("Simulando test exacto de JMX con datos predefinidos");

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connectionManager.isConnected()) {
                    throw new RuntimeException("No hay conexión activa con el autorizador");
                }

                // Datos de ejemplo como los del JMX
                String testPan = "4000000000000002";
                String testTrack2 = "4000000000000002=27094321";
                String testTerminalId = "TERM0001";
                String testCardAcceptorId = "MERCHANT001";
                String testAccount = "1234567890";

                // Crear mensaje exacto como JMX
                ISOMsg jmxMessage = connectionManager.createBalanceInquiryMessageWithoutPIN(
                        testPan, testTrack2, testTerminalId, testCardAcceptorId, testAccount
                );

                logger.debug("Enviando simulación JMX: STAN {}", jmxMessage.getString(11));

                // Enviar al autorizador
                ISOMsg response = connectionManager.sendMessage(jmxMessage).get();

                // Procesar respuesta
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Simulación JMX ejecutada exitosamente");
                result.put("testType", "JMX_BALANCE_INQUIRY_SIMULATION");
                result.put("requestMti", jmxMessage.getMTI());
                result.put("responseMti", response.getMTI());
                result.put("responseCode", response.getString(39));
                result.put("stan", response.getString(11));
                result.put("rrn", response.getString(37));
                result.put("timestamp", LocalDateTime.now());

                // Datos de la transacción enviada
                Map<String, String> requestSummary = new HashMap<>();
                requestSummary.put("pan", maskPan(testPan));
                requestSummary.put("processingCode", "301099");
                requestSummary.put("terminalId", testTerminalId);
                requestSummary.put("cardAcceptorId", testCardAcceptorId);
                result.put("requestSummary", requestSummary);

                // Campos completos de respuesta
                Map<String, String> responseFields = new HashMap<>();
                for (int i = 1; i <= 128; i++) {
                    if (response.hasField(i)) {
                        responseFields.put(String.valueOf(i), response.getString(i));
                    }
                }
                result.put("responseFields", responseFields);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                logger.error("Error en simulación JMX: {}", e.getMessage(), e);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Error en simulación JMX");
                error.put("error", e.getMessage());
                error.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        });
    }

    @Operation(summary = "Prueba de rendimiento básica",
            description = "Ejecuta múltiples transacciones para probar rendimiento")
    @PostMapping("/performance-test")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> performanceTest(
            @Parameter(description = "Número de transacciones a enviar")
            @RequestParam(defaultValue = "10") int transactionCount) {

        logger.info("Iniciando prueba de rendimiento con {} transacciones", transactionCount);

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connectionManager.isConnected()) {
                    throw new RuntimeException("No hay conexión activa con el autorizador");
                }

                long startTime = System.currentTimeMillis();
                int successCount = 0;
                int errorCount = 0;
                long totalResponseTime = 0;

                for (int i = 1; i <= transactionCount; i++) {
                    try {
                        long txnStartTime = System.currentTimeMillis();

                        // Crear transacción de prueba
                        String testPan = "4000000000000002";
                        String testTrack2 = "4000000000000002=27094321";
                        String testTerminalId = "TERM000" + (i % 10);
                        String testCardAcceptorId = "MERCHANT001";
                        String testAccount = "123456789" + i;

                        ISOMsg testMessage = connectionManager.createBalanceInquiryMessageWithoutPIN(
                                testPan, testTrack2, testTerminalId, testCardAcceptorId, testAccount
                        );

                        ISOMsg response = connectionManager.sendMessage(testMessage).get();

                        long txnResponseTime = System.currentTimeMillis() - txnStartTime;
                        totalResponseTime += txnResponseTime;

                        if (response != null && "0210".equals(response.getMTI())) {
                            successCount++;
                        } else {
                            errorCount++;
                        }

                        logger.debug("Transacción {}/{} - STAN: {} - Tiempo: {}ms",
                                i, transactionCount, testMessage.getString(11), txnResponseTime);

                    } catch (Exception e) {
                        errorCount++;
                        logger.warn("Error en transacción {}: {}", i, e.getMessage());
                    }
                }

                long totalTime = System.currentTimeMillis() - startTime;

                // Resultados de la prueba
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Prueba de rendimiento completada");
                result.put("totalTransactions", transactionCount);
                result.put("successfulTransactions", successCount);
                result.put("failedTransactions", errorCount);
                result.put("successRate", (double) successCount / transactionCount * 100);
                result.put("totalTimeMs", totalTime);
                result.put("averageResponseTimeMs", successCount > 0 ? totalResponseTime / successCount : 0);
                result.put("transactionsPerSecond", (double) transactionCount / (totalTime / 1000.0));
                result.put("timestamp", LocalDateTime.now());

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                logger.error("Error en prueba de rendimiento: {}", e.getMessage(), e);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Error en prueba de rendimiento");
                error.put("error", e.getMessage());
                error.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        });
    }


    // ================================
    // MÉTODOS AUXILIARES
    // ================================

    private ISOMsg createCustomMessage(Map<String, Object> requestData) throws Exception {
        ISOMsg msg = new ISOMsg();

        // MTI obligatorio
        String mti = (String) requestData.get("mti");
        if (mti == null) {
            throw new IllegalArgumentException("MTI es obligatorio");
        }
        msg.setMTI(mti);

        // Campos opcionales
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) requestData.get("fields");
        if (fields != null) {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                try {
                    int fieldNumber = Integer.parseInt(field.getKey());
                    msg.set(fieldNumber, field.getValue());
                } catch (NumberFormatException e) {
                    logger.warn("Campo inválido ignorado: {}", field.getKey());
                }
            }
        }

        return msg;
    }

    private Map<String, Object> extractBalanceInfo(ISOMsg response) {
        Map<String, Object> balanceInfo = new HashMap<>();

        try {
            // Campo 54 - Additional Amounts (si existe)
            if (response.hasField(54)) {
                String additionalAmounts = response.getString(54);
                balanceInfo.put("additionalAmounts", additionalAmounts);
                balanceInfo.put("rawBalanceData", additionalAmounts);
            }

            // Otros campos de balance si existen
            if (response.hasField(4)) {
                balanceInfo.put("transactionAmount", response.getString(4));
            }

        } catch (Exception e) {
            logger.warn("Error extrayendo información de balance: {}", e.getMessage());
        }

        return balanceInfo;
    }

    private String getDeclineReason(String responseCode) {
        if (responseCode == null) return "Sin código de respuesta";

        // Códigos de respuesta ISO8583 comunes
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
            case "92": return "Financial institution or intermediate network facility cannot be found";
            case "94": return "Duplicate transmission";
            case "96": return "System malfunction";
            default: return "Unknown response code: " + responseCode;
        }
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) {
            return pan;
        }
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }

    // ================================
    // CLASES AUXILIARES
    // ================================

    public static class BalanceInquiryRequest {
        private String pan;
        private String track2;
        private String terminalId;
        private String cardAcceptorId;
        private String account;

        // Constructor por defecto
        public BalanceInquiryRequest() {}

        // Constructor con parámetros
        public BalanceInquiryRequest(String pan, String track2, String terminalId,
                                     String cardAcceptorId, String account) {
            this.pan = pan;
            this.track2 = track2;
            this.terminalId = terminalId;
            this.cardAcceptorId = cardAcceptorId;
            this.account = account;
        }

        // Getters y Setters
        public String getPan() { return pan; }
        public void setPan(String pan) { this.pan = pan; }

        public String getTrack2() { return track2; }
        public void setTrack2(String track2) { this.track2 = track2; }

        public String getTerminalId() { return terminalId; }
        public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

        public String getCardAcceptorId() { return cardAcceptorId; }
        public void setCardAcceptorId(String cardAcceptorId) { this.cardAcceptorId = cardAcceptorId; }

        public String getAccount() { return account; }
        public void setAccount(String account) { this.account = account; }

        @Override
        public String toString() {
            return String.format("BalanceInquiryRequest{pan='%s', terminalId='%s', cardAcceptorId='%s', account='%s'}",
                    maskPan(pan), terminalId, cardAcceptorId, account);
        }

        private String maskPan(String pan) {
            if (pan == null || pan.length() < 8) return pan;
            return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
        }
    }


    @Operation(summary = "Limpiar buffer de respuestas",
            description = "Limpia respuestas pendientes que puedan estar causando STAN mismatch")
    @PostMapping("/clear-buffer")
    public ResponseEntity<Map<String, Object>> clearResponseBuffer() {

        try {
            logger.info("Limpiando buffer de respuestas pendientes...");

            connectionManager.clearResponseBuffer();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Buffer de respuestas limpiado");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error limpiando buffer: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error limpiando buffer");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Deshabilitar keep-alive",
            description = "Desactiva el keep-alive automático para evitar interferencias")
    @PostMapping("/disable-keepalive")
    public ResponseEntity<Map<String, Object>> disableKeepAlive() {

        try {
            connectionManager.disableKeepAlive();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Keep-alive deshabilitado");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error deshabilitando keep-alive: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Configurar keep-alive",
            description = "Configura el intervalo de keep-alive en minutos")
    @PostMapping("/configure-keepalive")
    public ResponseEntity<Map<String, Object>> configureKeepAlive(
            @Parameter(description = "Intervalo en minutos (0 para deshabilitar)")
            @RequestParam int intervalMinutes) {

        try {
            if (intervalMinutes <= 0) {
                connectionManager.disableKeepAlive();
            } else {
                connectionManager.enableKeepAlive(intervalMinutes);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", intervalMinutes > 0 ?
                    "Keep-alive configurado a " + intervalMinutes + " minutos" :
                    "Keep-alive deshabilitado");
            result.put("intervalMinutes", intervalMinutes);
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error configurando keep-alive: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}