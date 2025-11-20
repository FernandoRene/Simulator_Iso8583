package com.iso8583.simulator.api.controller;

import com.iso8583.simulator.core.packager.PackagerService;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Decodificar mensaje raw usando packager específico
     */
    @PostMapping("/decode")
    public ResponseEntity<DecodeResult> decodeMessage(
            @RequestBody DecodeRequest request) {

        long startTime = System.currentTimeMillis();
        DecodeResult result = new DecodeResult();
        result.packagerUsed = request.packager != null ? request.packager : "linkser";
        result.parsedAt = LocalDateTime.now();

        try {
            // Obtener packager
            GenericPackager packager = request.packager != null ?
                    packagerService.getPackager(request.packager) :
                    packagerService.getDefaultPackager();

            // Convertir hex string a bytes si es necesario
            byte[] messageBytes = parseMessageBytes(request.rawMessage);

            logger.info("📥 Decodificando mensaje con packager: {}", result.packagerUsed);
            logger.debug("Raw bytes (hex): {}", ISOUtil.hexString(messageBytes));

            // === DEBUG: ANALIZAR BYTES ANTES DEL UNPACK ===
            logger.debug("=== ANÁLISIS DETALLADO DEL MENSAJE ===");
            logger.debug("Longitud total mensaje: {} bytes", messageBytes.length);

            // Calcular posición aproximada del campo 42
            // MTI (4) + Bitmap (16) + campos fijos hasta el 41
            int estimatedField42Pos = 4 + 16 // MTI + Bitmap
                    + 19 // campo 2
                    + 6  // campo 3
                    + 12 // campo 4
                    + 12 // campo 6
                    + 10 // campo 7
                    + 6  // campo 11
                    + 6  // campo 12
                    + 4  // campo 13
                    + 4  // campo 14
                    + 4  // campo 15
                    + 4  // campo 18
                    + 3  // campo 19
                    + 3  // campo 22
                    + 2  // campo 25
                    + 6  // campo 32 (¿es fijo o variable?)
                    + 12 // campo 37
                    + 8; // campo 41

            logger.debug("Posición estimada campo 42: {}", estimatedField42Pos);

            if (messageBytes.length > estimatedField42Pos) {
                // Verificar área del campo 42 (15 bytes)
                byte[] field42Area = Arrays.copyOfRange(messageBytes, estimatedField42Pos,
                        Math.min(estimatedField42Pos + 15, messageBytes.length));
                logger.debug("Campo 42 bytes (hex): {}", ISOUtil.hexString(field42Area));
                logger.debug("Campo 42 como string: '{}'", new String(field42Area, "ISO8859-1"));
                logger.debug("Longitud campo 42: {} bytes", field42Area.length);

                // Verificar área del campo 43 (40 bytes)
                int field43Pos = estimatedField42Pos + 15;
                if (messageBytes.length > field43Pos) {
                    byte[] field43Area = Arrays.copyOfRange(messageBytes, field43Pos,
                            Math.min(field43Pos + 40, messageBytes.length));
                    logger.debug("Campo 43 bytes (hex): {}", ISOUtil.hexString(field43Area));
                    logger.debug("Campo 43 como string: '{}'", new String(field43Area, "ISO8859-1"));
                    logger.debug("Longitud campo 43: {} bytes", field43Area.length);
                }
            }
            logger.debug("=== FIN ANÁLISIS ===");
            // === FIN DEBUG ===

            // Debug específico para campos problemáticos
            int currentPos = 4 + 16; // MTI + Bitmap
            String[] fieldOrder = {"2", "3", "4", "6", "7", "11", "12", "13", "14", "15", "18", "19", "22", "25", "32", "37", "41", "42", "43"};
            int[] fieldLengths = {16, 6, 12, 12, 10, 6, 6, 4, 4, 4, 4, 3, 3, 2, 11, 12, 8, 15, 40};

            logger.debug("=== RECORRIENDO CAMPOS ===");
            for (int i = 0; i < fieldOrder.length; i++) {
                if (currentPos < messageBytes.length) {
                    int endPos = Math.min(currentPos + fieldLengths[i], messageBytes.length);
                    byte[] fieldData = Arrays.copyOfRange(messageBytes, currentPos, endPos);
                    logger.debug("Campo {}: pos={}, len={}, data='{}'",
                            fieldOrder[i], currentPos, fieldLengths[i], new String(fieldData, "ISO8859-1"));
                    currentPos += fieldLengths[i];
                }
            }
            logger.debug("Posición final: {}", currentPos);
            logger.debug("=== FIN RECORRIDO ===");

            ISOMsg msg = new ISOMsg();
            try {

                msg.setPackager(packager);
                // Debug: unpack campo por campo
                for (int i = 0; i <= 128; i++) {
                    try {
                        if (msg.hasField(i)) {
                            logger.debug("Campo {}: {} (offset: {})",
                                    i, msg.getString(i), msg.getFieldNumber());
                        }
                    } catch (Exception e) {
                        logger.debug("Error en campo {}: {}", i, e.getMessage());
                        break;
                    }
                }

                msg.unpack(messageBytes);
                if (msg.hasField(42)) {
                    byte[] field42Bytes = msg.getBytes(42);
                    String field42String = new String(field42Bytes, "ISO8859-1").trim(); // o sin trim()
                    logger.debug("Campo 42 raw: '{}'", field42String);
                    // Usar field42String en lugar de msg.getString(42)
                }
                // Después del unpack, verifica los bytes del campo 42
                if (msg.hasField(42)) {
                    byte[] field42Bytes = msg.getBytes(42);
                    logger.debug("Campo 42 bytes (hex): {}", ISOUtil.hexString(field42Bytes));
                    logger.debug("Campo 42 como string: '{}'", msg.getString(42));
                    logger.debug("Longitud campo 42: {}", msg.getString(42).length());
                }
            } catch (ISOException e) {
                logger.error("Error en campo específico: {}", e.getMessage());
            }
            // Decodificar mensaje
//            ISOMsg msg = new ISOMsg();
//            msg.setPackager(packager);
//
//            msg.unpack(messageBytes);

            // Extraer información del mensaje
            result.success = true;
            result.mti = msg.getMTI();
            result.fields = extractFields(msg);
            result.fieldCount = result.fields.size();

            // Metadata
            result.metadata.put("parsingTimeMs", System.currentTimeMillis() - startTime);
            result.metadata.put("messageLengthBytes", messageBytes.length);
            result.metadata.put("bitmapHex", msg.hasField(1) ? ISOUtil.hexString(msg.getBytes(1)) : "N/A");

            logger.info("✅ Mensaje decodificado exitosamente - MTI: {}, Campos: {}",
                    result.mti, result.fieldCount);

        } catch (ISOException e) {
            result.success = false;
            result.errors.add("Error parseando mensaje raw: " + e.getMessage());
            logger.error("❌ Error decodificando mensaje: {}", e.getMessage());
            logger.debug("Stack trace:", e);
        } catch (Exception e) {
            result.success = false;
            result.errors.add("Error inesperado: " + e.getMessage());
            logger.error("❌ Error inesperado: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(result);
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
    private byte[] parseMessageBytes(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw message cannot be empty");
        }

        rawMessage = rawMessage.trim().replaceAll("\\s+", "");

        // Si es hexadecimal
        if (rawMessage.matches("^[0-9A-Fa-f]+$") && rawMessage.length() % 2 == 0) {
            return ISOUtil.hex2byte(rawMessage);
        }

        // Si es texto ASCII
        return rawMessage.getBytes();
    }

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