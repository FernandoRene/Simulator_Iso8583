package com.iso8583.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class Iso8583SimulatorApplication {

    private static final Logger logger = LoggerFactory.getLogger(Iso8583SimulatorApplication.class);

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        SpringApplication app = new SpringApplication(Iso8583SimulatorApplication.class);
        app.run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");

        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("No se pudo determinar la dirección del host");
        }

        String[] activeProfiles = env.getActiveProfiles();
        String profiles = activeProfiles.length == 0 ? "default" : String.join(", ", activeProfiles);

        logger.info("""
                
                ----------------------------------------------------------
                🚀 ISO8583 Simulator está ejecutándose!
                ----------------------------------------------------------
                  Local:       http://localhost:{}{}
                  Externo:     http://{}:{}{}
                  Perfil:      {}
                  Swagger UI:  http://localhost:{}/swagger-ui.html
                  API Docs:    http://localhost:{}/api-docs
                  Health:      http://localhost:{}/api/v1/simulator/health
                ----------------------------------------------------------
                💡 Prueba los endpoints en Swagger UI para comenzar!
                ----------------------------------------------------------
                """,
                serverPort, contextPath,
                hostAddress, serverPort, contextPath,
                profiles,
                serverPort,
                serverPort,
                serverPort
        );
    }
}