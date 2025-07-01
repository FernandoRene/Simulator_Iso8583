package com.iso8583.simulator.simulator;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulador de mensajes ISO8583 con respuestas personalizables
 */
@Component
public class MessageSimulator {

    private static final Logger logger = LoggerFactory.getLogger(MessageSimulator.class);

    private final AtomicInteger totalMessagesSent = new AtomicInteger(0);
    private final AtomicInteger successfulResponses = new AtomicInteger(0);
    private final AtomicInteger failedResponses = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    private GenericPackager packager;

    // Configuración personalizable de respuestas
    private Map<String, String> responseCodeOverrides = new HashMap<>();
    private Map<String, String> panBasedResponses = new HashMap<>();
    private Map<String, String> terminalBasedResponses = new HashMap<>();
    private double customSuccessRate = 0.95;
    private boolean useCustomResponseCodes = false;

    public MessageSimulator() {
        logger.info("MessageSimulator inicializado con capacidades de personalización");
        initializeCustomResponses();
    }

    /**
     * Envía un mensaje ISO8583 con respuestas personalizables
     */
    public ISOMsg sendMessage(ISOMsg request) throws ISOException {
        totalMessagesSent.incrementAndGet();

        try {
            // Simular delay de red
            long startTime = System.currentTimeMillis();
            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
            long responseTime = System.currentTimeMillis() - startTime;

            totalResponseTime.addAndGet(responseTime);

            // Crear respuesta personalizada
            ISOMsg response = createCustomResponse(request);

            // Determinar si es éxito o fallo basado en el código de respuesta
            String responseCode = response.getString(39);
            if ("00".equals(responseCode)) {
                successfulResponses.incrementAndGet();
                logger.debug("Mensaje exitoso - STAN: {}, Response Code: {}",
                        response.getString(11), responseCode);
            } else {
                failedResponses.incrementAndGet();
                logger.debug("Mensaje con error simulado - STAN: {}, Response Code: {} ({})",
                        response.getString(11), responseCode, getResponseCodeDescription(responseCode));
            }

            return response;

        } catch (Exception e) {
            failedResponses.incrementAndGet();
            logger.error("Error simulando envío de mensaje: {}", e.getMessage());
            throw new ISOException("Error simulando mensaje: " + e.getMessage());
        }
    }

    /**
     * Crea una respuesta personalizada basada en reglas configuradas
     */
    private ISOMsg createCustomResponse(ISOMsg request) throws ISOException {
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

            // **LÓGICA PERSONALIZABLE DE CÓDIGOS DE RESPUESTA**
            String responseCode = determineResponseCode(request);
            response.set(39, responseCode); // Response Code

            // Campos adicionales basados en el código de respuesta
            if ("00".equals(responseCode)) {
                // Transacción aprobada
                response.set(38, generateAuthCode()); // Authorization Code

                // Para consultas de saldo, agregar información de balance
                if ("301099".equals(request.getString(3))) {
                    response.set(54, generateBalanceInfo()); // Additional Amounts - Balance
                }

                // Para transacciones financieras, agregar balance
                if ("0200".equals(requestMti) && !"301099".equals(request.getString(3))) {
                    response.set(54, "000C000000010000"); // Additional Amounts
                }
            } else {
                // Transacción rechazada - no agregar authorization code
                logger.debug("Transacción rechazada simulada con código: {} - {}",
                        responseCode, getResponseCodeDescription(responseCode));
            }

            // Fecha/hora de transmisión actualizada
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
            response.set(7, currentDateTime);

            logger.debug("Respuesta mock creada - MTI: {} -> {}, Response Code: {}, STAN: {}",
                    requestMti, responseMti, responseCode, response.getString(11));

        } catch (Exception e) {
            logger.error("Error creando respuesta mock: {}", e.getMessage());
            throw new ISOException("Error creando respuesta mock", e);
        }

        return response;
    }

    /**
     * Determina el código de respuesta basado en reglas personalizables
     */
    private String determineResponseCode(ISOMsg request) throws ISOException {
        try {
            String pan = request.getString(2);
            String terminalId = request.getString(41);
            String amount = request.getString(4);
            String processingCode = request.getString(3);

            // 1. Verificar overrides específicos por PAN
            if (pan != null && panBasedResponses.containsKey(pan)) {
                return panBasedResponses.get(pan);
            }

            // 2. Verificar overrides por Terminal ID
            if (terminalId != null && terminalBasedResponses.containsKey(terminalId)) {
                return terminalBasedResponses.get(terminalId);
            }

            // 3. Verificar overrides por Processing Code
            if (processingCode != null && responseCodeOverrides.containsKey(processingCode)) {
                return responseCodeOverrides.get(processingCode);
            }

            // 4. Reglas de negocio simuladas
            if (pan != null) {
                // Simular fondos insuficientes para PAN que terminan en 51
                if (pan.endsWith("51")) {
                    return "51"; // Insufficient funds
                }

                // Simular tarjeta expirada para PAN que terminan en 54
                if (pan.endsWith("54")) {
                    return "54"; // Expired card
                }

                // Simular PIN incorrecto para PAN que terminan en 55
                if (pan.endsWith("55")) {
                    return "55"; // Incorrect PIN
                }

                // Simular tarjeta restringida para PAN que terminan en 62
                if (pan.endsWith("62")) {
                    return "62"; // Restricted card
                }

                // Simular transacción no permitida para PAN que terminan en 57
                if (pan.endsWith("57")) {
                    return "57"; // Transaction not permitted to cardholder
                }
            }

            // 5. Simular límites por monto
            if (amount != null) {
                try {
                    long amountValue = Long.parseLong(amount);
                    // Simular límite excedido para montos mayores a 50000 (500.00)
                    if (amountValue > 5000000) { // 50000.00 en centavos
                        return "61"; // Exceeds withdrawal amount limit
                    }
                } catch (NumberFormatException e) {
                    // Ignorar si el monto no es numérico
                }
            }

            // 6. Usar tasa de éxito personalizable
            if (useCustomResponseCodes) {
                if (ThreadLocalRandom.current().nextDouble() >= customSuccessRate) {
                    // Seleccionar código de error aleatorio
                    String[] errorCodes = {"05", "12", "13", "30", "51", "54", "55", "62", "91"};
                    return errorCodes[ThreadLocalRandom.current().nextInt(errorCodes.length)];
                }
            } else {
                // Comportamiento original
                if (ThreadLocalRandom.current().nextDouble() >= 0.95) {
                    return "05"; // Do not honor
                }
            }

            // 7. Default: transacción aprobada
            return "00"; // Approved

        } catch (Exception e) {
            logger.warn("Error determinando código de respuesta, usando '00': {}", e.getMessage());
            return "00";
        }
    }

    // ================================
    // MÉTODOS DE CONFIGURACIÓN PERSONALIZABLE
    // ================================

    /**
     * Configura un código de respuesta específico para un Processing Code
     */
    public void setResponseCodeForProcessingCode(String processingCode, String responseCode) {
        responseCodeOverrides.put(processingCode, responseCode);
        logger.info("Configurado código de respuesta {} para Processing Code {}",
                responseCode, processingCode);
    }

    /**
     * Configura un código de respuesta específico para un PAN
     */
    public void setResponseCodeForPan(String pan, String responseCode) {
        panBasedResponses.put(pan, responseCode);
        logger.info("Configurado código de respuesta {} para PAN {}...{}",
                responseCode, pan.substring(0, 6), pan.substring(pan.length()-4));
    }

    /**
     * Configura un código de respuesta específico para un Terminal ID
     */
    public void setResponseCodeForTerminal(String terminalId, String responseCode) {
        terminalBasedResponses.put(terminalId, responseCode);
        logger.info("Configurado código de respuesta {} para Terminal {}",
                responseCode, terminalId);
    }

    /**
     * Configura la tasa de éxito personalizada
     */
    public void setCustomSuccessRate(double successRate) {
        this.customSuccessRate = successRate;
        this.useCustomResponseCodes = true;
        logger.info("Configurada tasa de éxito personalizada: {}%", successRate * 100);
    }

    /**
     * Habilita/deshabilita el uso de códigos de respuesta personalizados
     */
    public void setUseCustomResponseCodes(boolean useCustom) {
        this.useCustomResponseCodes = useCustom;
        logger.info("Uso de códigos de respuesta personalizados: {}",
                useCustom ? "HABILITADO" : "DESHABILITADO");
    }

    /**
     * Limpia todas las configuraciones personalizadas
     */
    public void clearCustomConfigurations() {
        responseCodeOverrides.clear();
        panBasedResponses.clear();
        terminalBasedResponses.clear();
        customSuccessRate = 0.95;
        useCustomResponseCodes = false;
        logger.info("Configuraciones personalizadas limpiadas");
    }

    /**
     * Obtiene las configuraciones actuales
     */
    public Map<String, Object> getCustomConfigurations() {
        Map<String, Object> config = new HashMap<>();
        config.put("responseCodeOverrides", new HashMap<>(responseCodeOverrides));
        config.put("panBasedResponses", maskPanResponses());
        config.put("terminalBasedResponses", new HashMap<>(terminalBasedResponses));
        config.put("customSuccessRate", customSuccessRate);
        config.put("useCustomResponseCodes", useCustomResponseCodes);
        return config;
    }

    // ================================
    // MÉTODOS AUXILIARES
    // ================================

    private void initializeCustomResponses() {
        // Ejemplos predefinidos para testing
        // Uncomment these for default test scenarios
        /*
        setResponseCodeForPan("4000000000000051", "51"); // Insufficient funds
        setResponseCodeForPan("4000000000000054", "54"); // Expired card
        setResponseCodeForPan("4000000000000055", "55"); // Incorrect PIN
        setResponseCodeForTerminal("TESTTERM", "62");     // Restricted card
        */
    }

    private Map<String, String> maskPanResponses() {
        Map<String, String> masked = new HashMap<>();
        for (Map.Entry<String, String> entry : panBasedResponses.entrySet()) {
            String pan = entry.getKey();
            String maskedPan = pan.length() >= 8 ?
                    pan.substring(0, 6) + "******" + pan.substring(pan.length()-4) : pan;
            masked.put(maskedPan, entry.getValue());
        }
        return masked;
    }

    private String getResponseCodeDescription(String responseCode) {
        switch (responseCode) {
            case "00": return "Approved";
            case "01": return "Refer to card issuer";
            case "05": return "Do not honor";
            case "12": return "Invalid transaction";
            case "13": return "Invalid amount";
            case "30": return "Format error";
            case "51": return "Insufficient funds";
            case "54": return "Expired card";
            case "55": return "Incorrect PIN";
            case "57": return "Transaction not permitted to cardholder";
            case "62": return "Restricted card";
            case "61": return "Exceeds withdrawal amount limit";
            case "91": return "Issuer or switch inoperative";
            default: return "Unknown";
        }
    }

    private String generateBalanceInfo() {
        // Simular información de balance para consultas
        long balance = ThreadLocalRandom.current().nextLong(10000, 1000000); // Entre 100.00 y 10000.00
        return String.format("01C%012d", balance);
    }

    // Métodos existentes sin cambios...
    private String getResponseMti(String requestMti) {
        switch (requestMti) {
            case "0200": return "0210";
            case "0400": return "0410";
            case "0800": return "0810";
            default:
                try {
                    int mti = Integer.parseInt(requestMti);
                    return String.format("%04d", mti + 10);
                } catch (NumberFormatException e) {
                    return "0010";
                }
        }
    }

    private void copyFieldIfPresent(ISOMsg from, ISOMsg to, int fieldNumber) throws ISOException {
        if (from.hasField(fieldNumber)) {
            to.set(fieldNumber, from.getString(fieldNumber));
        }
    }

    private String generateAuthCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    // Getters para estadísticas
    public int getTotalMessagesSent() { return totalMessagesSent.get(); }
    public int getSuccessfulResponses() { return successfulResponses.get(); }
    public int getFailedResponses() { return failedResponses.get(); }
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