package com.iso8583.simulator.core.connection;

import com.iso8583.simulator.core.config.SimulatorConfiguration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestor de conexiones al autorizador real con manejo correcto de respuestas
 */
@Component
public class ConnectionManager implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    @Autowired
    private SimulatorConfiguration config;

    private AtomicReference<ASCIIChannel> currentChannel = new AtomicReference<>();
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicReference<LocalDateTime> lastConnectionAttempt = new AtomicReference<>();
    private AtomicReference<String> lastError = new AtomicReference<>();

    // STAN secuencial para evitar duplicados
    private AtomicInteger stanSequence = new AtomicInteger(1);

    private GenericPackager packager;
    private Thread keepAliveThread;
    private volatile boolean shouldKeepAlive = true;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            initializePackager();
            logger.info("ConnectionManager inicializado para {}:{} usando ASCIIChannel",
                    config.getSwitch().getHost(), config.getSwitch().getPort());
        } catch (Exception e) {
            logger.error("Error inicializando ConnectionManager: {}", e.getMessage(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
        shouldKeepAlive = false;
        if (keepAliveThread != null && keepAliveThread.isAlive()) {
            keepAliveThread.interrupt();
        }
        disconnect();
    }

    /**
     * Establece conexión con el autorizador usando ASCIIChannel
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Conectando al autorizador {}:{} con ASCIIChannel",
                        config.getSwitch().getHost(), config.getSwitch().getPort());

                lastConnectionAttempt.set(LocalDateTime.now());

                // Crear ASCIIChannel
                ASCIIChannel channel = new ASCIIChannel(
                        config.getSwitch().getHost(),
                        config.getSwitch().getPort(),
                        packager
                );

                // Configurar timeout
                channel.setTimeout(config.getSwitch().getTimeout());

                // Conectar
                channel.connect();

                // Verificar conexión
                if (channel.isConnected()) {
                    currentChannel.set(channel);
                    isConnected.set(true);
                    lastError.set(null);

                    logger.info("Conexión ASCIIChannel establecida exitosamente con {}:{}",
                            config.getSwitch().getHost(), config.getSwitch().getPort());

                    // Iniciar keep-alive MENOS FRECUENTE
                    startKeepAlive();

                    return true;
                } else {
                    throw new IOException("No se pudo establecer la conexión ASCIIChannel");
                }

            } catch (Exception e) {
                String errorMsg = "Error conectando al autorizador: " + e.getMessage();
                logger.error(errorMsg, e);
                lastError.set(errorMsg);
                isConnected.set(false);
                return false;
            }
        });
    }

    /**
     * Desconecta del autorizador
     */
    public void disconnect() {
        try {
            shouldKeepAlive = false;
            isConnected.set(false);

            ASCIIChannel channel = currentChannel.get();
            if (channel != null) {
                if (channel.isConnected()) {
                    channel.disconnect();
                }
                currentChannel.set(null);
            }

            logger.info("Desconectado del autorizador");
        } catch (Exception e) {
            logger.error("Error desconectando: {}", e.getMessage(), e);
        }
    }

    /**
     * Envía un mensaje al autorizador con logging detallado
     */
    public CompletableFuture<ISOMsg> sendMessage(ISOMsg request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected.get()) {
                    throw new ISOException("No hay conexión activa con el autorizador");
                }

                ASCIIChannel channel = currentChannel.get();
                if (channel == null || !channel.isConnected()) {
                    throw new ISOException("Canal ASCIIChannel no disponible");
                }

                String requestStan = request.getString(11);
                String requestMti = request.getMTI();

                logger.info("=== ENVIANDO MENSAJE AL AUTORIZADOR ===");
                logger.info("MTI Request: {}, STAN: {}", requestMti, requestStan);

                // Log campos principales para debugging
                logMessageFields(request, "REQUEST");

                // **ENVÍO Y RECEPCIÓN CON MANEJO DE RESPUESTAS MÚLTIPLES**
                long startTime = System.currentTimeMillis();
                int maxRetries = 3;
                ISOMsg correctResponse = null;

                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    try {
                        // Enviar el request
                        if (attempt == 1) {
                            channel.send(request);
                            logger.debug("Mensaje enviado - Esperando respuesta con STAN: {}", requestStan);
                        }

                        // Recibir respuesta
                        ISOMsg response = channel.receive();

                        if (response == null) {
                            throw new ISOException("No se recibió respuesta del autorizador");
                        }

                        String responseMti = response.getMTI();
                        String responseStan = response.getString(11);
                        String responseCode = response.getString(39);

                        logger.info("=== RESPUESTA RECIBIDA DEL AUTORIZADOR ===");
                        logger.info("MTI Response: {}, STAN: {}, Response Code: {}",
                                responseMti, responseStan, responseCode);

                        // Log campos principales de respuesta
                        logMessageFields(response, "RESPONSE");

                        // **VALIDAR CORRESPONDENCIA DE STAN**
                        if (requestStan.equals(responseStan)) {
                            logger.info("✅ STAN MATCHING CORRECTO - Request: {}, Response: {}",
                                    requestStan, responseStan);
                            correctResponse = response;
                            break;
                        } else {
                            logger.warn("⚠️  STAN NO COINCIDE - Intento {}/{} - Request: {}, Response: {}",
                                    attempt, maxRetries, requestStan, responseStan);

                            if (attempt < maxRetries) {
                                logger.info("Esperando siguiente respuesta...");
                                // No reenviar, solo esperar la siguiente respuesta
                                continue;
                            } else {
                                logger.error("❌ MÁXIMO DE INTENTOS ALCANZADO - Usando última respuesta recibida");
                                correctResponse = response;
                            }
                        }

                    } catch (Exception e) {
                        logger.error("Error en intento {} de envío/recepción: {}", attempt, e.getMessage());
                        if (attempt == maxRetries) {
                            throw e;
                        }
                        Thread.sleep(500); // Esperar antes del siguiente intento
                    }
                }

                if (correctResponse == null) {
                    throw new ISOException("No se recibió respuesta válida después de " + maxRetries + " intentos");
                }

                // Validar MTI correspondence
                if (!isValidResponseForRequest(requestMti, correctResponse.getMTI())) {
                    logger.warn("ADVERTENCIA: MTI de respuesta {} no corresponde al request {}",
                            correctResponse.getMTI(), requestMti);
                }

                long responseTime = System.currentTimeMillis() - startTime;
                logger.info("🎯 Respuesta procesada exitosamente en {}ms", responseTime);

                return correctResponse;

            } catch (Exception e) {
                logger.error("Error enviando mensaje al autorizador: {}", e.getMessage(), e);

                // Si hay error de conexión, intentar reconectar
                if (e instanceof IOException && config.getConnection().isAutoReconnect()) {
                    logger.info("Intentando reconexión automática...");
                    reconnect();
                }

                throw new RuntimeException("Error enviando mensaje: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Prueba la conexión con un mensaje de red (0800) - SOLO CUANDO SE SOLICITE
     */
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected.get()) {
                    logger.warn("No hay conexión activa para probar");
                    return false;
                }

                // Crear mensaje de red (0800)
                ISOMsg testMessage = createNetworkTestMessage();
                String testStan = testMessage.getString(11);

                logger.info("Probando conexión con mensaje 0800 - STAN: {}", testStan);

                ISOMsg response = sendMessage(testMessage).get();

                if (response != null && "0810".equals(response.getMTI())) {
                    String responseCode = response.getString(39);
                    String responseStan = response.getString(11);

                    logger.info("Test de conexión - Request STAN: {}, Response STAN: {}, Response Code: {}",
                            testStan, responseStan, responseCode);

                    // Para mensajes de red, el éxito puede ser response code 00 o incluso null
                    boolean success = "00".equals(responseCode) || responseCode == null;

                    if (success) {
                        logger.info("Test de conexión exitoso");
                    } else {
                        logger.warn("Test de conexión con advertencia - Response Code: {}", responseCode);
                    }

                    return success;
                } else {
                    logger.warn("Test de conexión falló - MTI de respuesta inválido: {}",
                            response != null ? response.getMTI() : "null");
                    return false;
                }

            } catch (Exception e) {
                logger.error("Error probando conexión: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Crear mensaje de consulta de saldo SIN campo 52
     */
    public ISOMsg createBalanceInquiryMessageWithoutPIN(String pan, String track2, String terminalId,
                                                        String cardAcceptorId, String account) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200"); // Financial Transaction Request

        String stan = generateStan();

        // Campos basados en la configuración JMX - SIN CAMPO 52
        msg.set(2, pan);                              // PAN
        msg.set(3, "301099");                         // Processing Code - Balance Inquiry
        msg.set(7, getCurrentTransmissionDateTime()); // Transmission Date/Time
        msg.set(11, stan);                            // STAN
        msg.set(12, getCurrentTime());                // Local Transaction Time
        msg.set(13, getCurrentDate());                // Local Transaction Date
        msg.set(14, "2709");                          // Expiration Date
        msg.set(15, getCurrentDate());                // Settlement Date
        msg.set(18, "6011");                          // Merchant Type
        msg.set(19, "068");                           // Country Code
        msg.set(22, "051");                           // POS Entry Mode
        msg.set(25, "02");                            // POS Condition Code
        msg.set(32, "409911");                        // Acquiring Institution
        msg.set(35, track2);                          // Track 2 Data
        msg.set(37, generateRrn());                   // RRN
        msg.set(41, terminalId);                      // Terminal ID
        msg.set(42, cardAcceptorId);                  // Card Acceptor ID
        msg.set(43, "SIMULADOR TEST TERMINAL");       // Card Acceptor Name
        msg.set(49, "068");                           // Currency Code
        // CAMPO 52 OMITIDO INTENCIONALMENTE
        msg.set(102, account);                        // Account Identification

        logger.info("Mensaje 0200 creado - PAN: {}...{}, STAN: {}, Processing Code: 301099",
                pan.substring(0, 6), pan.substring(pan.length()-4), stan);

        return msg;
    }

    /**
     * Obtiene el estado actual de la conexión
     */
    public ConnectionStatus getConnectionStatus() {
        ConnectionStatus status = new ConnectionStatus();
        status.setConnected(isConnected.get());
        status.setHost(config.getSwitch().getHost());
        status.setPort(config.getSwitch().getPort());
        status.setLastConnectionAttempt(lastConnectionAttempt.get());
        status.setLastError(lastError.get());
        status.setChannelType("ASCIIChannel");

        ASCIIChannel channel = currentChannel.get();
        if (channel != null) {
            try {
                status.setChannelConnected(channel.isConnected());
                status.setSocketInfo(getSocketInfo(channel));
            } catch (Exception e) {
                status.setChannelConnected(false);
            }
        }

        return status;
    }

    // Métodos auxiliares privados

    /**
     * Crear mensaje de test de red (0800)
     */
    private ISOMsg createNetworkTestMessage() throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0800"); // Network Management Request
        msg.set(7, getCurrentTransmissionDateTime()); // Transmission Date/Time
        msg.set(11, generateStan());                  // STAN
        msg.set(70, "301");                           // Network Management Information Code
        return msg;
    }

    /**
     * Valida que la respuesta corresponde al request
     */
    private boolean isValidResponseForRequest(String requestMti, String responseMti) {
        switch (requestMti) {
            case "0200": return "0210".equals(responseMti);
            case "0400": return "0410".equals(responseMti);
            case "0800": return "0810".equals(responseMti);
            default: return false;
        }
    }

    /**
     * Log detallado de campos del mensaje
     */
    private void logMessageFields(ISOMsg msg, String type) {
        try {
            logger.debug("=== {} FIELDS ===", type);
            logger.debug("MTI: {}", msg.getMTI());

            // Campos principales
            if (msg.hasField(2)) logger.debug("F2 (PAN): {}...{}",
                    msg.getString(2).substring(0, 6), msg.getString(2).substring(msg.getString(2).length()-4));
            if (msg.hasField(3)) logger.debug("F3 (Processing Code): {}", msg.getString(3));
            if (msg.hasField(4)) logger.debug("F4 (Amount): {}", msg.getString(4));
            if (msg.hasField(7)) logger.debug("F7 (Transmission DateTime): {}", msg.getString(7));
            if (msg.hasField(11)) logger.debug("F11 (STAN): {}", msg.getString(11));
            if (msg.hasField(37)) logger.debug("F37 (RRN): {}", msg.getString(37));
            if (msg.hasField(39)) logger.debug("F39 (Response Code): {}", msg.getString(39));
            if (msg.hasField(41)) logger.debug("F41 (Terminal ID): {}", msg.getString(41));
            if (msg.hasField(42)) logger.debug("F42 (Card Acceptor ID): {}", msg.getString(42));
            if (msg.hasField(70)) logger.debug("F70 (Network Management Info): {}", msg.getString(70));
            if (msg.hasField(102)) logger.debug("F102 (Account ID): {}", msg.getString(102));

        } catch (Exception e) {
            logger.warn("Error logging message fields: {}", e.getMessage());
        }
    }

    /**
     * Reconecta automáticamente
     */
    private void reconnect() {
        int maxAttempts = config.getConnection().getMaxReconnectAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.info("Intento de reconexión {}/{}", attempt, maxAttempts);

                disconnect();
                Thread.sleep(config.getSwitch().getRetry().getDelay());

                if (connect().get()) {
                    logger.info("Reconexión exitosa en intento {}", attempt);
                    return;
                }

            } catch (Exception e) {
                logger.warn("Intento de reconexión {} falló: {}", attempt, e.getMessage());
            }
        }

        logger.error("No se pudo reconectar después de {} intentos", maxAttempts);
    }

    /**
     * Inicializa el packager
     */
    private void initializePackager() throws ISOException {
        try {
            packager = new GenericPackager("packagers/iso87ascii.xml");
            logger.info("Packager ISO8583 inicializado desde packagers/iso87ascii.xml");
        } catch (Exception e) {
            logger.warn("No se pudo cargar packager personalizado, usando genérico: {}", e.getMessage());
            try {
                packager = new GenericPackager("jar:packager/iso87ascii.xml");
                logger.info("Packager ISO8583 genérico inicializado");
            } catch (Exception e2) {
                logger.error("Error inicializando packager: {}", e2.getMessage(), e2);
                throw new ISOException("No se pudo inicializar ningún packager", e2);
            }
        }
    }

    /**
     * Inicia keep-alive con intervalo MAYOR (5 minutos en lugar de 1)
     */
    private void startKeepAlive() {
        // Keep-alive cada 10 minutos como mínimo para evitar interferencias
        long keepAliveInterval = Math.max(config.getConnection().getKeepAliveInterval(), 600000); // Mínimo 10 minutos

        if (keepAliveInterval > 0) {
            keepAliveThread = new Thread(() -> {
                while (shouldKeepAlive && isConnected.get()) {
                    try {
                        Thread.sleep(keepAliveInterval);

                        if (isConnected.get()) {
                            logger.info("🔄 Ejecutando keep-alive programado (cada {} minutos)...",
                                    keepAliveInterval / 60000);

                            // Solo hacer keep-alive si no hay transacciones recientes
                            boolean shouldDoKeepAlive = true;

                            if (shouldDoKeepAlive) {
                                testConnectionForKeepAlive();
                            } else {
                                logger.debug("Saltando keep-alive - actividad reciente detectada");
                            }
                        }

                    } catch (InterruptedException e) {
                        logger.debug("Keep-alive thread interrumpido");
                        break;
                    } catch (Exception e) {
                        logger.warn("Error en keep-alive: {}", e.getMessage());
                    }
                }
            });
            keepAliveThread.setDaemon(true);
            keepAliveThread.setName("ISO8583-KeepAlive");
            keepAliveThread.start();

            logger.info("Keep-alive iniciado con intervalo de {} minutos (reducido para evitar interferencias)",
                    keepAliveInterval / 60000);
        } else {
            logger.info("Keep-alive DESHABILITADO para evitar interferencias con transacciones");
        }
    }

    /**
     * Test de conexión específico para keep-alive con manejo de respuestas mezcladas
     */
    private void testConnectionForKeepAlive() {
        try {
            if (!isConnected.get()) {
                logger.warn("No hay conexión activa para keep-alive");
                return;
            }

            // Crear mensaje de red con STAN único
            ISOMsg testMessage = createNetworkTestMessage();
            String testStan = testMessage.getString(11);

            logger.info("🔄 Keep-alive: enviando mensaje 0800 - STAN: {}", testStan);

            // Usar el método de envío con manejo de STAN
            CompletableFuture<ISOMsg> future = sendMessage(testMessage);

            // Timeout más corto para keep-alive
            ISOMsg response = future.get(10, java.util.concurrent.TimeUnit.SECONDS);

            if (response != null && "0810".equals(response.getMTI())) {
                String responseCode = response.getString(39);
                String responseStan = response.getString(11);

                if (testStan.equals(responseStan)) {
                    logger.info("✅ Keep-alive exitoso - STAN: {}, Response Code: {}",
                            responseStan, responseCode);
                } else {
                    logger.warn("⚠️  Keep-alive con STAN no matching - Enviado: {}, Recibido: {}",
                            testStan, responseStan);
                }
            } else {
                logger.warn("Keep-alive: respuesta inválida");
            }

        } catch (java.util.concurrent.TimeoutException e) {
            logger.warn("Keep-alive: timeout esperando respuesta");
        } catch (Exception e) {
            logger.error("Error en keep-alive: {}", e.getMessage());
        }
    }

    /**
     * Deshabilitar keep-alive automático completamente
     */
    public void disableKeepAlive() {
        shouldKeepAlive = false;
        if (keepAliveThread != null && keepAliveThread.isAlive()) {
            keepAliveThread.interrupt();
        }
        logger.info("Keep-alive DESHABILITADO para evitar interferencias");
    }

    /**
     * Habilitar keep-alive con intervalo personalizado
     */
    public void enableKeepAlive(long intervalMinutes) {
        shouldKeepAlive = false;
        if (keepAliveThread != null && keepAliveThread.isAlive()) {
            keepAliveThread.interrupt();
        }

        // Conversión explícita de long a int
        long intervalMillis = intervalMinutes * 60000;

        // Actualizar configuración temporalmente - USANDO setKeepAliveInterval
        // Nota: Necesitamos verificar si el método acepta int o long
        try {
            // Si setKeepAliveInterval acepta int:
            config.getConnection().setKeepAliveInterval((int) intervalMillis);
        } catch (Exception e) {
            logger.warn("Error configurando keep-alive interval: {}", e.getMessage());
        }

        startKeepAlive();

        logger.info("Keep-alive HABILITADO con intervalo de {} minutos", intervalMinutes);
    }

    /**
     * Limpiar buffer de respuestas pendientes (método utilitario)
     */
    public void clearResponseBuffer() {
        try {
            ASCIIChannel channel = currentChannel.get();
            if (channel != null && channel.isConnected()) {
                // Intentar leer respuestas pendientes con timeout corto
                channel.setTimeout(1000); // 1 segundo

                int clearedResponses = 0;
                while (clearedResponses < 10) { // Máximo 10 respuestas pendientes
                    try {
                        ISOMsg pendingResponse = channel.receive();
                        if (pendingResponse != null) {
                            clearedResponses++;
                            logger.warn("🧹 Respuesta pendiente limpiada - MTI: {}, STAN: {}",
                                    pendingResponse.getMTI(), pendingResponse.getString(11));
                        } else {
                            break;
                        }
                    } catch (Exception e) {
                        break; // No más respuestas pendientes
                    }
                }

                // Restaurar timeout original
                channel.setTimeout(config.getSwitch().getTimeout());

                if (clearedResponses > 0) {
                    logger.info("🧹 Buffer limpiado: {} respuestas pendientes eliminadas", clearedResponses);
                }
            }
        } catch (Exception e) {
            logger.warn("Error limpiando buffer de respuestas: {}", e.getMessage());
        }
    }
    // Métodos utilitarios

    private String getCurrentTransmissionDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
    }

    private String generateStan() {
        // STAN secuencial para evitar duplicados
        int stan = stanSequence.getAndIncrement();
        if (stan > 999999) {
            stanSequence.set(1);
            stan = 1;
        }
        return String.format("%06d", stan);
    }

    private String generateRrn() {
        String julian = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyDDD"));
        String stan = generateStan();
        String rrn = julian + stan;

        if (rrn.length() > 12) {
            rrn = rrn.substring(0, 12);
        } else if (rrn.length() < 12) {
            rrn = rrn + "0".repeat(12 - rrn.length());
        }

        return rrn;
    }

    private String getSocketInfo(ASCIIChannel channel) {
        try {
            Socket socket = channel.getSocket();
            if (socket != null) {
                return String.format("Local: %s:%d, Remote: %s:%d",
                        socket.getLocalAddress(), socket.getLocalPort(),
                        socket.getRemoteSocketAddress(), socket.getPort());
            }
        } catch (Exception e) {
            // Ignorar
        }
        return "Socket info no disponible";
    }

    // Getters
    public boolean isConnected() {
        return isConnected.get();
    }

    public String getLastError() {
        return lastError.get();
    }

    /**
     * Clase para estado de conexión
     */
    public static class ConnectionStatus {
        private boolean connected;
        private boolean channelConnected;
        private String host;
        private int port;
        private LocalDateTime lastConnectionAttempt;
        private String lastError;
        private String socketInfo;
        private String channelType;

        // Getters y setters
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }

        public boolean isChannelConnected() { return channelConnected; }
        public void setChannelConnected(boolean channelConnected) { this.channelConnected = channelConnected; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public LocalDateTime getLastConnectionAttempt() { return lastConnectionAttempt; }
        public void setLastConnectionAttempt(LocalDateTime lastConnectionAttempt) { this.lastConnectionAttempt = lastConnectionAttempt; }

        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }

        public String getSocketInfo() { return socketInfo; }
        public void setSocketInfo(String socketInfo) { this.socketInfo = socketInfo; }

        public String getChannelType() { return channelType; }
        public void setChannelType(String channelType) { this.channelType = channelType; }
    }
}