package com.iso8583.simulator.simulator;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulador de mensajes ISO8583 simplificado
 */
@Component
public class MessageSimulator {

    private static final Logger logger = LoggerFactory.getLogger(MessageSimulator.class);

    private final AtomicInteger totalMessagesSent = new AtomicInteger(0);
    private final AtomicInteger successfulResponses = new AtomicInteger(0);
    private final AtomicInteger failedResponses = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    private GenericPackager packager;

    public MessageSimulator() {
        logger.info("MessageSimulator inicializado en modo simplificado");
    }

    /**
     * Envía un mensaje ISO8583 (versión mock)
     */
    public ISOMsg sendMessage(ISOMsg request) throws ISOException {
        totalMessagesSent.incrementAndGet();

        try {
            // Simular delay de red
            long startTime = System.currentTimeMillis();
            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
            long responseTime = System.currentTimeMillis() - startTime;

            totalResponseTime.addAndGet(responseTime);

            // Crear respuesta mock
            ISOMsg response = createMockResponse(request);

            // Simular éxito/fallo (95% éxito)
            if (ThreadLocalRandom.current().nextDouble() < 0.95) {
                successfulResponses.incrementAndGet();
                logger.debug("Mensaje enviado exitosamente. Response time: {}ms", responseTime);
            } else {
                failedResponses.incrementAndGet();
                response.set(39, "05"); // Declined
                logger.debug("Mensaje simulado como fallido");
            }

            return response;

        } catch (Exception e) {
            failedResponses.incrementAndGet();
            logger.error("Error simulando envío de mensaje: {}", e.getMessage());
            throw new ISOException("Error simulando mensaje: " + e.getMessage());
        }
    }

    /**
     * Crea una respuesta mock basada en el request
     */
    private ISOMsg createMockResponse(ISOMsg request) throws ISOException {
        ISOMsg response = new ISOMsg();

        try {
            // Determinar MTI de respuesta
            String requestMti = request.getMTI();
            String responseMti = getResponseMti(requestMti);
            response.setMTI(responseMti);

            // Copiar campos esenciales del request
            copyFieldIfPresent(request, response, 2);  // PAN
            copyFieldIfPresent(request, response, 3);  // Processing Code
            copyFieldIfPresent(request, response, 4);  // Amount
            copyFieldIfPresent(request, response, 11); // STAN
            copyFieldIfPresent(request, response, 12); // Local Time
            copyFieldIfPresent(request, response, 13); // Local Date
            copyFieldIfPresent(request, response, 32); // Acquiring Institution
            copyFieldIfPresent(request, response, 37); // RRN
            copyFieldIfPresent(request, response, 41); // Terminal ID
            copyFieldIfPresent(request, response, 42); // Merchant ID

            // Agregar campos de respuesta
            response.set(39, "00"); // Response Code - Approved
            response.set(38, generateAuthCode()); // Authorization Code

            // Para transacciones financieras, agregar balance (opcional)
            if ("0200".equals(requestMti)) {
                response.set(54, "000C000000010000"); // Additional Amounts
            }

            // Fecha/hora de transmisión actualizada
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
            response.set(7, currentDateTime);

            logger.debug("Respuesta mock creada para MTI: {} -> {}", requestMti, responseMti);

        } catch (Exception e) {
            logger.error("Error creando respuesta mock: {}", e.getMessage());
            throw new ISOException("Error creando respuesta mock", e);
        }

        return response;
    }

    /**
     * Determina el MTI de respuesta basado en el MTI del request
     */
    private String getResponseMti(String requestMti) {
        switch (requestMti) {
            case "0200": // Financial Request
                return "0210"; // Financial Response
            case "0400": // Reversal Request
                return "0410"; // Reversal Response
            case "0800": // Network Management Request
                return "0810"; // Network Management Response
            default:
                // Para otros MTIs, incrementar en 10
                try {
                    int mti = Integer.parseInt(requestMti);
                    return String.format("%04d", mti + 10);
                } catch (NumberFormatException e) {
                    return "0010"; // Response genérico
                }
        }
    }

    /**
     * Copia un campo del request al response si existe
     */
    private void copyFieldIfPresent(ISOMsg from, ISOMsg to, int fieldNumber) throws ISOException {
        if (from.hasField(fieldNumber)) {
            to.set(fieldNumber, from.getString(fieldNumber));
        }
    }

    /**
     * Genera un código de autorización aleatorio
     */
    private String generateAuthCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    /**
     * Valida un mensaje ISO8583 básicamente
     */
    public boolean validateMessage(ISOMsg msg) {
        try {
            // Validaciones básicas
            if (msg == null) {
                return false;
            }

            if (msg.getMTI() == null || msg.getMTI().length() != 4) {
                logger.warn("MTI inválido: {}", msg.getMTI());
                return false;
            }

            // Para mensajes financieros, verificar campos obligatorios
            String mti = msg.getMTI();
            if ("0200".equals(mti) || "0400".equals(mti)) {
                if (!msg.hasField(2) || !msg.hasField(3) || !msg.hasField(11)) {
                    logger.warn("Campos obligatorios faltantes para MTI: {}", mti);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("Error validando mensaje: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Genera datos de ejemplo para testing
     */
    public ISOMsg createSampleMessage(String mti) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(mti);

        switch (mti) {
            case "0200": // Financial Request
                msg.set(2, "4000000000000002");    // PAN
                msg.set(3, "000000");              // Processing Code
                msg.set(4, "000000001000");        // Amount
                msg.set(11, generateStan());       // STAN
                msg.set(12, getCurrentTime());     // Local Time
                msg.set(13, getCurrentDate());     // Local Date
                msg.set(32, "123456");             // Acquiring Institution
                msg.set(37, generateRrn());        // RRN
                msg.set(41, "TERM0001");           // Terminal ID
                msg.set(42, "MERCHANT001");        // Merchant ID
                break;

            case "0800": // Network Management
                msg.set(11, generateStan());       // STAN
                msg.set(12, getCurrentTime());     // Local Time
                msg.set(13, getCurrentDate());     // Local Date
                msg.set(70, "301");                // Network Management Information
                break;
        }

        // Transmission Date/Time
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        msg.set(7, currentDateTime);

        return msg;
    }

    // Métodos utilitarios
    private String generateStan() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999999));
    }

    private String generateRrn() {
        return String.format("%012d", ThreadLocalRandom.current().nextLong(1, 999999999999L));
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
    }

    // Getters para estadísticas
    public int getTotalMessagesSent() {
        return totalMessagesSent.get();
    }

    public int getSuccessfulResponses() {
        return successfulResponses.get();
    }

    public int getFailedResponses() {
        return failedResponses.get();
    }

    public long getAverageResponseTime() {
        int total = totalMessagesSent.get();
        return total > 0 ? totalResponseTime.get() / total : 0;
    }

    public void resetStats() {
        totalMessagesSent.set(0);
        successfulResponses.set(0);
        failedResponses.set(0);
        totalResponseTime.set(0);
        logger.info("Estadísticas del simulador reiniciadas");
    }
}