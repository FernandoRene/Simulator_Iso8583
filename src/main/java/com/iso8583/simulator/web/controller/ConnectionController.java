package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.core.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador SIMPLIFICADO para manejo de conexión PSEUDO-MUX
 * Versión LIMPIA sin errores de compilación
 */
@RestController
@RequestMapping("/api/v1/connection")
@CrossOrigin(origins = "*")
public class ConnectionController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);

    @Autowired
    private ConnectionManager connectionManager;

    /**
     * Conectar manualmente al autorizador
     */
    @PostMapping("/connect")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> connect() {
        logger.info("🔄 Solicitud de conexión manual recibida");

        return connectionManager.connect()
                .thenApply(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", success ?
                            "Conexión establecida exitosamente" :
                            "Error estableciendo conexión");
                    response.put("timestamp", System.currentTimeMillis());

                    if (success) {
                        logger.info("✅ Conexión manual exitosa");
                        return ResponseEntity.ok(response);
                    } else {
                        logger.error("❌ Conexión manual falló");
                        response.put("error", connectionManager.getLastError());
                        return ResponseEntity.status(500).body(response);
                    }
                });
    }

    /**
     * Desconectar del autorizador
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        logger.info("🔌 Solicitud de desconexión recibida");

        Map<String, Object> response = new HashMap<>();

        try {
            connectionManager.disconnect();
            response.put("success", true);
            response.put("message", "Desconectado exitosamente");
            response.put("timestamp", System.currentTimeMillis());

            logger.info("✅ Desconexión manual exitosa");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error desconectando");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());

            logger.error("❌ Error en desconexión manual: {}", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Obtener estado detallado de la conexión
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        logger.debug("📊 Consultando estado de conexión");

        ConnectionManager.ConnectionStatus status = connectionManager.getConnectionStatus();

        Map<String, Object> response = new HashMap<>();
        response.put("connected", status.isConnected());
        response.put("channelConnected", status.isChannelConnected());
        response.put("host", status.getHost());
        response.put("port", status.getPort());
        response.put("lastConnectionAttempt", status.getLastConnectionAttempt());
        response.put("lastError", status.getLastError());
        response.put("socketInfo", status.getSocketInfo());
        response.put("channelType", status.getChannelType());
        response.put("pendingRequestsCount", status.getPendingRequestsCount());
        response.put("outputKeys", connectionManager.getOutputKeys());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Test de conexión específico
     */
    @PostMapping("/test")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> testConnection() {
        logger.info("🧪 Test de conexión solicitado");

        return connectionManager.testConnection()
                .thenApply(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("testType", "network_management_0800");
                    response.put("message", success ?
                            "Test de conexión exitoso" :
                            "Test de conexión falló");
                    response.put("timestamp", System.currentTimeMillis());
                    response.put("host", connectionManager.getConnectionStatus().getHost());
                    response.put("port", connectionManager.getConnectionStatus().getPort());

                    if (success) {
                        logger.info("✅ Test de conexión exitoso");
                    } else {
                        logger.error("❌ Test de conexión falló");
                        response.put("error", connectionManager.getLastError());
                    }

                    return ResponseEntity.ok(response);
                });
    }

    /**
     * Limpiar buffer de requests pendientes
     */
    @PostMapping("/clear-buffer")
    public ResponseEntity<Map<String, Object>> clearBuffer() {
        logger.info("🧹 Limpiando buffer de requests pendientes");

        Map<String, Object> response = new HashMap<>();

        try {
            int pendingCount = connectionManager.getPendingRequestsCount();
            connectionManager.clearResponseBuffer();

            response.put("success", true);
            response.put("message", "Buffer limpiado exitosamente");
            response.put("clearedRequests", pendingCount);
            response.put("timestamp", System.currentTimeMillis());

            logger.info("✅ Buffer limpiado - {} requests pendientes eliminados", pendingCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error limpiando buffer");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());

            logger.error("❌ Error limpiando buffer: {}", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}