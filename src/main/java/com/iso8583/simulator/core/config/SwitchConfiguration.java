package com.iso8583.simulator.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "simulator")
public class SwitchConfiguration {
    private Map<String, SwitchConfig> switches;

    public Map<String, SwitchConfig> getSwitches() { return switches; }
    public void setSwitches(Map<String, SwitchConfig> switches) { this.switches = switches; }

    public SwitchConfig getSwitch(String name) {
        return switches != null ? switches.get(name) : null;
    }

    public static class SwitchConfig {
        private String name;
        private String description;
        private ConnectionConfig connection;
        private PackagerConfig packager;
        private ValidationConfig validation;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public ConnectionConfig getConnection() { return connection; }
        public void setConnection(ConnectionConfig connection) { this.connection = connection; }

        public PackagerConfig getPackager() { return packager; }
        public void setPackager(PackagerConfig packager) { this.packager = packager; }

        public ValidationConfig getValidation() { return validation; }
        public void setValidation(ValidationConfig validation) { this.validation = validation; }
    }

    public static class ConnectionConfig {
        private String host;
        private Integer port;
        private String protocol;
        private String channel;
        private Integer timeout;
        private Boolean keepAlive;

        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }

        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }

        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }

        public Boolean getKeepAlive() { return keepAlive; }
        public void setKeepAlive(Boolean keepAlive) { this.keepAlive = keepAlive; }
    }

    public static class PackagerConfig {
        private String type;
        private String configFile;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getConfigFile() { return configFile; }
        public void setConfigFile(String configFile) { this.configFile = configFile; }
    }

    public static class ValidationConfig {
        private String successResponseCode;
        private Integer responseField;
        private Integer[] requiredFields;

        public String getSuccessResponseCode() { return successResponseCode; }
        public void setSuccessResponseCode(String successResponseCode) { this.successResponseCode = successResponseCode; }

        public Integer getResponseField() { return responseField; }
        public void setResponseField(Integer responseField) { this.responseField = responseField; }

        public Integer[] getRequiredFields() { return requiredFields; }
        public void setRequiredFields(Integer[] requiredFields) { this.requiredFields = requiredFields; }
    }
}