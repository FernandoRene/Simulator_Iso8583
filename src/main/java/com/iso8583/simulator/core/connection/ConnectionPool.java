package com.iso8583.simulator.core.connection;

import org.jpos.iso.ISOChannel;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool de conexiones para manejo eficiente de canales ISO8583
 */
@Component
public class ConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private final BlockingQueue<ISOChannel> availableChannels;
    private final AtomicInteger activeConnections;
    private final int maxPoolSize;
    private final int initialPoolSize;
    private final long maxIdleTime;

    private String host;
    private int port;
    private GenericPackager packager;

    public ConnectionPool() {
        this.maxPoolSize = 10;
        this.initialPoolSize = 2;
        this.maxIdleTime = 300000; // 5 minutos
        this.availableChannels = new LinkedBlockingQueue<>();
        this.activeConnections = new AtomicInteger(0);
    }

    public void initialize(String host, int port, GenericPackager packager) {
        this.host = host;
        this.port = port;
        this.packager = packager;

        // Crear conexiones iniciales
        for (int i = 0; i < initialPoolSize; i++) {
            try {
                ISOChannel channel = createChannel();
                if (channel != null) {
                    availableChannels.offer(channel);
                }
            } catch (Exception e) {
                logger.warn("No se pudo crear conexión inicial {}: {}", i, e.getMessage());
            }
        }

        logger.info("Pool de conexiones inicializado con {} conexiones", availableChannels.size());
    }

    public ISOChannel getChannel() throws Exception {
        ISOChannel channel = availableChannels.poll();

        if (channel == null || !channel.isConnected()) {
            if (activeConnections.get() < maxPoolSize) {
                channel = createChannel();
                if (channel != null) {
                    activeConnections.incrementAndGet();
                }
            } else {
                throw new Exception("Pool de conexiones agotado");
            }
        }

        return channel;
    }

    public void returnChannel(ISOChannel channel) {
        if (channel != null && channel.isConnected()) {
            availableChannels.offer(channel);
        } else {
            activeConnections.decrementAndGet();
        }
    }

    public void closeChannel(ISOChannel channel) {
        if (channel != null) {
            try {
                channel.disconnect();
            } catch (Exception e) {
                logger.warn("Error cerrando canal: {}", e.getMessage());
            } finally {
                activeConnections.decrementAndGet();
            }
        }
    }

    private ISOChannel createChannel() throws Exception {
        try {
            ASCIIChannel channel = new ASCIIChannel(host, port, packager);
            channel.setTimeout(30000); // 30 segundos
            channel.connect();

            logger.debug("Nueva conexión creada a {}:{}", host, port);
            return channel;

        } catch (Exception e) {
            logger.error("Error creando canal: {}", e.getMessage());
            throw e;
        }
    }

    public boolean testConnection() {
        try {
            Socket socket = new Socket(host, port);
            socket.close();
            return true;
        } catch (Exception e) {
            logger.debug("Test de conexión falló: {}", e.getMessage());
            return false;
        }
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public int getAvailableConnections() {
        return availableChannels.size();
    }

    public String getPoolStatus() {
        return String.format("Activas: %d, Disponibles: %d, Máximo: %d",
                activeConnections.get(), availableChannels.size(), maxPoolSize);
    }

    public void shutdown() {
        logger.info("Cerrando pool de conexiones...");

        while (!availableChannels.isEmpty()) {
            ISOChannel channel = availableChannels.poll();
            closeChannel(channel);
        }

        logger.info("Pool de conexiones cerrado");
    }
}