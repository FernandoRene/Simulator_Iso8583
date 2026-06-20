package com.iso8583.simulator.api.controller;

import com.iso8583.simulator.core.parser.ISOMessageParser;
import com.iso8583.simulator.core.parser.PackagerManager;
import com.iso8583.simulator.core.parser.model.ParseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller REST para parsing de mensajes ISO8583 con soporte multi-packager
 */
@RestController
@RequestMapping("/api/v1/parser")
@Tag(name = "ISO Message Parser", description = "Parsing y validación de mensajes ISO8583")
@CrossOrigin(origins = "*")
public class ISOMessageParserController {

    private static final Logger logger = LoggerFactory.getLogger(ISOMessageParserController.class);

    @Autowired
    private ISOMessageParser parser;

    @Autowired
    private PackagerManager packagerManager;

    /**
     * Decodifica mensaje ISO (ASCII o HEX) a JSON
     */
    @PostMapping("/decode")
    @Operation(summary = "Decode ISO8583 message",
            description = "Convierte mensaje ISO8583 (ASCII o HEX) a JSON con packager seleccionable")
    public ResponseEntity<Map<String, Object>> decodeMessage(
            @RequestBody Map<String, String> request) {

        try {
            String rawMessage = request.get("message");
            String packagerName = request.getOrDefault("packager", "iso87ascii"); // Default Linkser

            if (rawMessage == null || rawMessage.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Mensaje es requerido"));
            }

            logger.info("📥 Decodificando mensaje ISO con packager '{}' - {} chars",
                    packagerName, rawMessage.length());

            ParseResult result = parser.parseRaw(rawMessage, packagerName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("mti", result.getMti());
            response.put("fields", convertFieldsToMap(result));
            response.put("fieldCount", result.getFieldCount());
            response.put("errors", result.getErrors());
            response.put("warnings", result.getWarnings());
            response.put("metadata", result.getMetadata());
            response.put("parsedAt", result.getParsedAt());
            response.put("packagerUsed", packagerName);

            if (result.isSuccess()) {
                logger.info("✅ Mensaje decodificado - MTI: {}, {} campos",
                        result.getMti(), result.getFieldCount());
            } else {
                logger.warn("⚠️ Decodificación con errores: {}", result.getErrors());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error decodificando mensaje: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error decodificando mensaje: " + e.getMessage()));
        }
    }

    /**
     * Codifica JSON a mensaje ISO
     */
    @PostMapping("/encode")
    @Operation(summary = "Encode JSON to ISO8583",
            description = "Convierte JSON a mensaje ISO8583 (ASCII) con packager seleccionable")
    public ResponseEntity<Map<String, Object>> encodeMessage(
            @RequestBody Map<String, Object> request) {

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> fields = (Map<String, String>) request.get("fields");
            String packagerName = (String) request.getOrDefault("packager", "linkser");
            String format = (String) request.getOrDefault("format", "ascii");

            if (fields == null || fields.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Campos del mensaje son requeridos"));
            }

            logger.info("📤 Codificando mensaje ISO con packager '{}' - {} campos",
                    packagerName, fields.size());

            ISOMsg msg = parser.buildFromMap(fields, packagerName);
            byte[] packed = msg.pack();
            String encodedMessage = "hex".equalsIgnoreCase(format)
                    ? ISOUtil.hexString(packed)
                    : new String(packed);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", encodedMessage);
            response.put("format", format);
            response.put("mti", msg.getMTI());
            response.put("length", packed.length);
            response.put("fieldCount", fields.size() - 1);
            response.put("packagerUsed", packagerName);

            logger.info("✅ Mensaje codificado - MTI: {}, {} bytes", msg.getMTI(), packed.length);

            return ResponseEntity.ok(response);

        } catch (ISOException e) {
            logger.error("❌ Error codificando mensaje: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error codificando mensaje: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Error inesperado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno: " + e.getMessage()));
        }
    }

    /**
     * Pretty print de mensaje
     */
    @PostMapping("/pretty-print")
    @Operation(summary = "Pretty print ISO8583 message")
    public ResponseEntity<Map<String, Object>> prettyPrint(
            @RequestBody Map<String, Object> request) {

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> fields = (Map<String, String>) request.get("fields");
            String packagerName = (String) request.getOrDefault("packager", "linkser");

            ISOMsg msg = parser.buildFromMap(fields, packagerName);
            String prettyPrint = parser.prettyPrint(msg, packagerName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prettyPrint", prettyPrint);
            response.put("mti", msg.getMTI());
            response.put("packagerUsed", packagerName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error generando pretty print: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error generando pretty print: " + e.getMessage()));
        }
    }

    /**
     * Lista packagers disponibles
     */
    @GetMapping("/packagers")
    @Operation(summary = "List available packagers")
    public ResponseEntity<Map<String, Object>> listPackagers() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("packagers", packagerManager.getAvailablePackagers());
            response.put("default", packagerManager.getDefaultPackagerName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error listando packagers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error listando packagers: " + e.getMessage()));
        }
    }

    /**
     * Cambia packager por defecto
     */
    @PutMapping("/packagers/default")
    @Operation(summary = "Set default packager")
    public ResponseEntity<Map<String, Object>> setDefaultPackager(
            @RequestBody Map<String, String> request) {
        try {
            String packagerName = request.get("packager");

            if (packagerName == null || packagerName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Nombre de packager es requerido"));
            }

            packagerManager.setDefaultPackager(packagerName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Packager por defecto actualizado");
            response.put("defaultPackager", packagerName);

            logger.info("✅ Packager por defecto cambiado a: {}", packagerName);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Error cambiando packager: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error cambiando packager: " + e.getMessage()));
        }
    }

    /**
     * Carga un packager desde XML text (útil para debugging)
     */
    @PostMapping("/packagers/load")
    @Operation(summary = "Load packager from XML text")
    public ResponseEntity<Map<String, Object>> loadPackagerFromText(
            @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String xmlContent = request.get("xmlContent");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Nombre de packager es requerido"));
            }

            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Contenido XML es requerido"));
            }

            // Crear InputStream desde el texto
            InputStream is = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));

            // Registrar packager
            packagerManager.registerPackagerFromInputStream(name, is);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Packager cargado exitosamente");
            response.put("name", name);

            logger.info("✅ Packager '{}' cargado desde texto", name);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error cargando packager: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error cargando packager: " + e.getMessage()));
        }
    }

    /**
     * Diagnóstico del PackagerManager
     */
    @GetMapping("/packagers/diagnostics")
    @Operation(summary = "Get packager diagnostics")
    public ResponseEntity<Map<String, Object>> getPackagerDiagnostics() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("diagnostics", packagerManager.getDiagnostics());
            response.put("packagers", packagerManager.getAvailablePackagers());
            response.put("default", packagerManager.getDefaultPackagerName());

            // Info detallada de cada packager
            Map<String, Object> packagerDetails = new HashMap<>();
            for (String name : packagerManager.getAvailablePackagers()) {
                PackagerManager.PackagerInfo info = packagerManager.getPackagerInfo(name);
                if (info != null) {
                    packagerDetails.put(name, info);
                }
            }
            response.put("details", packagerDetails);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error en diagnóstico: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error en diagnóstico: " + e.getMessage()));
        }
    }

    // ========================================================================
    // MÉTODOS PRIVADOS - HELPERS
    // ========================================================================

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }

    private Map<String, Object> convertFieldsToMap(ParseResult result) {
        Map<String, Object> fieldsMap = new HashMap<>();

        result.getFields().forEach((fieldNumber, isoField) -> {
            Map<String, Object> fieldData = new HashMap<>();
            fieldData.put("value", isoField.getValue());
            fieldData.put("name", isoField.getName());
            fieldData.put("description", isoField.getDescription());

            fieldsMap.put(String.valueOf(fieldNumber), fieldData);
        });

        return fieldsMap;
    }
}