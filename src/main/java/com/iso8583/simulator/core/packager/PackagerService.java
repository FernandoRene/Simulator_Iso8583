package com.iso8583.simulator.core.packager;

import org.jpos.iso.ISOException;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para gestión de packagers ISO8583
 * Soporta múltiples packagers: Linkser, genérico, personalizado
 */
@Service
public class PackagerService {

    private static final Logger logger = LoggerFactory.getLogger(PackagerService.class);

    private final Map<String, GenericPackager> packagers = new HashMap<>();
    private String defaultPackagerName = "iso87ascii";

    /**
     * Inicializa los packagers disponibles
     */
    @PostConstruct
    public void initializePackagers() {
        logger.info("🔧 Inicializando packagers ISO8583...");

        // Registrar packager genérico ISO87
        try {
            GenericPackager genericPackager = loadPackager("packagers/iso87ascii.xml");
            packagers.put("iso87ascii", genericPackager);
            logger.info("✅ Packager genérico ISO87 cargado exitosamente");
        } catch (Exception e) {
            logger.warn("⚠️ Packager genérico no disponible: {}", e.getMessage());
        }

        if (packagers.isEmpty()) {
            logger.error("❌ NO SE PUDO CARGAR NINGÚN PACKAGER - El sistema no funcionará correctamente");
        } else {
            logger.info("✅ Packagers inicializados: {}", packagers.keySet());
            logger.info("📌 Packager por defecto: {}", defaultPackagerName);
        }
    }

    /**
     * Carga un packager desde un archivo XML
     */
    private GenericPackager loadPackager(String configPath) throws ISOException {
        try {
            // Intentar cargar desde classpath
            InputStream is = getClass().getClassLoader().getResourceAsStream(configPath);

            if (is != null) {
                GenericPackager packager = new GenericPackager(is);
                logger.debug("Packager cargado desde classpath: {}", configPath);
                return packager;
            } else {
                // Intentar cargar directamente
                GenericPackager packager = new GenericPackager(configPath);
                logger.debug("Packager cargado directamente: {}", configPath);
                return packager;
            }
        } catch (Exception e) {
            throw new ISOException("Error cargando packager desde " + configPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el packager por defecto
     */
    public GenericPackager getDefaultPackager() throws ISOException {
        return getPackager(defaultPackagerName);
    }

    /**
     * Obtiene un packager específico por nombre
     */
    public GenericPackager getPackager(String name) throws ISOException {
        if (name == null || name.trim().isEmpty()) {
            name = defaultPackagerName;
        }

        GenericPackager packager = packagers.get(name.toLowerCase());

        if (packager == null) {
            throw new ISOException("Packager no encontrado: " + name +
                    ". Disponibles: " + packagers.keySet());
        }

        return packager;
    }

    /**
     * Obtiene el packager Linkser específicamente
     */
    public GenericPackager getLinkserPackager() throws ISOException {
        return getPackager("linkser");
    }

    /**
     * Lista todos los packagers disponibles
     */
    public Map<String, String> getAvailablePackagers() {
        Map<String, String> available = new HashMap<>();

        packagers.forEach((name, packager) -> {
            String description = getPackagerDescription(name);
            available.put(name, description);
        });

        return available;
    }

    /**
     * Obtiene la descripción de un packager
     */
    private String getPackagerDescription(String name) {
        switch (name.toLowerCase()) {
            case "linkser":
                return "ISO 8583:1987 - Configuración Linkser completa con todos los campos";
            case "generic":
                return "ISO 8583:1987 - Packager genérico estándar";
            case "jar":
                return "ISO 8583:1987 - Packager desde JAR";
            default:
                return "Packager personalizado";
        }
    }

    /**
     * Cambia el packager por defecto
     */
    public void setDefaultPackager(String name) throws ISOException {
        if (!packagers.containsKey(name.toLowerCase())) {
            throw new ISOException("Packager no existe: " + name);
        }

        this.defaultPackagerName = name.toLowerCase();
        logger.info("📌 Packager por defecto cambiado a: {}", name);
    }

    /**
     * Verifica si un packager está disponible
     */
    public boolean isPackagerAvailable(String name) {
        return packagers.containsKey(name.toLowerCase());
    }

    /**
     * Obtiene información de un campo específico de un packager
     */
    public String getFieldInfo(String packagerName, int fieldNumber) throws ISOException {
        GenericPackager packager = getPackager(packagerName);

        try {
            if (packager.getFieldPackager(fieldNumber) != null) {
                return String.format("Campo %d: %s (Clase: %s)",
                        fieldNumber,
                        packager.getFieldPackager(fieldNumber).getDescription(),
                        packager.getFieldPackager(fieldNumber).getClass().getSimpleName());
            } else {
                return String.format("Campo %d: No definido en packager %s", fieldNumber, packagerName);
            }
        } catch (Exception e) {
            return String.format("Campo %d: Error obteniendo info - %s", fieldNumber, e.getMessage());
        }
    }

    /**
     * Validar que un packager tenga todos los campos necesarios
     */
    public ValidationResult validatePackager(String packagerName, int[] requiredFields) {
        ValidationResult result = new ValidationResult();
        result.packagerName = packagerName;

        try {
            GenericPackager packager = getPackager(packagerName);

            for (int fieldNumber : requiredFields) {
                if (packager.getFieldPackager(fieldNumber) == null) {
                    result.missingFields.add(fieldNumber);
                    result.valid = false;
                } else {
                    result.presentFields.add(fieldNumber);
                }
            }

            if (result.valid) {
                logger.info("✅ Packager {} validado - Todos los campos requeridos presentes", packagerName);
            } else {
                logger.warn("⚠️ Packager {} - Campos faltantes: {}", packagerName, result.missingFields);
            }

        } catch (Exception e) {
            result.valid = false;
            result.error = e.getMessage();
            logger.error("❌ Error validando packager {}: {}", packagerName, e.getMessage());
        }

        return result;
    }

    /**
     * Clase para resultado de validación de packager
     */
    public static class ValidationResult {
        public boolean valid = true;
        public String packagerName;
        public java.util.List<Integer> missingFields = new java.util.ArrayList<>();
        public java.util.List<Integer> presentFields = new java.util.ArrayList<>();
        public String error;

        @Override
        public String toString() {
            if (!valid) {
                return String.format("Packager %s - INVÁLIDO: %s (Campos faltantes: %s)",
                        packagerName, error != null ? error : "campos faltantes", missingFields);
            }
            return String.format("Packager %s - VÁLIDO (%d campos verificados)",
                    packagerName, presentFields.size());
        }
    }
}