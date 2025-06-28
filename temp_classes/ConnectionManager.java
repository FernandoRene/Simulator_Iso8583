package com.iso8583.simulator.core.connection;

import com.iso8583.simulator.core.config.SwitchConfiguration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<String, ConnectionPool> connectionPools = new ConcurrentHashMap<>();
    private final Map<String, GenericPackager> packagers = new ConcurrentHashMap<>();

    public void initializeSwitch(String switchName, SwitchConfiguration.SwitchConfig config) throws ISOException, IOException {
        logger.info("Initializing switch: {}", switchName);

        // Load packager
        GenericPackager packager = loadPackager(config.getPackager().getConfigFile());
        packagers.put(switchName, packager);

        // Create connection pool
        ConnectionPool pool = new ConnectionPool(config, packager);
        connectionPools.put(switchName, pool);

        logger.info("Switch {} initialized successfully", switchName);
    }

    public ISOMsg sendMessage(String switchName, ISOMsg message) throws ISOException, IOException {
        ConnectionPool pool = connectionPools.get(switchName);
        if (pool == null) {
            throw new IllegalArgumentException("Switch not configured: " + switchName);
        }

        return pool.sendMessage(message);
    }

    public GenericPackager getPackager(String switchName) {
        return packagers.get(switchName);
    }

    private GenericPackager loadPackager(String packagerFile) throws ISOException, IOException {
        ClassPathResource resource = new ClassPathResource(packagerFile);
        GenericPackager packager = new GenericPackager(resource.getInputStream());
        return packager;
    }

    public void closeAllConnections() {
        for (ConnectionPool pool : connectionPools.values()) {
            pool.closeAll();
        }
        connectionPools.clear();
    }

    public boolean isConnected(String switchName) {
        ConnectionPool pool = connectionPools.get(switchName);
        return pool != null && pool.hasAvailableConnections();
    }

    public Map<String, Object> getConnectionStats(String switchName) {
        ConnectionPool pool = connectionPools.get(switchName);
        return pool != null ? pool.getStats() : null;
    }
}