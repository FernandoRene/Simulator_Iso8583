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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

/**
 * ConnectionManager con PSEUDO-MUX para resolver STAN MISMATCH
 * Versión LIMPIA y FUNCIONAL sin duplicados
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
    private Thread responseListenerThread;
    private volatile boolean shouldKeepAlive = true;
    private volatile boolean shouldListen = true;

    // *** PSEUDO-MUX IMPLEMENTATION ***
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    private String[] outputKeys = {"3", "7", "11", "41"}; // Processing Code, DateTime, STAN, Response Code
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Clase para manejar requests pendientes
     */
    private static class PendingRequest {
        final ISOMsg request;
        final CompletableFuture<ISOMsg> future;
        final long timestamp;
        final String matchKey;

        PendingRequest(ISOMsg request, CompletableFuture<ISOMsg> future, String matchKey) {
            this.request = request;
            this.future = future;
            this.timestamp = System.currentTimeMillis();
            this.matchKey = matchKey;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            initializePackager();
            logger.info("🏭 ConnectionManager con PSEUDO-MUX inicializado para {}:{}",
                    config.getSwitch().getHost(), config.getSwitch().getPort());
            logger.info("🔧 OutputKeys para matching: [{}]", String.join(", ", outputKeys));
            logger.info("ℹ️ Usar /api/v1/connection/connect para conectar manualmente");
        } catch (Exception e) {
            logger.error("❌ Error inicializando ConnectionManager: {}", e.getMessage(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
        shouldKeepAlive = false;
        shouldListen = false;

        if (keepAliveThread != null && keepAliveThread.isAlive()) {
            keepAliveThread.interrupt();
        }
        if (responseListenerThread != null && responseListenerThread.isAlive()) {
            responseListenerThread.interrupt();
        }

        executorService.shutdown();
        disconnect();
    }

    /**
     * Establece conexión con Pseudo-MUX
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("🏦 Conectando con PSEUDO-MUX al autorizador {}:{}",
                        config.getSwitch().getHost(), config.getSwitch().getPort());

                lastConnectionAttempt.set(LocalDateTime.now());

                // Limpiar conexión anterior si existe
                disconnect();

                // Crear ASCIIChannel
                ASCIIChannel channel = new ASCIIChannel(
                        config.getSwitch().getHost(),
                        config.getSwitch().getPort(),
                        packager
                );

                channel.setTimeout(config.getSwitch().getTimeout());

                logger.debug("🔌 Intentando conectar canal...");
                channel.connect();

                if (!channel.isConnected()) {
                    throw new IOException("No se pudo establecer la conexión ASCIIChannel");
                }

                logger.debug("✅ Canal conectado exitosamente");
                currentChannel.set(channel);
                isConnected.set(true);
                lastError.set(null);

                // Iniciar listener de respuestas
                startResponseListener();
                startKeepAlive();

                logger.info("✅ Conexión establecida con PSEUDO-MUX - Matching por OutputKeys");
                return true;

            } catch (Exception e) {
                String errorMsg = "Error conectando al autorizador: " + e.getMessage();
                logger.error(errorMsg, e);
                lastError.set(errorMsg);
                isConnected.set(false);

                // Limpiar canal en caso de error
                currentChannel.set(null);
                return false;
            }
        });
    }

    /**
     * Inicia el listener de respuestas en thread separado
     */
    private void startResponseListener() {
        shouldListen = true;
        responseListenerThread = new Thread(() -> {
            logger.info("🔄 Response Listener iniciado para PSEUDO-MUX");

            while (shouldListen && isConnected.get()) {
                try {
                    ASCIIChannel channel = currentChannel.get();
                    if (channel != null && channel.isConnected()) {
                        // Recibir respuesta (puede hacer timeout normalmente)
                        ISOMsg response = channel.receive();

                        if (response != null) {
                            logger.debug("📥 Respuesta recibida, procesando...");
                            processResponse(response);
                        }
                    } else {
                        // Canal no disponible - pausa más larga para evitar spam
                        Thread.sleep(5000);
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout normal del socket - continuar sin logging
                    logger.debug("⏰ Socket timeout normal en ResponseListener");
                } catch (ISOException e) {
                    if (shouldListen && isConnected.get()) {
                        // Error real de ISO - reconectar puede ser necesario
                        logger.warn("⚠️ ISO Error en ResponseListener: {}", e.getMessage());
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (shouldListen) {
                        // Solo loggear errores realmente inesperados
                        if (!(e.getCause() instanceof java.net.SocketTimeoutException)) {
                            logger.error("❌ Error inesperado en ResponseListener: {}", e.getMessage());
                        }
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            logger.info("🛑 Response Listener detenido");
        });

        responseListenerThread.setDaemon(true);
        responseListenerThread.setName("ISO8583-ResponseListener");
        responseListenerThread.start();

        logger.debug("✅ Response Listener thread iniciado");
    }

    /**
     * Procesa respuesta recibida y la matchea con request pendiente
     */
    private void processResponse(ISOMsg response) {
        try {
            String responseMti = response.getMTI();
            String responseStan = response.getString(11);
            String responseCode = response.getString(39);

            logger.debug("📥 Respuesta recibida - MTI: {}, STAN: {}, Code: {}",
                    responseMti, responseStan, responseCode);

            // Generar clave de matching usando OutputKeys
            String matchKey = generateMatchKey(response);

            // Buscar request pendiente que coincida
            PendingRequest pendingRequest = findMatchingRequest(matchKey, response);

            if (pendingRequest != null) {
                // Match encontrado!
                pendingRequests.remove(pendingRequest.matchKey);

                long responseTime = System.currentTimeMillis() - pendingRequest.timestamp;

                logger.info("✅ MATCH ENCONTRADO - Request STAN: {}, Response STAN: {}, {}ms",
                        pendingRequest.request.getString(11), responseStan, responseTime);

                // Completar el future
                pendingRequest.future.complete(response);

            } else {
                logger.warn("⚠️ NO MATCH - Response STAN: {}, Match Key: {}", responseStan, matchKey);
                logger.debug("Requests pendientes: {}", pendingRequests.keySet());
            }

        } catch (Exception e) {
            logger.error("Error procesando respuesta: {}", e.getMessage(), e);
        }
    }

    /**
     * Busca request pendiente que coincida con la respuesta
     */
    private PendingRequest findMatchingRequest(String responseMatchKey, ISOMsg response) {
        // Buscar por match key exacto primero
        PendingRequest exactMatch = pendingRequests.get(responseMatchKey);
        if (exactMatch != null) {
            return exactMatch;
        }

        // Si no hay match exacto, buscar por STAN (fallback)
        String responseStan = response.getString(11);
        if (responseStan != null) {
            for (PendingRequest pending : pendingRequests.values()) {
                String requestStan = pending.request.getString(11);
                if (responseStan.equals(requestStan)) {
                    logger.debug("Match por STAN fallback: {}", responseStan);
                    return pending;
                }
            }
        }

        return null;
    }

    /**
     * Genera clave de matching usando OutputKeys
     */
    private String generateMatchKey(ISOMsg msg) {
        StringBuilder key = new StringBuilder();

        for (String field : outputKeys) {
            String value = msg.getString(Integer.parseInt(field));
            if (value != null) {
                key.append(field).append(":").append(value).append("|");
            }
        }

        return key.toString();
    }

    /**
     * Envía mensaje usando Pseudo-MUX
     */
    public CompletableFuture<ISOMsg> sendMessage(ISOMsg request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verificar conexión antes de continuar
                if (!isConnected.get()) {
                    throw new ISOException("No hay conexión activa con el autorizador");
                }

                ASCIIChannel channel = currentChannel.get();
                if (channel == null || !channel.isConnected()) {
                    logger.warn("⚠️ Canal desconectado, intentando reconectar...");

                    // Intentar reconexión automática con timeout
                    try {
                        if (connect().get(10, TimeUnit.SECONDS)) {
                            channel = currentChannel.get();
                            logger.info("✅ Reconexión exitosa");
                        } else {
                            throw new ISOException("No se pudo establecer conexión después de reconexión");
                        }
                    } catch (TimeoutException e) {
                        throw new ISOException("Timeout durante reconexión automática");
                    }
                }

                String requestStan = request.getString(11);
                String requestMti = request.getMTI();

                // Generar clave de matching para el request
                String matchKey = generateMatchKey(request);

                // Crear future para la respuesta
                CompletableFuture<ISOMsg> responseFuture = new CompletableFuture<>();

                // Registrar request pendiente
                PendingRequest pendingRequest = new PendingRequest(request, responseFuture, matchKey);
                pendingRequests.put(matchKey, pendingRequest);

                logger.info("📤 ENVIANDO - MTI: {}, STAN: {}, Match Key: {} [PSEUDO-MUX]",
                        requestMti, requestStan, matchKey);

                // Enviar request de forma sincronizada
                try {
                    synchronized (channel) {
                        channel.send(request);
                        logger.debug("📤 Mensaje enviado por canal");
                    }
                } catch (Exception sendError) {
                    // Limpiar request pendiente si falla el envío
                    pendingRequests.remove(matchKey);
                    throw new ISOException("Error enviando mensaje: " + sendError.getMessage(), sendError);
                }

                // Esperar respuesta con timeout
                long timeoutMs = config.getSwitch().getTimeout();

                try {
                    ISOMsg response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                    logger.debug("📥 Respuesta recibida para STAN: {}", requestStan);
                    return response;

                } catch (TimeoutException e) {
                    // Limpiar request pendiente en caso de timeout
                    pendingRequests.remove(matchKey);
                    logger.error("⏰ TIMEOUT - STAN: {}, {}ms", requestStan, timeoutMs);
                    throw new ISOException("Timeout: No se recibió respuesta del autorizador");
                }

            } catch (Exception e) {
                logger.error("❌ Error enviando mensaje: {}", e.getMessage(), e);

                if (e instanceof IOException) {
                    logger.info("🔄 Intentando reconexión automática...");
                    CompletableFuture.runAsync(this::reconnect);
                }

                throw new RuntimeException("Error enviando mensaje: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Limpia requests pendientes que han expirado
     */
    private void cleanupExpiredRequests() {
        long currentTime = System.currentTimeMillis();
        long timeoutMs = config.getSwitch().getTimeout();

        pendingRequests.entrySet().removeIf(entry -> {
            PendingRequest pending = entry.getValue();
            if (currentTime - pending.timestamp > timeoutMs) {
                logger.warn("🧹 Limpiando request expirado - STAN: {}",
                        pending.request.getString(11));
                pending.future.completeExceptionally(
                        new ISOException("Request expirado"));
                return true;
            }
            return false;
        });
    }

    /**
     * Desconecta del autorizador
     */
    public void disconnect() {
        try {
            logger.info("🔌 Iniciando desconexión...");

            shouldKeepAlive = false;
            shouldListen = false;
            isConnected.set(false);

            // Completar todos los requests pendientes con error
            int pendingCount = pendingRequests.size();
            if (pendingCount > 0) {
                logger.warn("⚠️ Completando {} requests pendientes con error", pendingCount);
                for (PendingRequest pending : pendingRequests.values()) {
                    pending.future.completeExceptionally(
                            new ISOException("Conexión cerrada"));
                }
                pendingRequests.clear();
            }

            // Detener Response Listener
            if (responseListenerThread != null && responseListenerThread.isAlive()) {
                logger.debug("🛑 Deteniendo Response Listener...");
                responseListenerThread.interrupt();
                try {
                    responseListenerThread.join(2000);
                    logger.debug("✅ Response Listener detenido");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("⚠️ Timeout deteniendo Response Listener");
                }
            }

            // Detener Keep Alive
            if (keepAliveThread != null && keepAliveThread.isAlive()) {
                logger.debug("🛑 Deteniendo Keep Alive...");
                keepAliveThread.interrupt();
                try {
                    keepAliveThread.join(1000);
                    logger.debug("✅ Keep Alive detenido");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("⚠️ Timeout deteniendo Keep Alive");
                }
            }

            // Desconectar canal
            ASCIIChannel channel = currentChannel.get();
            if (channel != null) {
                try {
                    if (channel.isConnected()) {
                        logger.debug("🔌 Desconectando canal ASCIIChannel...");
                        channel.disconnect();
                        logger.debug("✅ Canal desconectado");
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ Error desconectando canal: {}", e.getMessage());
                }
                currentChannel.set(null);
            }

            logger.info("✅ Desconexión completada");
        } catch (Exception e) {
            logger.error("❌ Error durante desconexión: {}", e.getMessage(), e);
        }
    }

    // *** MÉTODOS REQUERIDOS POR CONTROLLER ***

    public void clearResponseBuffer() {
        int clearedCount = pendingRequests.size();
        pendingRequests.clear();
        logger.info("🧹 Buffer limpiado - {} requests pendientes eliminados", clearedCount);
    }

    public void enableKeepAlive(int intervalMinutes) {
        long intervalMs = intervalMinutes * 60 * 1000L;

        try {
            config.getConnection().setKeepAliveInterval((int) intervalMs);

            if (keepAliveThread != null && keepAliveThread.isAlive()) {
                shouldKeepAlive = false;
                keepAliveThread.interrupt();
                Thread.sleep(1000);
            }

            startKeepAlive();
            logger.info("✅ Keep-alive habilitado - Intervalo: {} minutos", intervalMinutes);
        } catch (Exception e) {
            logger.warn("Error configurando keep-alive: {}", e.getMessage());
        }
    }

    /**
     * Test de conexión
     */
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected.get()) {
                    return false;
                }

                ISOMsg testMessage = createNetworkTestMessage();
                String testStan = testMessage.getString(11);

                logger.info("🔍 Test de conexión - STAN: {} [PSEUDO-MUX]", testStan);

                ISOMsg response = sendMessage(testMessage).get();

                if (response != null && "0810".equals(response.getMTI())) {
                    String responseCode = response.getString(39);
                    String responseStan = response.getString(11);

                    boolean success = "00".equals(responseCode) || responseCode == null;

                    if (success) {
                        logger.info("✅ Test de conexión EXITOSO - Response STAN: {}", responseStan);
                        return true;
                    } else {
                        logger.warn("⚠️ Test con advertencias - Code: {}", responseCode);
                        return false;
                    }
                }
                return false;

            } catch (Exception e) {
                logger.error("❌ Error probando conexión: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Crear mensaje de consulta de saldo sin PIN
     */
    public ISOMsg createBalanceInquiryMessageWithoutPIN(String pan, String track2, String terminalId,
                                                        String cardAcceptorId, String account) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");

        String stan = generateStan();

        msg.set(2, pan);
        msg.set(3, "301099");
        msg.set(7, getCurrentTransmissionDateTime());
        msg.set(11, stan);
        msg.set(12, getCurrentTime());
        msg.set(13, getCurrentDate());
        msg.set(14, "2709");
        msg.set(15, getCurrentDate());
        msg.set(18, "6011");
        msg.set(19, "068");
        msg.set(22, "051");
        msg.set(25, "02");
        msg.set(32, "409911");
        msg.set(35, track2);
        msg.set(37, generateRrn());
        msg.set(41, terminalId);
        msg.set(42, cardAcceptorId);
        msg.set(43, "SIMULADOR TEST TERMINAL");
        msg.set(49, "068");
        msg.set(102, account);

        logger.info("📝 Balance Inquiry - PAN: {}...{}, STAN: {} [PSEUDO-MUX]",
                pan.substring(0, 6), pan.substring(pan.length()-4), stan);

        return msg;
    }

    // *** MÉTODOS UTILITARIOS ***

    private void initializePackager() throws ISOException {
        try {
            packager = new GenericPackager("packagers/iso87ascii.xml");
            logger.info("Packager inicializado desde packagers/iso87ascii.xml");
        } catch (Exception e) {
            logger.warn("Packager personalizado no encontrado, usando genérico: {}", e.getMessage());
            try {
                packager = new GenericPackager("jar:packager/iso87ascii.xml");
                logger.info("Packager genérico inicializado");
            } catch (Exception e2) {
                logger.error("Error inicializando packager: {}", e2.getMessage(), e2);
                throw new ISOException("No se pudo inicializar packager", e2);
            }
        }
    }

    private void startKeepAlive() {
        long keepAliveInterval = 900000L; // 15 minutos por defecto

        try {
            keepAliveInterval = Math.max(config.getConnection().getKeepAliveInterval(), 900000L);
        } catch (Exception e) {
            logger.warn("⚠️ Usando keep-alive por defecto: 15 minutos");
        }

        if (keepAliveInterval > 0) {
            shouldKeepAlive = true;
            long finalKeepAliveInterval = keepAliveInterval;
            keepAliveThread = new Thread(() -> {
                while (shouldKeepAlive && isConnected.get()) {
                    try {
                        Thread.sleep(finalKeepAliveInterval);

                        if (isConnected.get()) {
                            logger.debug("🔄 Keep-alive programado...");
                            testConnection();

                            // Aprovechar para limpiar requests expirados
                            cleanupExpiredRequests();
                        }

                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        logger.warn("Error en keep-alive: {}", e.getMessage());
                    }
                }
            });
            keepAliveThread.setDaemon(true);
            keepAliveThread.setName("ISO8583-KeepAlive");
            keepAliveThread.start();

            logger.info("Keep-alive iniciado - Intervalo: {} minutos", keepAliveInterval / 60000);
        }
    }

    private void reconnect() {
        int maxAttempts = 3; // Default si config no está disponible
        long delay = 5000; // Default 5 segundos

        try {
            maxAttempts = config.getConnection().getMaxReconnectAttempts();
            delay = config.getSwitch().getRetry().getDelay();
        } catch (Exception e) {
            logger.warn("⚠️ Usando valores por defecto para reconexión: maxAttempts={}, delay={}ms", maxAttempts, delay);
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.info("🔄 Intento de reconexión {}/{}", attempt, maxAttempts);

                disconnect();
                Thread.sleep(delay);

                if (connect().get(30, TimeUnit.SECONDS)) {
                    logger.info("✅ Reconexión exitosa en intento {}", attempt);
                    return;
                } else {
                    logger.warn("❌ Reconexión falló en intento {}", attempt);
                }

            } catch (Exception e) {
                logger.warn("❌ Intento de reconexión {} falló: {}", attempt, e.getMessage());
            }
        }

        logger.error("💥 No se pudo reconectar después de {} intentos", maxAttempts);
    }

    private ISOMsg createNetworkTestMessage() throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0800");
        msg.set(7, getCurrentTransmissionDateTime());
        msg.set(11, generateStan());
        msg.set(70, "301");
        return msg;
    }

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

    // *** GETTERS Y CONFIGURACIÓN ***

    public boolean isConnected() { return isConnected.get(); }
    public String getLastError() { return lastError.get(); }
    public String[] getOutputKeys() { return outputKeys.clone(); }
    public int getPendingRequestsCount() { return pendingRequests.size(); }

    public void configureOutputKeys(String[] newOutputKeys) {
        if (newOutputKeys != null && newOutputKeys.length > 0) {
            this.outputKeys = newOutputKeys.clone();
            logger.info("🔧 OutputKeys configurados: [{}]", String.join(", ", this.outputKeys));
        } else {
            logger.warn("⚠️ OutputKeys inválidos, manteniendo configuración actual");
        }
    }

    public void disableKeepAlive() {
        shouldKeepAlive = false;
        if (keepAliveThread != null && keepAliveThread.isAlive()) {
            keepAliveThread.interrupt();
        }
        logger.info("🚫 Keep-alive DESHABILITADO");
    }

    public ConnectionStatus getConnectionStatus() {
        ConnectionStatus status = new ConnectionStatus();
        status.setConnected(isConnected.get());
        status.setHost(config.getSwitch().getHost());
        status.setPort(config.getSwitch().getPort());
        status.setLastConnectionAttempt(lastConnectionAttempt.get());
        status.setLastError(lastError.get());
        status.setChannelType("PSEUDO-MUX con OutputKeys");
        status.setPendingRequestsCount(pendingRequests.size());

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
        private int pendingRequestsCount;

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

        public int getPendingRequestsCount() { return pendingRequestsCount; }
        public void setPendingRequestsCount(int pendingRequestsCount) { this.pendingRequestsCount = pendingRequestsCount; }
    }
}