package com.iso8583.simulator.web.controller;

import com.iso8583.simulator.core.config.SimulatorConfiguration;
import com.iso8583.simulator.core.connection.ConnectionManager;
import com.iso8583.simulator.web.dto.ConnectionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador SIMPLIFICADO para manejo de conexion PSEUDO-MUX
 * Version LIMPIA sin errores de compilacion
 */
@RestController
@RequestMapping("/api/v1/connection")
@CrossOrigin(origins = "*")
public class ConnectionController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);

    @Autowired
    private ConnectionManager connectionManager;

    @Autowired
    private SimulatorConfiguration config;  //
    /**
     * Conectar manualmente al autorizador.
     * Siempre intenta la conexión TCP real - "Conectar" es una acción explícita
     * del usuario y no debe depender del modo global (mock/real/hybrid), que es
     * independiente y solo decide qué API usa el envío de transacciones.
     */
    @PostMapping("/connect")
    public ResponseEntity<ConnectionResponse> connect() {
        logger.info("ðŸ”„ Solicitud de conexion recibida");

        ConnectionResponse response = new ConnectionResponse();

        // ================================================================
        //   Conexion TCP al autorizador (siempre se intenta)
        // ================================================================
        String activeHost = connectionManager.getConnectionStatus().getHost();
        int activePort = connectionManager.getConnectionStatus().getPort();

        logger.info("  ==========================================");
        logger.info("  INICIANDO CONEXION TCP AL AUTORIZADOR");
        logger.info("  Autorizador: {}:{}", activeHost, activePort);
        logger.info("  Intentando conexion TCP...");

        try {
            boolean connected = connectionManager.connect().get();

            if (connected) {
                ConnectionManager.ConnectionStatus status = connectionManager.getConnectionStatus();

                logger.info("  ==========================================");
                logger.info("  CONEXION ESTABLECIDA EXITOSAMENTE");
                logger.info("  Autorizador: {}:{}", activeHost, activePort);
                logger.info("  Socket: {}", status.getSocketInfo());
                logger.info("  Canal: {} ACTIVO", status.getChannelType());
                logger.info("  Protocolo: PSEUDO-MUX con OutputKeys");
                logger.info("  ==========================================");

                response.setSuccess(true);
                response.setMode("REAL");
                response.setTcpConnectionRequired(true);
                response.setMessage("Connected to real authorizer");
                response.setSimulatorType("Real Authorizer");
                response.setAuthorizer(activeHost + ":" + activePort);
                response.setSocketInfo(status.getSocketInfo());
                response.setChannelConnected(status.isChannelConnected());
                response.setChannelType(status.getChannelType());

                logger.info("  Conexion REAL lista para transacciones");
                return ResponseEntity.ok(response);

            } else {
                logger.error("  ==========================================");
                logger.error("  ERROR: No se pudo establecer conexion TCP");
                logger.error("  Autorizador: {}:{}", activeHost, activePort);
                logger.error("  Error: {}", connectionManager.getLastError());
                logger.error("  ==========================================");

                response.setSuccess(false);
                response.setMode("REAL");
                response.setTcpConnectionRequired(true);
                response.setMessage("Failed to connect to real authorizer: " + connectionManager.getLastError());
                response.setSimulatorType("Real Authorizer");
                response.setAuthorizer(activeHost + ":" + activePort);
                response.setChannelConnected(false);

                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            logger.error("  ==========================================");
            logger.error("  EXCEPCION al conectar: {}", e.getMessage());
            logger.error("  ==========================================");

            response.setSuccess(false);
            response.setMode("REAL");
            response.setTcpConnectionRequired(true);
            response.setMessage("Exception connecting: " + e.getMessage());
            response.setChannelConnected(false);

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Desconectar del autorizador
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        logger.info("  Solicitud de desconexion recibida");

        Map<String, Object> response = new HashMap<>();

        try {
            connectionManager.disconnect();
            response.put("success", true);
            response.put("message", "Desconectado exitosamente");
            response.put("timestamp", System.currentTimeMillis());

            logger.info("  Desconexion manual exitosa");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error desconectando");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());

            logger.error("  Error en desconexion manual: {}", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Obtener estado detallado de la conexion
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        ConnectionManager.ConnectionStatus status = connectionManager.getConnectionStatus();

        Map<String, Object> response = new HashMap<>();
        response.put("connected", status.isConnected());
        response.put("channelConnected", status.isChannelConnected());
        response.put("mode", config.getMode().getCode());
        response.put("host", status.getHost());
        response.put("port", status.getPort());
        response.put("lastConnectionAttempt", status.getLastConnectionAttempt());
        response.put("lastError", status.getLastError());
        response.put("socketInfo", status.getSocketInfo());
        response.put("channelType", status.getChannelType());
        response.put("pendingRequests", status.getPendingRequestsCount());

        return ResponseEntity.ok(response);
    }

    /**
     * Información de solo lectura del perfil de switch activo (host, puerto, canal, packager).
     * No tiene contraparte de cambio - el perfil se cambia por configuración + reinicio.
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getActiveProfile() {
        return ResponseEntity.ok(connectionManager.getActiveProfileInfo());
    }

    /**
     * Test de conexion especifico
     */
    @PostMapping("/test")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> testConnection() {
        logger.info("ðŸ§ª Test de conexion solicitado");

        return connectionManager.testConnection()
                .thenApply(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("testType", "network_management_0800");
                    response.put("message", success ?
                            "Test de conexion exitoso" :
                            "Test de conexion fallo");
                    response.put("timestamp", System.currentTimeMillis());
                    response.put("host", connectionManager.getConnectionStatus().getHost());
                    response.put("port", connectionManager.getConnectionStatus().getPort());

                    if (success) {
                        logger.info("Test de conexion exitoso");
                    } else {
                        logger.error("Test de conexion fallo");
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
        logger.info("ðŸ§¹ Limpiando buffer de requests pendientes");

        Map<String, Object> response = new HashMap<>();

        try {
            int pendingCount = connectionManager.getPendingRequestsCount();
            connectionManager.clearResponseBuffer();

            response.put("success", true);
            response.put("message", "Buffer limpiado exitosamente");
            response.put("clearedRequests", pendingCount);
            response.put("timestamp", System.currentTimeMillis());

            logger.info("  Buffer limpiado - {} requests pendientes eliminados", pendingCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error limpiando buffer");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());

            logger.error("  Error limpiando buffer: {}", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}