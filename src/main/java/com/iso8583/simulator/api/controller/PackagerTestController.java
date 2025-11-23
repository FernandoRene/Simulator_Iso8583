package com.iso8583.simulator.api.controller;

import com.iso8583.simulator.core.packager.PackagerService;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOFieldPackager;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller para testing y debugging de packagers ISO8583
 */
@RestController
@RequestMapping("/api/v1/packager")
public class PackagerTestController {

    private static final Logger logger = LoggerFactory.getLogger(PackagerTestController.class);

    @Autowired
    private PackagerService packagerService;

    /**
     * Listar packagers disponibles
     */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailablePackagers() {
        Map<String, Object> response = new HashMap<>();
        response.put("packagers", packagerService.getAvailablePackagers());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    /// Temporal
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeMessage(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Obtener el mensaje de diferentes formas posibles
            String rawMessage = null;

            if (request.containsKey("rawMessage")) {
                rawMessage = (String) request.get("rawMessage");
            } else if (request.containsKey("message")) {
                rawMessage = (String) request.get("message");
            } else if (request.containsKey("data")) {
                rawMessage = (String) request.get("data");
            } else {
                // Buscar cualquier string en el request
                for (Object value : request.values()) {
                    if (value instanceof String) {
                        String strValue = (String) value;
                        if (strValue.length() > 10) { // Asumir que es el mensaje si es suficientemente largo
                            rawMessage = strValue;
                            break;
                        }
                    }
                }
            }

            if (rawMessage == null || rawMessage.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "No se encontró mensaje en el request. Use 'rawMessage', 'message' o 'data'");
                return ResponseEntity.ok(result);
            }

            logger.info("🔍 Analizando mensaje: {} caracteres", rawMessage.length());

            // Convertir a bytes
            byte[] messageBytes = parseMessageBytes(rawMessage);

            result.put("totalLength", messageBytes.length);
            result.put("hexRepresentation", ISOUtil.hexString(messageBytes));
            result.put("inputLength", rawMessage.length());
            result.put("inputSample", rawMessage.substring(0, Math.min(50, rawMessage.length())) + "...");

            // Analizar MTI
            if (messageBytes.length >= 4) {
                String mti = new String(Arrays.copyOfRange(messageBytes, 0, 4), "ISO8859-1");
                result.put("mti", mti);
                logger.info("📋 MTI: {}", mti);
            }

            // Analizar Bitmap
            if (messageBytes.length >= 20) {
                byte[] bitmapBytes = Arrays.copyOfRange(messageBytes, 4, 20);
                String bitmapHex = ISOUtil.hexString(bitmapBytes);
                result.put("bitmapHex", bitmapHex);

                List<Integer> presentFields = decodeBitmapManual(bitmapBytes);
                result.put("presentFields", presentFields);

                logger.info("📊 Bitmap: {}", bitmapHex);
                logger.info("📍 Campos presentes: {}", presentFields);

                // Análisis detallado de estructura
                analyzeMessageStructureDetailed(messageBytes, presentFields, result);
            } else {
                result.put("error", "Mensaje demasiado corto. Mínimo 20 bytes requeridos (MTI + Bitmap)");
            }

            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("❌ Error en análisis: {}", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Análisis detallado de la estructura del mensaje
     */
    private void analyzeMessageStructureDetailed(byte[] messageBytes, List<Integer> presentFields, Map<String, Object> result) {
        List<Map<String, Object>> fieldAnalysis = new ArrayList<>();
        int position = 20; // Después de MTI + Bitmap

        for (int fieldNum : presentFields) {
            if (fieldNum == 1) continue; // Saltar bitmap

            Map<String, Object> fieldInfo = new HashMap<>();
            fieldInfo.put("fieldNumber", fieldNum);
            fieldInfo.put("position", position);

            // Longitud aproximada basada en el tipo de campo
            int approxLength = getApproximateFieldLength(fieldNum);
            fieldInfo.put("estimatedLength", approxLength);

            if (position < messageBytes.length) {
                int endPos = Math.min(position + approxLength, messageBytes.length);
                byte[] fieldData = Arrays.copyOfRange(messageBytes, position, endPos);
                String fieldValue = null;
                try {
                    fieldValue = new String(fieldData, "ISO8859-1");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }

                fieldInfo.put("rawData", ISOUtil.hexString(fieldData));
                fieldInfo.put("stringValue", fieldValue);
                fieldInfo.put("actualLength", fieldData.length);

                position += approxLength;
            } else {
                fieldInfo.put("error", "Posición excede longitud del mensaje");
            }

            fieldAnalysis.add(fieldInfo);
        }

        result.put("fieldAnalysis", fieldAnalysis);
        result.put("finalPosition", position);
        result.put("remainingBytes", messageBytes.length - position);
    }

    /**
     * Decodificar bitmap manualmente
     */
    private List<Integer> decodeBitmapManual(byte[] bitmapBytes) {
        List<Integer> presentFields = new ArrayList<>();

        try {
            // Bitmap primario (campos 1-64)
            for (int byteIndex = 0; byteIndex < bitmapBytes.length; byteIndex++) {
                byte b = bitmapBytes[byteIndex];
                for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                    if (((b >> (7 - bitIndex)) & 1) == 1) {
                        int fieldNumber = (byteIndex * 8) + bitIndex + 1;
                        if (fieldNumber > 1) { // El campo 1 es el bitmap mismo
                            presentFields.add(fieldNumber);
                        }
                    }
                }
            }

            // Verificar si hay bitmap secundario (campo 1 bit 1)
            if (presentFields.contains(1)) {
                // El bitmap tiene campo secundario, necesitaríamos analizar más bytes
                logger.info("⚠️ Bitmap secundario detectado - análisis extendido necesario");
            }

        } catch (Exception e) {
            logger.error("Error decodificando bitmap: {}", e.getMessage());
        }

        return presentFields;
    }

    /**
     * Longitudes aproximadas para análisis
     */
    private int getApproximateFieldLength(int fieldNum) {
        switch (fieldNum) {
            case 2: return 19;  // PAN - Primary Account Number (LLVAR)
            case 3: return 6;   // Processing Code
            case 4: return 12;  // Amount, Transaction
            case 5: return 12;  // Amount, Settlement
            case 6: return 12;  // Amount, Cardholder Billing
            case 7: return 10;  // Transmission Date and Time
            case 9: return 8;   // Conversion Rate, Settlement
            case 10: return 8;  // Conversion Rate, Cardholder Billing
            case 11: return 6;  // System Trace Audit Number
            case 12: return 6;  // Time, Local Transaction
            case 13: return 4;  // Date, Local Transaction
            case 14: return 4;  // Date, Expiration
            case 15: return 4;  // Date, Settlement
            case 16: return 4;  // Date, Conversion
            case 18: return 4;  // Merchant Type
            case 22: return 3;  // Point of Service Entry Mode
            case 25: return 2;  // Point of Service Condition Code
            case 32: return 11; // Acquiring Institution Ident Code (LLVAR)
            case 33: return 11; // Forwarding Institution Ident Code (LLVAR)
            case 35: return 37; // Track 2 Data (LLVAR)
            case 37: return 12; // Retrieval Reference Number
            case 38: return 6;  // Authorization Identification Response
            case 39: return 2;  // Response Code
            case 41: return 8;  // Card Acceptor Terminal Identification
            case 42: return 15; // Card Acceptor Identification Code
            case 43: return 40; // Card Acceptor Name/Location
            case 44: return 25; // Additional Response Data (LLVAR)
            case 45: return 76; // Track 1 Data (LLVAR)
            case 48: return 999; // Additional Data - Private (LLLVAR)
            case 49: return 3;  // Currency Code, Transaction
            case 52: return 64; // PIN Data (binary)
            case 55: return 999; // Reserved ISO (LLLVAR)
            case 60: return 999; // Reserved Private (LLLVAR)
            case 61: return 999; // Reserved Private (LLLVAR)
            case 62: return 999; // Reserved Private (LLLVAR)
            case 63: return 999; // Reserved Private (LLLVAR)
            case 64: return 64; // Message Authentication Code Field (binary)
            case 90: return 42; // Original Data Elements
            case 95: return 42; // Replacement Amounts
            case 100: return 11; // Receiving Institution Ident Code (LLVAR)
            case 102: return 28; // Account Identification 1 (LLVAR)
            case 103: return 28; // Account Identification 2 (LLVAR)
            default: return 10; // Longitud por defecto para campos desconocidos
        }
    }


    /**
     * Versión SIMPLIFICADA - Solo unpack normal
     */
    @PostMapping("/decode")
    public ResponseEntity<DecodeResult> decodeMessage(
            @RequestBody DecodeRequest request) {

        long startTime = System.currentTimeMillis();
        DecodeResult result = new DecodeResult();
        result.packagerUsed = request.packager != null ? request.packager : "iso87ascii";
        result.parsedAt = LocalDateTime.now();

        try {
            // Configurar encoding
            System.setProperty("file.encoding", "ISO8859-1");
            System.setProperty("jpos.encoding", "ISO8859-1");

            // Obtener packager
            GenericPackager packager = request.packager != null ?
                    packagerService.getPackager(request.packager) :
                    packagerService.getDefaultPackager();

            // Convertir mensaje a bytes
            byte[] messageBytes = parseMessageBytes(request.rawMessage);

            logger.info("📥 Decodificando mensaje con packager: {}", result.packagerUsed);
            logger.info("📏 Longitud total del mensaje: {} bytes", messageBytes.length);

            // Usar unpack manual que preserva espacios
            ISOMsg msg = unpackManualWithSpaces(packager, messageBytes);

            // Extraer información
            result.success = true;
            result.mti = msg.getMTI();
            result.fields = extractFieldsManual(msg, messageBytes);
            result.fieldCount = result.fields.size();

            // Metadata
            result.metadata.put("parsingTimeMs", System.currentTimeMillis() - startTime);
            result.metadata.put("messageLengthBytes", messageBytes.length);

            logger.info("✅ Mensaje procesado - MTI: {}, Campos: {}", result.mti, result.fieldCount);

        } catch (Exception e) {
            result.success = false;
            result.errors.add("Error: " + e.getMessage());
            logger.error("❌ Error: {}", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Unpack manual que preserva espacios en campos fijos
     */
    private ISOMsg unpackManualWithSpaces(GenericPackager packager, byte[] messageBytes) {
        ISOMsg msg = new ISOMsg();

        try {
            msg.setPackager(packager);

            // 1. Establecer MTI (primeros 4 bytes)
            if (messageBytes.length >= 4) {
                String mti = new String(Arrays.copyOfRange(messageBytes, 0, 4), "ISO8859-1");
                msg.setMTI(mti);
                logger.info("📋 MTI: {}", mti);
            }

            // 2. Establecer Bitmap (bytes 4-20)
            if (messageBytes.length >= 20) {
                byte[] bitmapBytes = Arrays.copyOfRange(messageBytes, 4, 20);
                msg.set(1, bitmapBytes);

                // 3. Decodificar bitmap para saber qué campos están presentes
                List<Integer> presentFields = decodeBitmapManual(bitmapBytes);
                logger.info("📍 Campos presentes: {}", presentFields);

                // 4. Procesar campos según el orden esperado
                int position = 20;

                // Orden de campos basado en tu análisis
                int[] fieldOrder = {2, 3, 4, 6, 7, 11, 12, 13, 14, 15, 18, 19, 22, 25, 32, 37, 41, 42, 43};

                for (int fieldNum : fieldOrder) {
                    if (presentFields.contains(fieldNum)) {
                        int fieldLength = getExactFieldLength(fieldNum);

                        if (position + fieldLength <= messageBytes.length) {
                            byte[] fieldData = Arrays.copyOfRange(messageBytes, position, position + fieldLength);
                            String fieldValue = new String(fieldData, "ISO8859-1");

                            // PRESERVAR ESPACIOS para campos fijos
                            msg.set(fieldNum, fieldValue);

                            logger.debug("✅ Campo {}: '{}' (posición: {}-{}, longitud: {})",
                                    fieldNum, fieldValue, position, position + fieldLength, fieldLength);

                            position += fieldLength;
                        } else {
                            logger.warn("⚠️ Campo {} excede longitud del mensaje", fieldNum);
                            break;
                        }
                    }
                }

                logger.info("📊 Procesamiento completado - Posición final: {}/{}", position, messageBytes.length);
            }

        } catch (Exception e) {
            logger.error("❌ Error en unpack manual: {}", e.getMessage());
        }

        return msg;
    }

    /**
     * Longitudes exactas de campos (basado en tu análisis)
     */
    private int getExactFieldLength(int fieldNum) {
        switch (fieldNum) {
            case 2: return 16;   // PAN
            case 3: return 6;    // Processing Code
            case 4: return 12;   // Amount
            case 6: return 12;   // Amount, Cardholder Billing
            case 7: return 10;   // Transmission DateTime
            case 11: return 6;   // STAN
            case 12: return 6;   // Local Time
            case 13: return 4;   // Local Date
            case 14: return 4;   // Date, Expiration
            case 15: return 4;   // Date, Settlement
            case 18: return 4;   // Merchant Type
            case 19: return 3;   // Acquiring Institution Country Code
            case 22: return 3;   // Point of Service Entry Mode
            case 25: return 2;   // Point of Service Condition Code
            case 32: return 6;   // Acquiring Institution Ident Code
            case 37: return 12;  // RRN
            case 41: return 8;   // Terminal ID
            case 42: return 15;  // Card Acceptor ID (CON ESPACIOS)
            case 43: return 40;  // Card Acceptor Name/Location (CON ESPACIOS)
            default: return 10;
        }
    }
    /**
     * Extraer campos manualmente preservando espacios
     */
    private Map<String, String> extractFieldsManual(ISOMsg msg, byte[] messageBytes) {
        Map<String, String> fields = new TreeMap<>();

        try {
            // Campos que sabemos están presentes basado en tu análisis
            int[] knownFields = {2, 3, 4, 6, 7, 11, 12, 13, 14, 15, 18, 19, 22, 25, 32, 37, 41, 42, 43};

            for (int fieldNum : knownFields) {
                if (msg.hasField(fieldNum)) {
                    try {
                        String value = msg.getString(fieldNum);

                        // Para campos fijos, mostrar con espacios preservados
                        if (fieldNum == 42 || fieldNum == 43 || fieldNum == 41) {
                            fields.put(String.valueOf(fieldNum), value);
                            logger.info("🔍 Campo {}: '{}' (longitud: {})", fieldNum, value, value.length());
                        } else {
                            fields.put(String.valueOf(fieldNum), value != null ? value.trim() : "[null]");
                        }

                    } catch (Exception e) {
                        fields.put(String.valueOf(fieldNum), "[Error: " + e.getMessage() + "]");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extrayendo campos: {}", e.getMessage());
        }

        return fields;
    }

    /**
     * Método para unpack manual cuando el automático falla - VERSIÓN CORREGIDA
     */
    private ISOMsg unpackManually(GenericPackager packager, byte[] messageBytes, BitSet bitmap) {
        ISOMsg msg = new ISOMsg();

        try {
            msg.setPackager(packager);

            // Establecer MTI manualmente
            if (messageBytes.length >= 4) {
                String mti = new String(Arrays.copyOfRange(messageBytes, 0, 4), "ISO8859-1");
                msg.setMTI(mti);
                logger.debug("MTI establecido: {}", mti);
            }

            // Si no tenemos bitmap, intentar extraerlo
            if (bitmap == null && messageBytes.length >= 20) {
                byte[] bitmapBytes = Arrays.copyOfRange(messageBytes, 4, 20);
                msg.set(1, bitmapBytes);
                bitmap = decodeBitmap(bitmapBytes);
            }

            if (bitmap != null) {
                int currentPosition = 20; // Después de MTI + Bitmap

                logger.debug("🛠️ Iniciando unpack manual - Posición: {}", currentPosition);

                // Procesar campos según bitmap
                for (int fieldNum = 2; fieldNum <= 128; fieldNum++) {
                    if (bitmap.get(fieldNum)) {
                        try {
                            // Obtener el packager del campo
                            Object fieldPackagerObj = packager.getFieldPackager(fieldNum);
                            if (fieldPackagerObj != null) {
                                // Usar reflexión para obtener la longitud ya que no tenemos ISOFieldPackager
                                int fieldLength = getFieldLength(fieldPackagerObj, fieldNum);

                                if (fieldLength <= 0) {
                                    logger.warn("⚠️ Longitud desconocida para campo {}, usando valor por defecto", fieldNum);
                                    fieldLength = getDefaultFieldLength(fieldNum);
                                }

                                // Verificar que tenemos suficientes bytes
                                if (currentPosition + fieldLength > messageBytes.length) {
                                    logger.warn("❌ Campo {} excede longitud del mensaje. Requerido: {}, Disponible: {}",
                                            fieldNum, fieldLength, messageBytes.length - currentPosition);
                                    break;
                                }

                                byte[] fieldData = Arrays.copyOfRange(messageBytes, currentPosition,
                                        currentPosition + fieldLength);
                                String fieldValue = new String(fieldData, "ISO8859-1");

                                msg.set(fieldNum, fieldValue);
                                currentPosition += fieldLength;

                                logger.debug("✅ Campo {} establecido: '{}' (longitud: {}, posición: {})",
                                        fieldNum, fieldValue.trim(), fieldLength, currentPosition);
                            } else {
                                logger.warn("⚠️ No se encontró packager para campo {}", fieldNum);
                            }
                        } catch (Exception e) {
                            logger.warn("⚠️ Error en campo {}: {}. Continuando...", fieldNum, e.getMessage());
                            // Intentar continuar con el siguiente campo
                        }
                    }
                }

                logger.info("🛠️ Unpack manual completado - Posición final: {}/{}",
                        currentPosition, messageBytes.length);
            } else {
                logger.error("❌ No se pudo obtener bitmap para unpack manual");
            }

        } catch (Exception e) {
            logger.error("❌ Error en unpack manual: {}", e.getMessage());
        }

        return msg;
    }

    /**
     * Decodificar bitmap manualmente
     */
    private BitSet decodeBitmap(byte[] bitmapBytes) {
        BitSet bitmap = new BitSet(128);
        try {
            for (int i = 0; i < bitmapBytes.length * 8; i++) {
                int byteIndex = i / 8;
                int bitIndex = 7 - (i % 8);

                if (byteIndex < bitmapBytes.length) {
                    byte b = bitmapBytes[byteIndex];
                    if (((b >> bitIndex) & 1) == 1) {
                        bitmap.set(i + 1); // Los bitmaps empiezan en bit 1
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error decodificando bitmap: {}", e.getMessage());
        }
        return bitmap;
    }

    /**
     * Obtener longitud del campo usando reflexión
     */
    private int getFieldLength(Object fieldPackager, int fieldNum) {
        try {
            // Intentar obtener el método getLength()
            java.lang.reflect.Method getLengthMethod = fieldPackager.getClass().getMethod("getLength");
            Object result = getLengthMethod.invoke(fieldPackager);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception e) {
            logger.debug("No se pudo obtener longitud para campo {}: {}", fieldNum, e.getMessage());
        }
        return getDefaultFieldLength(fieldNum);
    }

    /**
     * Longitudes por defecto para campos comunes
     */
    private int getDefaultFieldLength(int fieldNum) {
        switch (fieldNum) {
            case 2: return 19;  // PAN
            case 3: return 6;   // Processing Code
            case 4: return 12;  // Amount
            case 7: return 10;  // Transmission DateTime
            case 11: return 6;  // STAN
            case 12: return 6;  // Local Time
            case 13: return 4;  // Local Date
            case 37: return 12; // RRN
            case 41: return 8;  // Terminal ID
            case 42: return 15; // Card Acceptor ID
            case 43: return 40; // Card Acceptor Name/Location
            default: return 10; // Longitud por defecto
        }
    }

    /**
     * Parsear mensaje desde diferentes formatos - VERSIÓN MEJORADA
     */
    private byte[] parseMessageBytes(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw message cannot be empty");
        }

        String cleanedMessage = rawMessage.trim();

        logger.debug("Mensaje original: '{}'", cleanedMessage);
        logger.debug("Longitud mensaje: {} caracteres", cleanedMessage.length());

        // Si parece hexadecimal (solo caracteres 0-9, A-F, a-f)
        if (cleanedMessage.matches("^[0-9A-Fa-f]+$") && cleanedMessage.length() % 2 == 0) {
            logger.debug("Detectado formato HEX");
            return ISOUtil.hex2byte(cleanedMessage);
        }

        // Si tiene espacios, eliminar para análisis
        if (cleanedMessage.contains(" ")) {
            String noSpaces = cleanedMessage.replaceAll("\\s+", "");
            logger.debug("Mensaje sin espacios: '{}'", noSpaces);
            logger.debug("Longitud sin espacios: {} caracteres", noSpaces.length());

            // Verificar si sin espacios es hexadecimal
            if (noSpaces.matches("^[0-9A-Fa-f]+$") && noSpaces.length() % 2 == 0) {
                logger.debug("Detectado formato HEX después de quitar espacios");
                return ISOUtil.hex2byte(noSpaces);
            }

            cleanedMessage = noSpaces;
        }

        logger.debug("Usando formato ASCII/Text");
        // Si es texto ASCII/ISO8859-1
        try {
            return cleanedMessage.getBytes("ISO8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            return cleanedMessage.getBytes();
        }
    }


    /**
     * Validar que un packager tenga campos requeridos
     */
    @PostMapping("/validate")
    public ResponseEntity<PackagerService.ValidationResult> validatePackager(
            @RequestParam String packagerName,
            @RequestBody int[] requiredFields) {

        PackagerService.ValidationResult result =
                packagerService.validatePackager(packagerName, requiredFields);

        return ResponseEntity.ok(result);
    }

    /**
     * Obtener información de un campo específico
     */
    @GetMapping("/{packagerName}/field/{fieldNumber}")
    public ResponseEntity<Map<String, Object>> getFieldInfo(
            @PathVariable String packagerName,
            @PathVariable int fieldNumber) {

        Map<String, Object> response = new HashMap<>();

        try {
            String info = packagerService.getFieldInfo(packagerName, fieldNumber);
            response.put("fieldNumber", fieldNumber);
            response.put("packager", packagerName);
            response.put("info", info);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Test rápido de decodificación con mensaje de prueba
     */
    @GetMapping("/test/{packagerName}")
    public ResponseEntity<Map<String, Object>> quickTest(@PathVariable String packagerName) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Mensaje de prueba simple: 0800 network management
            String testMessage = "08002000000000000000" +
                    "1016123456" +  // Campo 7
                    "000001";        // Campo 11

            DecodeRequest request = new DecodeRequest();
            request.packager = packagerName;
            request.rawMessage = testMessage;

            DecodeResult result = decodeMessage(request).getBody();

            response.put("testMessage", testMessage);
            response.put("result", result);
            response.put("success", result.success);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }


    // ===== MÉTODOS UTILITARIOS =====

    /**
     * Parsear mensaje desde diferentes formatos
     */
//    private byte[] parseMessageBytes(String rawMessage) {
//        if (rawMessage == null || rawMessage.trim().isEmpty()) {
//            throw new IllegalArgumentException("Raw message cannot be empty");
//        }
//
//        rawMessage = rawMessage.trim().replaceAll("\\s+", "");
//
//        // Si es hexadecimal
//        if (rawMessage.matches("^[0-9A-Fa-f]+$") && rawMessage.length() % 2 == 0) {
//            return ISOUtil.hex2byte(rawMessage);
//        }
//
//        // Si es texto ASCII
//        return rawMessage.getBytes();
//    }

    /**
     * Extraer campos del mensaje ISO
     */
    /**
     * Extrae campos del mensaje ISO con post-procesamiento
     */
    private Map<String, String> extractFields(ISOMsg msg) {
        Map<String, String> fields = new TreeMap<>();

        try {
            for (int i = 0; i <= 128; i++) {
                if (msg.hasField(i)) {
                    try {
                        String value = msg.getString(i);

                        // Lista de campos alfanuméricos con padding de espacios
                        // Estos campos típicamente usan relleno de espacios a la derecha
                        boolean shouldTrim = (i == 41 || i == 42 || i == 43);

                        if (shouldTrim && value != null) {
                            value = value.trim();
                        }

                        fields.put(String.valueOf(i), value != null ? value : "[Binary Data]");

                        logger.debug("Campo {}: '{}' (length: {})",
                                i, value, value != null ? value.length() : 0);

                    } catch (Exception e) {
                        // Campo binario - mostrar en hex
                        try {
                            byte[] bytes = msg.getBytes(i);
                            fields.put(String.valueOf(i), "[HEX: " + ISOUtil.hexString(bytes) + "]");
                        } catch (Exception ex) {
                            fields.put(String.valueOf(i), "[Error reading field]");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extrayendo campos: {}", e.getMessage());
        }

        return fields;
    }
    /*
    private Map<String, String> extractFields(ISOMsg msg) {
        Map<String, String> fields = new TreeMap<>(); // TreeMap para orden

        try {
            for (int i = 0; i <= 128; i++) {
                if (msg.hasField(i)) {
                    try {
                        String value = msg.getString(i);
                        fields.put(String.valueOf(i), value != null ? value : "[Binary Data]");
                    } catch (Exception e) {
                        // Campo binario - mostrar en hex
                        try {
                            byte[] bytes = msg.getBytes(i);
                            fields.put(String.valueOf(i), "[HEX: " + ISOUtil.hexString(bytes) + "]");
                        } catch (Exception ex) {
                            fields.put(String.valueOf(i), "[Error reading field]");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extrayendo campos: {}", e.getMessage());
        }

        return fields;
    }
    */

    // ===== CLASES DE REQUEST/RESPONSE =====

    public static class DecodeRequest {
        public String rawMessage;
        public String packager;
        public String format = "hex"; // hex o ascii
    }

    public static class DecodeResult {
        public boolean success;
        public String mti;
        public Map<String, String> fields = new TreeMap<>();
        public int fieldCount;
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public String packagerUsed;
        public LocalDateTime parsedAt;
        public Map<String, Object> metadata = new HashMap<>();
    }
}