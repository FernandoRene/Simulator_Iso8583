package com.iso8583.simulator.web.service;

import com.iso8583.simulator.core.config.SimulatorConfiguration;
import com.iso8583.simulator.core.connection.ConnectionManager;
import com.iso8583.simulator.core.enums.SimulatorMode;
import com.iso8583.simulator.simulator.MessageSimulator;
import com.iso8583.simulator.web.dto.MessageRequest;
import com.iso8583.simulator.web.dto.MessageResponse;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servicio principal del simulador con soporte completo Mock/Real
 */
@Service
public class SimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorService.class);

    @Autowired
    private SimulatorConfiguration config;

    @Autowired
    private MessageSimulator messageSimulator;

    @Autowired
    private ConnectionManager connectionManager;

    /**
     * Envía un mensaje usando el modo configurado (Mock o Real)
     */
    public CompletableFuture<MessageResponse> sendMessage(MessageRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Enviando mensaje en modo: {} - MTI: {}", config.getMode(), request.getMessageType());

                if (config.isMockMode()) {
                    return sendMockMessage(request);
                } else if (config.isRealMode()) {
                    return sendRealMessage(request);
                } else {
                    throw new IllegalStateException("Modo de simulador no válido: " + config.getMode());
                }

            } catch (Exception e) {
                logger.error("Error enviando mensaje: {}", e.getMessage(), e);
                MessageResponse errorResponse = new MessageResponse(false);
                errorResponse.setErrorMessage("Error interno: " + e.getMessage());
                return errorResponse;
            }
        });
    }

    /**
     * Envía mensaje en modo Mock
     */
    private MessageResponse sendMockMessage(MessageRequest request) {
        try {
            logger.debug("Procesando mensaje en modo MOCK");

            // Simular delay de red configurado
            int responseTime = ThreadLocalRandom.current().nextInt(
                    config.getMock().getMinResponseTime(),
                    config.getMock().getMaxResponseTime()
            );
            Thread.sleep(responseTime);

            // Crear mensaje ISO8583 para el simulador mock
            ISOMsg isoRequest = createISOMessage(request);
            ISOMsg isoResponse = messageSimulator.sendMessage(isoRequest);

            // Convertir respuesta ISO a DTO
            MessageResponse response = convertToMessageResponse(isoResponse, request);
            response.setResponseTime((long) responseTime);
            response.setMockMode(true);

            logger.debug("Mensaje MOCK procesado exitosamente en {}ms", responseTime);
            return response;

        } catch (Exception e) {
            logger.error("Error en modo mock: {}", e.getMessage(), e);
            MessageResponse errorResponse = new MessageResponse(false);
            errorResponse.setErrorMessage("Error mock: " + e.getMessage());
            errorResponse.setMockMode(true);
            return errorResponse;
        }
    }

    /**
     * Envía mensaje en modo Real (conexión al autorizador)
     */
    private MessageResponse sendRealMessage(MessageRequest request) {
        try {
            logger.debug("Procesando mensaje en modo REAL - Host: {}:{}",
                    config.getSwitch().getHost(), config.getSwitch().getPort());

            long startTime = System.currentTimeMillis();

            // Verificar conexión
            if (!connectionManager.isConnected()) {
                logger.warn("No hay conexión activa, intentando conectar...");
                boolean connected = connectionManager.connect().get();
                if (!connected) {
                    throw new RuntimeException("No se pudo establecer conexión con el autorizador");
                }
            }

            // Crear mensaje ISO8583
            ISOMsg isoRequest = createISOMessage(request);

            // Enviar al autorizador real
            ISOMsg isoResponse = connectionManager.sendMessage(isoRequest).get();

            long responseTime = System.currentTimeMillis() - startTime;

            // Convertir respuesta ISO a DTO
            MessageResponse response = convertToMessageResponse(isoResponse, request);
            response.setResponseTime(responseTime);
            response.setMockMode(false);

            logger.debug("Mensaje REAL procesado exitosamente en {}ms", responseTime);
            return response;

        } catch (Exception e) {
            logger.error("Error en modo real: {}", e.getMessage(), e);
            MessageResponse errorResponse = new MessageResponse(false);
            errorResponse.setErrorMessage("Error conexión real: " + e.getMessage());
            errorResponse.setMockMode(false);
            return errorResponse;
        }
    }

    /**
     * Cambia el modo del simulador dinámicamente
     */
    public boolean changeMode(SimulatorMode newMode) {
        if (!config.isDynamicModeChange()) {
            logger.warn("Cambio dinámico de modo deshabilitado");
            return false;
        }

        try {
            SimulatorMode oldMode = config.getMode();
            config.setMode(newMode);

            logger.info("Modo del simulador cambiado de {} a {}", oldMode, newMode);

            // Realizar acciones específicas según el modo
            if (newMode.isRealEnabled()) {
                initializeRealConnection();
            } else if (oldMode.isRealEnabled() && !newMode.isRealEnabled()) {
                // Si cambiamos de real a mock, desconectar
                connectionManager.disconnect();
            }

            return true;
        } catch (Exception e) {
            logger.error("Error cambiando modo del simulador: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Obtiene el estado actual del simulador
     */
    public Map<String, Object> getSimulatorStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("mode", config.getMode().getCode());
        status.put("modeDescription", config.getMode().getDescription());
        status.put("dynamicModeChangeEnabled", config.isDynamicModeChange());
        status.put("timestamp", LocalDateTime.now());

        // Estado de conexión según el modo
        if (config.isRealMode()) {
            status.put("connectionStatus", getRealConnectionStatus());
        } else {
            status.put("connectionStatus", getMockConnectionStatus());
        }

        // Estadísticas del simulador
        status.put("stats", getSimulatorStats());

        return status;
    }

    /**
     * Prueba la conexión según el modo actual
     */
    public CompletableFuture<Map<String, Object>> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();

            try {
                if (config.isRealMode()) {
                    return testRealConnection();
                } else {
                    return testMockConnection();
                }
            } catch (Exception e) {
                logger.error("Error probando conexión: {}", e.getMessage(), e);
                result.put("success", false);
                result.put("error", e.getMessage());
                result.put("timestamp", LocalDateTime.now());
                return result;
            }
        });
    }

    /**
     * Inicializa conexión automática en modo real
     */
    public CompletableFuture<Boolean> initializeConnection() {
        if (config.isRealMode()) {
            logger.info("Inicializando conexión automática en modo real");
            return connectionManager.connect();
        } else {
            logger.debug("Modo mock - no se requiere conexión real");
            return CompletableFuture.completedFuture(true);
        }
    }

    /**
     * Fuerza reconexión en modo real
     */
    public CompletableFuture<Boolean> forceReconnect() {
        if (config.isRealMode()) {
            logger.info("Forzando reconexión al autorizador");
            connectionManager.disconnect();
            return connectionManager.connect();
        } else {
            logger.warn("Reconexión solicitada en modo mock - ignorando");
            return CompletableFuture.completedFuture(false);
        }
    }

    // Métodos auxiliares privados

    private ISOMsg createISOMessage(MessageRequest request) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(request.getMessageType());

        if (request.getFields() != null) {
            for (Map.Entry<String, String> field : request.getFields().entrySet()) {
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

    private MessageResponse convertToMessageResponse(ISOMsg isoResponse, MessageRequest originalRequest) {
        try {
            MessageResponse response = new MessageResponse(true);
            response.setRequestMti(originalRequest.getMessageType());
            response.setResponseMti(isoResponse.getMTI());
            response.setResponseCode(isoResponse.getString(39));
            response.setTimestamp(LocalDateTime.now());
            response.setRequestFields(originalRequest.getFields());

            // Convertir campos de respuesta ISO a Map
            Map<String, String> responseFields = new HashMap<>();
            for (int i = 1; i <= 128; i++) {
                if (isoResponse.hasField(i)) {
                    responseFields.put(String.valueOf(i), isoResponse.getString(i));
                }
            }
            response.setResponseFields(responseFields);

            return response;
        } catch (Exception e) {
            logger.error("Error convirtiendo respuesta ISO: {}", e.getMessage(), e);
            MessageResponse errorResponse = new MessageResponse(false);
            errorResponse.setErrorMessage("Error procesando respuesta: " + e.getMessage());
            return errorResponse;
        }
    }

    private void initializeRealConnection() {
        if (config.getSwitch().isEnabled()) {
            logger.info("Inicializando conexión real al autorizador {}:{}",
                    config.getSwitch().getHost(), config.getSwitch().getPort());

            // Conectar de forma asíncrona
            connectionManager.connect().thenAccept(success -> {
                if (success) {
                    logger.info("Conexión real inicializada exitosamente");
                } else {
                    logger.warn("No se pudo inicializar la conexión real: {}",
                            connectionManager.getLastError());
                }
            });
        }
    }

    private Map<String, Object> getRealConnectionStatus() {
        ConnectionManager.ConnectionStatus status = connectionManager.getConnectionStatus();

        Map<String, Object> result = new HashMap<>();
        result.put("connected", status.isConnected());
        result.put("channelConnected", status.isChannelConnected());
        result.put("host", status.getHost());
        result.put("port", status.getPort());
        result.put("lastConnectionAttempt", status.getLastConnectionAttempt());
        result.put("lastError", status.getLastError());
        result.put("socketInfo", status.getSocketInfo());
        result.put("type", "real");

        return result;
    }

    private Map<String, Object> getMockConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", true);
        status.put("host", "mock-simulator");
        status.put("port", "N/A");
        status.put("lastChecked", LocalDateTime.now());
        status.put("type", "mock");
        status.put("responseTime", "45ms");
        return status;
    }

    private Map<String, Object> getSimulatorStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessagesSent", messageSimulator.getTotalMessagesSent());
        stats.put("successfulResponses", messageSimulator.getSuccessfulResponses());
        stats.put("failedResponses", messageSimulator.getFailedResponses());
        stats.put("averageResponseTime", messageSimulator.getAverageResponseTime());
        return stats;
    }

    private Map<String, Object> testRealConnection() {
        Map<String, Object> result = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            boolean success = connectionManager.testConnection().get();
            long responseTime = System.currentTimeMillis() - startTime;

            result.put("success", success);
            result.put("responseTime", responseTime + "ms");
            result.put("host", config.getSwitch().getHost());
            result.put("port", config.getSwitch().getPort());
            result.put("testType", "network_management");

            if (success) {
                result.put("message", "Conexión real funcionando correctamente");
            } else {
                result.put("message", "Test de conexión real falló");
                result.put("error", connectionManager.getLastError());
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error probando conexión real");
            result.put("error", e.getMessage());
        }

        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    private Map<String, Object> testMockConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            Thread.sleep(100);
            result.put("success", true);
            result.put("responseTime", ThreadLocalRandom.current().nextInt(30, 80) + "ms");
            result.put("message", "Conexión mock funcionando correctamente");
            result.put("testType", "mock_simulation");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("success", false);
            result.put("message", "Test mock interrumpido");
        }
        result.put("timestamp", LocalDateTime.now());
        return result;
    }
}