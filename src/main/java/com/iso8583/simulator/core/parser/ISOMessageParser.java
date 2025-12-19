package com.iso8583.simulator.core.parser;

import com.iso8583.simulator.core.parser.catalog.FieldCatalog;
import com.iso8583.simulator.core.parser.model.ISOField;
import com.iso8583.simulator.core.parser.model.ParseResult;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//import javax.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de parsing de mensajes ISO8583 con soporte multi-packager
 * Proporciona conversión entre formatos y validación
 */
@Service
public class ISOMessageParser {

    private static final Logger logger = LoggerFactory.getLogger(ISOMessageParser.class);

    @Autowired
    private PackagerManager packagerManager;

    @PostConstruct
    public void init() {
        logger.info("✅ ISOMessageParser inicializado con PackagerManager");
        logger.info("   Packagers disponibles: {}", packagerManager.getAvailablePackagers());
        logger.info("   Packager por defecto: {}", packagerManager.getDefaultPackagerName());
    }

    /**
     * Convierte ISOMsg a ParseResult (usando packager por defecto)
     */
    public ParseResult parse(ISOMsg msg) {
        return parse(msg, null);
    }

    /**
     * Convierte ISOMsg a ParseResult (con packager específico)
     */
    public ParseResult parse(ISOMsg msg, String packagerName) {
        long startTime = System.currentTimeMillis();
        ParseResult result = new ParseResult(true);

        try {
            if (msg == null) {
                result.addError("Mensaje ISO es null");
                return result;
            }

            // Extraer MTI
            String mti = msg.getMTI();
            result.setMti(mti);

            // Metadata del mensaje
            parseMetadata(msg, result, packagerName);

            // Extraer todos los campos
            for (int i = 0; i <= 128; i++) {
                if (msg.hasField(i)) {
                    String value = msg.getString(i);
                    ISOField field = createField(i, value);
                    result.addField(field);
                }
            }

            // Raw message (hex dump)
            try {
                byte[] msgBytes = msg.pack();
                result.setRawMessage(ISOUtil.hexString(msgBytes));
            } catch (Exception e) {
                result.addWarning("No se pudo generar raw message: " + e.getMessage());
            }

            // Tiempo de parsing
            long parsingTime = System.currentTimeMillis() - startTime;
            result.getMetadata().setParsingTimeMs(parsingTime);

            logger.debug("✅ Mensaje parseado - MTI: {}, {} campos, {}ms",
                    mti, result.getFieldCount(), parsingTime);

        } catch (Exception e) {
            result.addError("Error parseando mensaje: " + e.getMessage());
            logger.error("❌ Error en parse(): {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Convierte Map<String, String> a ISOMsg (usando packager por defecto)
     */
    public ISOMsg buildFromMap(Map<String, String> fieldMap) throws ISOException {
        return buildFromMap(fieldMap, null);
    }

    /**
     * Convierte Map<String, String> a ISOMsg (con packager específico)
     */
    public ISOMsg buildFromMap(Map<String, String> fieldMap, String packagerName) throws ISOException {
        if (fieldMap == null || fieldMap.isEmpty()) {
            throw new ISOException("Field map está vacío");
        }

        GenericPackager packager = packagerManager.getPackager(packagerName);
        if (packager == null) {
            throw new ISOException("Packager no disponible: " + packagerName);
        }

        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);

        for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
            System.out.println("Clave: " + entry.getKey() + ", Valor: " + entry.getValue());
        }

        try {
            // Establecer MTI
            String mti = fieldMap.get("0");
            logger.warn("⚠️ Campo mti capturado: {}", mti);
            if (mti == null || mti.isEmpty()) {
                throw new ISOException("MTI es obligatorio revision log");
            }
            msg.setMTI(mti);

            // Establecer campos
            for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip MTI y campos especiales
                if ("mti".equalsIgnoreCase(key) || value == null || value.isEmpty()) {
                    continue;
                }

                try {
                    int fieldNumber = Integer.parseInt(key);
                    msg.set(fieldNumber, value);
                    logger.debug("Campo {} = {}", fieldNumber, value);
                } catch (NumberFormatException e) {
                    logger.warn("⚠️ Campo ignorado (no numérico): {}", key);
                }
            }

            String packagerUsed = packagerName != null ? packagerName : packagerManager.getDefaultPackagerName();
            logger.info("✅ ISOMsg construido con packager '{}' - MTI: {}, {} campos",
                    packagerUsed, mti, fieldMap.size() - 1);

            return msg;

        } catch (Exception e) {
            logger.error("❌ Error construyendo ISOMsg desde Map: {}", e.getMessage());
            throw new ISOException("Error construyendo mensaje: " + e.getMessage(), e);
        }
    }

    /**
     * Convierte mensaje raw (hex string o ASCII) a ParseResult
     */
    public ParseResult parseRaw(String rawMessage) {
        return parseRaw(rawMessage, null);
    }

    /**
     * Convierte mensaje raw con packager específico
     */
    public ParseResult parseRaw(String rawMessage, String packagerName) {
        ParseResult result = new ParseResult(true);

        try {
            GenericPackager packager = packagerManager.getPackager(packagerName);
            if (packager == null) {
                result.addError("Packager no disponible: " + packagerName);
                return result;
            }

            byte[] msgBytes;

            // Detectar si es hex o ASCII
            if (rawMessage.matches("[0-9A-Fa-f]+") && rawMessage.length() % 2 == 0) {
                // Es hex
                logger.debug("Detectado formato HEX");
                msgBytes = ISOUtil.hex2byte(rawMessage);
            } else {
                // Es ASCII
                logger.debug("Detectado formato ASCII");
                msgBytes = rawMessage.getBytes();
            }

            // Crear ISOMsg desde bytes
            ISOMsg msg = new ISOMsg();
            msg.setPackager(packager);
            msg.unpack(msgBytes);

            // Parse a resultado
            String packagerUsed = packagerName != null ? packagerName : packagerManager.getDefaultPackagerName();
            logger.info("✅ Mensaje raw parseado con packager '{}'", packagerUsed);

            return parse(msg, packagerName);

        } catch (Exception e) {
            result.addError("Error parseando mensaje raw: " + e.getMessage());
            logger.error("❌ Error en parseRaw(): {}", e.getMessage());
        }

        return result;
    }

    /**
     * Pretty print de un mensaje ISO para debugging
     */
    public String prettyPrint(ISOMsg msg) {
        return prettyPrint(msg, null);
    }

    /**
     * Pretty print con nombre de packager
     */
    public String prettyPrint(ISOMsg msg, String packagerName) {
        if (msg == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════════════════════════════╗\n");
        sb.append("║             ISO 8583 MESSAGE - PRETTY PRINT                    ║\n");
        sb.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        String packagerUsed = packagerName != null ? packagerName : packagerManager.getDefaultPackagerName();
        sb.append(String.format("Packager: %s\n", packagerUsed));
        sb.append("─".repeat(64)).append("\n\n");

        try {
            // MTI
            String mti = msg.getMTI();
            sb.append(String.format("MTI: %s (%s)\n", mti, getMTIDescription(mti)));
            sb.append("─".repeat(64)).append("\n\n");

            // Campos
            sb.append("CAMPOS PRESENTES:\n\n");

            for (int i = 2; i <= 128; i++) { // Skip 0 (MTI) y 1 (Bitmap)
                if (msg.hasField(i)) {
                    String value = msg.getString(i);
                    FieldCatalog.FieldDefinition def = FieldCatalog.getDefinition(i);

                    if (def != null) {
                        sb.append(String.format("[%03d] %-40s\n", i, def.getName()));
                        sb.append(String.format("      Type: %-20s Max Length: %d\n",
                                def.getTypeDescription(), def.getMaxLength()));
                        sb.append(String.format("      Value: %s\n", formatValue(value, i)));
                        sb.append(String.format("      Info: %s\n", def.getDescription()));
                    } else {
                        sb.append(String.format("[%03d] Campo no catalogado\n", i));
                        sb.append(String.format("      Value: %s\n", value));
                    }
                    sb.append("\n");
                }
            }

            // Raw bytes (footer)
            try {
                byte[] packed = msg.pack();
                sb.append("\n").append("─".repeat(64)).append("\n");
                sb.append("RAW MESSAGE (HEX):\n");
                sb.append(ISOUtil.hexString(packed)).append("\n");
                sb.append(String.format("Length: %d bytes\n", packed.length));
            } catch (Exception e) {
                sb.append("\n[Error generando raw message]\n");
            }

        } catch (Exception e) {
            sb.append("\n[Error generando pretty print: ").append(e.getMessage()).append("]\n");
        }

        return sb.toString();
    }

    /**
     * Convierte ISOMsg a JSON string (simple)
     */
    public String toJson(ISOMsg msg) {
        return toJson(msg, null);
    }

    /**
     * Convierte ISOMsg a JSON con packager específico
     */
    public String toJson(ISOMsg msg, String packagerName) {
        if (msg == null) {
            return "{}";
        }

        ParseResult result = parse(msg, packagerName);
        return toJson(result);
    }

    /**
     * Convierte ParseResult a JSON string
     */
    public String toJson(ParseResult result) {
        if (result == null) {
            return "{}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append(String.format("  \"success\": %s,\n", result.isSuccess()));
        json.append(String.format("  \"mti\": \"%s\",\n", result.getMti()));
        json.append("  \"fields\": {\n");

        List<Map.Entry<Integer, ISOField>> sortedFields = result.getFields().entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        for (int i = 0; i < sortedFields.size(); i++) {
            Map.Entry<Integer, ISOField> entry = sortedFields.get(i);
            ISOField field = entry.getValue();

            json.append(String.format("    \"%d\": \"%s\"", field.getFieldNumber(),
                    escapeJson(field.getValue())));

            if (i < sortedFields.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  },\n");
        json.append(String.format("  \"fieldCount\": %d,\n", result.getFieldCount()));
        json.append(String.format("  \"parsedAt\": \"%s\"\n", result.getParsedAt()));
        json.append("}");

        return json.toString();
    }

    /**
     * Valida un mensaje ISO8583
     */
    public ParseResult validate(ISOMsg msg) {
        return validate(msg, null);
    }

    /**
     * Valida con packager específico
     */
    public ParseResult validate(ISOMsg msg, String packagerName) {
        ParseResult result = parse(msg, packagerName);

        // Validaciones adicionales
        validateMTI(result);
        validateRequiredFields(result);
        validateFieldFormats(result);
        validateFieldLengths(result);

        return result;
    }

    /**
     * Dump completo del mensaje (similar a jPOS dump)
     */
    public String dump(ISOMsg msg) {
        if (msg == null) {
            return "null";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        msg.dump(ps, "");
        return baos.toString();
    }

    // ========================================================================
    // MÉTODOS PRIVADOS - HELPERS
    // ========================================================================

    private ISOField createField(int fieldNumber, String value) {
        FieldCatalog.FieldDefinition def = FieldCatalog.getDefinition(fieldNumber);

        ISOField.Builder builder = ISOField.builder()
                .fieldNumber(fieldNumber)
                .value(value);

        if (def != null) {
            builder.name(def.getName())
                    .description(def.getDescription())
                    .type(def.getType())
                    .length(def.getMaxLength());
        }

        return builder.build();
    }

    private void parseMetadata(ISOMsg msg, ParseResult result, String packagerName) {
        try {
            String mti = msg.getMTI();
            if (mti != null && mti.length() == 4) {
                result.getMetadata().setMessageClass(getMTIClass(mti));
                result.getMetadata().setMessageFunction(getMTIFunction(mti));
                result.getMetadata().setMessageOrigin(getMTIOrigin(mti));
            }

            String packagerUsed = packagerName != null ? packagerName : packagerManager.getDefaultPackagerName();
            result.getMetadata().setPackagerType(packagerUsed);
        } catch (Exception e) {
            logger.debug("Error extrayendo metadata: {}", e.getMessage());
        }
    }

    private void validateMTI(ParseResult result) {
        String mti = result.getMti();
        if (mti == null || mti.isEmpty()) {
            result.addError("MTI es obligatorio");
            return;
        }

        if (!mti.matches("\\d{4}")) {
            result.addError("MTI debe ser numérico de 4 dígitos");
        }

        // Validar primer dígito (versión)
        char version = mti.charAt(0);
        if (version != '0' && version != '1' && version != '2') {
            result.addWarning("MTI versión inusual: " + version);
        }
    }

    private void validateRequiredFields(ParseResult result) {
        String mti = result.getMti();

        // Campos comúnmente requeridos según MTI
        if (mti != null && mti.startsWith("02")) { // Financial transactions
            if (result.getFieldValue(2) == null) {
                result.addWarning("Campo 2 (PAN) generalmente requerido para transacciones financieras");
            }
            if (result.getFieldValue(3) == null) {
                result.addWarning("Campo 3 (Processing Code) generalmente requerido");
            }
        }
    }

    private void validateFieldFormats(ParseResult result) {
        for (ISOField field : result.getFields().values()) {
            FieldCatalog.FieldDefinition def = FieldCatalog.getDefinition(field.getFieldNumber());

            if (def != null) {
                String value = field.getValue();
                String type = def.getType();

                // Validar según tipo
                if ("n".equals(type) && !value.matches("\\d*")) {
                    result.addWarning("Campo " + field.getFieldNumber() +
                            " debe ser numérico: " + value);
                }
            }
        }
    }

    private void validateFieldLengths(ParseResult result) {
        for (ISOField field : result.getFields().values()) {
            FieldCatalog.FieldDefinition def = FieldCatalog.getDefinition(field.getFieldNumber());

            if (def != null) {
                String value = field.getValue();
                if (value != null && value.length() > def.getMaxLength()) {
                    result.addWarning("Campo " + field.getFieldNumber() +
                            " excede longitud máxima (" + def.getMaxLength() + "): " + value.length());
                }
            }
        }
    }

    private String getMTIDescription(String mti) {
        if (mti == null || mti.length() != 4) return "Invalid MTI";

        String version = getMTIClass(mti);
        String function = getMTIFunction(mti);
        String origin = getMTIOrigin(mti);

        return String.format("%s - %s - %s", version, function, origin);
    }

    private String getMTIClass(String mti) {
        switch (mti.charAt(1)) {
            case '0': return "Authorization";
            case '1': return "Financial";
            case '2': return "File Action";
            case '3': return "Reversal/Chargeback";
            case '4': return "Reconciliation";
            case '5': return "Batch Upload";
            case '6': return "Administrative";
            case '7': return "Fee Collection";
            case '8': return "Network Management";
            case '9': return "Reserved";
            default: return "Unknown";
        }
    }

    private String getMTIFunction(String mti) {
        switch (mti.charAt(2)) {
            case '0': return "Request";
            case '1': return "Request Response";
            case '2': return "Advice";
            case '3': return "Advice Response";
            case '4': return "Notification";
            case '5': return "Notification Acknowledgement";
            case '6': return "Instruction";
            case '7': return "Instruction Acknowledgement";
            case '8': return "Reserved for ISO";
            case '9': return "Reserved for ISO";
            default: return "Unknown";
        }
    }

    private String getMTIOrigin(String mti) {
        switch (mti.charAt(3)) {
            case '0': return "Acquirer";
            case '1': return "Acquirer Repeat";
            case '2': return "Issuer";
            case '3': return "Issuer Repeat";
            case '4': return "Other";
            case '5': return "Other Repeat";
            default: return "Unknown";
        }
    }

    private String formatValue(String value, int fieldNumber) {
        if (value == null) return "null";

        // Enmascarar campos sensibles
        if (fieldNumber == 2) { // PAN
            if (value.length() > 10) {
                return value.substring(0, 6) + "****" + value.substring(value.length() - 4);
            }
        } else if (fieldNumber == 52) { // PIN
            return "******* (encrypted)";
        }

        // Limitar longitud de visualización
        if (value.length() > 50) {
            return value.substring(0, 47) + "...";
        }

        return value;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ========================================================================
    // GETTERS PÚBLICOS
    // ========================================================================

    public PackagerManager getPackagerManager() {
        return packagerManager;
    }
}