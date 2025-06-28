package com.iso8583.simulator.web.service;

import com.iso8583.simulator.core.config.SwitchConfiguration;
import com.iso8583.simulator.core.connection.ConnectionManager;
import com.iso8583.simulator.core.enums.MessageType;
import com.iso8583.simulator.core.enums.ResponseCode;
import com.iso8583.simulator.core.message.ISO8583MessageBuilder;
import com.iso8583.simulator.core.message.ISO8583MessageParser;
import com.iso8583.simulator.core.message.MessageValidator;
import com.iso8583.simulator.simulator.MessageSimulator;
import com.iso8583.simulator.simulator.ResponseGenerator;
import com.iso8583.simulator.web.dto.MessageRequest;
import com.iso8583.simulator.web.dto.MessageResponse;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class SimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorService.class);

    @Autowired
    private SwitchConfiguration switchConfig;

    @Autowired
    private ConnectionManager connectionManager;

    @Autowired
    private ISO8583MessageBuilder messageBuilder;

    @Autowired
    private ISO8583MessageParser messageParser;

    @Autowired
    private MessageValidator messageValidator;

    @Autowired
    private MessageSimulator messageSimulator;

    @Autowired
    private ResponseGenerator responseGenerator;

    /**
     * Envía un mensaje ISO8583 al autorizador
     */
    public CompletableFuture<MessageResponse> sendMessage(MessageRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Construir el mensaje ISO8583
                ISOMsg isoMsg = buildISOMessage(request);

                // Validar el mensaje
                if (!messageValidator.validate(isoMsg)) {
                    return createErrorResponse("Invalid message format", null);
                }

                // Enviar mensaje al autorizador
                long startTime = System.currentTimeMillis();
                ISOMsg response = messageSimulator.sendMessage(isoMsg);
                long responseTime = System.currentTimeMillis() - startTime;

                // Procesar respuesta
                if (response != null) {
                    return createSuccessResponse(isoMsg, response, responseTime);
                } else {
                    return createErrorResponse("No response from authorizer", isoMsg);
                }

            } catch (Exception e) {
                logger.error("Error sending message: {}", e.getMessage(), e);
                return createErrorResponse("Error: " + e.getMessage(), null);
            }
        });
    }

    /**
     * Genera un mensaje mock sin enviar al autorizador
     */
    public MessageResponse generateMockMessage(MessageRequest request) {
        try {
            // Construir mensaje ISO8583
            ISOMsg isoMsg = buildISOMessage(request);

            // Generar respuesta mock
            ISOMsg mockResponse = responseGenerator.generateResponse(isoMsg, ResponseCode.APPROVED);

            return createSuccessResponse(isoMsg, mockResponse, 50L); // Mock response time

        } catch (Exception e) {
            logger.error("Error generating mock message: {}", e.getMessage(), e);
            return createErrorResponse("Error generating mock: " + e.getMessage(), null);
        }
    }

    /**
     * Obtiene el estado de la conexión con el autorizador
     */
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            boolean isConnected = connectionManager.isConnected();
            status.put("connected", isConnected);
            status.put("host", switchConfig.getHost());
            status.put("port", switchConfig.getPort());
            status.put("lastChecked", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            if (isConnected) {
                status.put("connectionPool", connectionManager.getPoolStatus());
            }

        } catch (Exception e) {
            status.put("connected", false);
            status.put("error", e.getMessage());
        }

        return status;
    }

    /**
     * Prueba la conectividad con el autorizador
     */
    public CompletableFuture<Map<String, Object>> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();

            try {
                long startTime = System.currentTimeMillis();
                boolean success = connectionManager.testConnection();
                long responseTime = System.currentTimeMillis() - startTime;

                result.put("success", success);
                result.put("responseTime", responseTime);
                result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                if (success) {
                    result.put("message", "Connection test successful");
                } else {
                    result.put("message", "Connection test failed");
                }

            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error testing connection: " + e.getMessage());
                result.put("error", e.getMessage());
            }

            return result;
        });
    }

    /**
     * Obtiene estadísticas del simulador
     */
    public Map<String, Object> getSimulatorStats() {
        Map<String, Object> stats = new HashMap<>();

        // Estadísticas básicas
        stats.put("totalMessagesSent", messageSimulator.getTotalMessagesSent());
        stats.put("successfulResponses", messageSimulator.getSuccessfulResponses());
        stats.put("failedResponses", messageSimulator.getFailedResponses());
        stats.put("averageResponseTime", messageSimulator.getAverageResponseTime());

        // Estado de conexión
        stats.put("connectionStatus", getConnectionStatus());

        // Configuración actual
        Map<String, Object> config = new HashMap<>();
        config.put("host", switchConfig.getHost());
        config.put("port", switchConfig.getPort());
        config.put("timeout", switchConfig.getTimeout());
        stats.put("configuration", config);

        return stats;
    }

    /**
     * Construye un mensaje ISO8583 desde el request
     */
    private ISOMsg buildISOMessage(MessageRequest request) throws ISOException {
        MessageType msgType = MessageType.valueOf(request.getMessageType());

        ISOMsg isoMsg = messageBuilder.createMessage(msgType);

        // Agregar campos específicos del request
        if (request.getFields() != null) {
            for (Map.Entry<String, String> field : request.getFields().entrySet()) {
                int fieldNumber = Integer.parseInt(field.getKey());
                isoMsg.set(fieldNumber, field.getValue());
            }
        }

        return isoMsg;
    }

    /**
     * Crea una respuesta exitosa
     */
    private MessageResponse createSuccessResponse(ISOMsg request, ISOMsg response, long responseTime) {
        MessageResponse msgResponse = new MessageResponse();
        msgResponse.setSuccess(true);
        msgResponse.setResponseTime(responseTime);
        msgResponse.setTimestamp(LocalDateTime.now());

        try {
            // Datos del request
            Map<String, String> requestFields = messageParser.parseToMap(request);
            msgResponse.setRequestFields(requestFields);
            msgResponse.setRequestMti(request.getMTI());

            // Datos de la respuesta
            Map<String, String> responseFields = messageParser.parseToMap(response);
            msgResponse.setResponseFields(responseFields);
            msgResponse.setResponseMti(response.getMTI());

            // Código de respuesta
            if (response.hasField(39)) {
                msgResponse.setResponseCode(response.getString(39));
            }

        } catch (ISOException e) {
            logger.warn("Error parsing message fields: {}", e.getMessage());
        }

        return msgResponse;
    }

    /**
     * Crea una respuesta de error
     */
    private MessageResponse createErrorResponse(String errorMessage, ISOMsg request) {
        MessageResponse msgResponse = new MessageResponse();
        msgResponse.setSuccess(false);
        msgResponse.setErrorMessage(errorMessage);
        msgResponse.setTimestamp(LocalDateTime.now());

        if (request != null) {
            try {
                Map<String, String> requestFields = messageParser.parseToMap(request);
                msgResponse.setRequestFields(requestFields);
                msgResponse.setRequestMti(request.getMTI());
            } catch (ISOException e) {
                logger.warn("Error parsing request fields: {}", e.getMessage());
            }
        }

        return msgResponse;
    }
}