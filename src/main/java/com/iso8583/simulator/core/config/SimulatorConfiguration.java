package com.iso8583.simulator.core.config;

import com.iso8583.simulator.core.enums.SimulatorMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del simulador con soporte para alternancia Mock/Real
 */
@Component
@ConfigurationProperties(prefix = "iso8583.simulator")
public class SimulatorConfiguration {

    private SimulatorMode mode = SimulatorMode.MOCK; // Modo por defecto
    private boolean dynamicModeChange = true; // Permitir cambio dinámico

    private SwitchConfig switchConfig = new SwitchConfig();
    private MockConfig mockConfig = new MockConfig();
    private ConnectionConfig connectionConfig = new ConnectionConfig();
    private MonitoringConfig monitoringConfig = new MonitoringConfig();

    // Getters y Setters principales
    public SimulatorMode getMode() {
        return mode;
    }

    public void setMode(SimulatorMode mode) {
        this.mode = mode;
    }

    public void setMode(String mode) {
        this.mode = SimulatorMode.fromCode(mode);
    }

    public boolean isDynamicModeChange() {
        return dynamicModeChange;
    }

    public void setDynamicModeChange(boolean dynamicModeChange) {
        this.dynamicModeChange = dynamicModeChange;
    }

    public SwitchConfig getSwitch() {
        return switchConfig;
    }

    public void setSwitch(SwitchConfig switchConfig) {
        this.switchConfig = switchConfig;
    }

    public MockConfig getMock() {
        return mockConfig;
    }

    public void setMock(MockConfig mockConfig) {
        this.mockConfig = mockConfig;
    }

    public ConnectionConfig getConnection() {
        return connectionConfig;
    }

    public void setConnection(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public MonitoringConfig getMonitoring() {
        return monitoringConfig;
    }

    public void setMonitoring(MonitoringConfig monitoringConfig) {
        this.monitoringConfig = monitoringConfig;
    }

    // Métodos de conveniencia
    public boolean isMockMode() {
        return mode.isMockEnabled();
    }

    public boolean isRealMode() {
        return mode.isRealEnabled();
    }

    public boolean isHybridMode() {
        return mode == SimulatorMode.HYBRID;
    }

    /**
     * Configuración del switch/autorizador
     */
    public static class SwitchConfig {
        private String host = "172.16.1.211";
        private int port = 5105;
        private int timeout = 30000;
        private boolean enabled = true;
        private ConnectionPool connectionPool = new ConnectionPool();
        private Retry retry = new Retry();

        // Getters y Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public ConnectionPool getConnectionPool() { return connectionPool; }
        public void setConnectionPool(ConnectionPool connectionPool) { this.connectionPool = connectionPool; }

        public Retry getRetry() { return retry; }
        public void setRetry(Retry retry) { this.retry = retry; }

        public static class ConnectionPool {
            private int initialSize = 2;
            private int maxSize = 10;
            private long maxIdleTime = 300000;

            public int getInitialSize() { return initialSize; }
            public void setInitialSize(int initialSize) { this.initialSize = initialSize; }

            public int getMaxSize() { return maxSize; }
            public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

            public long getMaxIdleTime() { return maxIdleTime; }
            public void setMaxIdleTime(long maxIdleTime) { this.maxIdleTime = maxIdleTime; }
        }

        public static class Retry {
            private int maxAttempts = 3;
            private long delay = 1000;

            public int getMaxAttempts() { return maxAttempts; }
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

            public long getDelay() { return delay; }
            public void setDelay(long delay) { this.delay = delay; }
        }
    }

    /**
     * Configuración del modo mock
     */
    public static class MockConfig {
        private boolean enabled = true;
        private int minResponseTime = 50;
        private int maxResponseTime = 200;
        private double successRate = 0.95;
        private boolean realisticErrors = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getMinResponseTime() { return minResponseTime; }
        public void setMinResponseTime(int minResponseTime) { this.minResponseTime = minResponseTime; }

        public int getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(int maxResponseTime) { this.maxResponseTime = maxResponseTime; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public boolean isRealisticErrors() { return realisticErrors; }
        public void setRealisticErrors(boolean realisticErrors) { this.realisticErrors = realisticErrors; }
    }

    /**
     * Configuración de conexión
     */
    public static class ConnectionConfig {
        private int keepAliveInterval = 60000;
        private boolean autoReconnect = true;
        private int maxReconnectAttempts = 5;

        public int getKeepAliveInterval() { return keepAliveInterval; }
        public void setKeepAliveInterval(int keepAliveInterval) { this.keepAliveInterval = keepAliveInterval; }

        public boolean isAutoReconnect() { return autoReconnect; }
        public void setAutoReconnect(boolean autoReconnect) { this.autoReconnect = autoReconnect; }

        public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
        public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
    }

    /**
     * Configuración de monitoreo
     */
    public static class MonitoringConfig {
        private boolean enabled = true;
        private long metricsInterval = 60000;
        private boolean performanceTracking = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public long getMetricsInterval() { return metricsInterval; }
        public void setMetricsInterval(long metricsInterval) { this.metricsInterval = metricsInterval; }

        public boolean isPerformanceTracking() { return performanceTracking; }
        public void setPerformanceTracking(boolean performanceTracking) { this.performanceTracking = performanceTracking; }
    }
}