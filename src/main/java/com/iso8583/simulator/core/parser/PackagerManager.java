package com.iso8583.simulator.core.parser;

import org.jpos.iso.ISOException;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

//import javax.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Gestor de múltiples packagers ISO8583 con diagnóstico mejorado
 */
@Service
public class PackagerManager {

    private static final Logger logger = LoggerFactory.getLogger(PackagerManager.class);

    private final Map<String, GenericPackager> packagers = new HashMap<>();
    private String defaultPackagerName = "iso87ascii";

    @PostConstruct
    public void init() {
        logger.info("🏭 Inicializando PackagerManager...");

        // Cargar packagers disponibles
        loadPackager("iso87ascii", "packagers/iso87ascii.xml");

        if (packagers.isEmpty()) {
            logger.error("❌ CRÍTICO: No se cargó ningún packager!");
        } else {
            logger.info("✅ PackagerManager inicializado con {} packagers", packagers.size());
            packagers.keySet().forEach(name ->
                    logger.info("   ✓ Packager cargado: {}", name)
            );
        }
    }
    /*
    private void loadPackager(String name, String path) {
        try {
            GenericPackager packager = new GenericPackager(path);
            packagers.put(name, packager);
            logger.info(" Packager '{}' cargado desde {}", name, path);
        } catch (ISOException e) {
            logger.warn("⚠ No se pudo cargar packager '{}' desde {}: {}",
                    name, path, e.getMessage());

            // Intentar cargar desde JAR
            tryLoadFromJar(name, path);
        }
    }
    */
     //*/
    /**     NUEVO en Revisión
     * Carga un packager usando ClassPathResource (más confiable)
     */

    private void loadPackager(String name, String path) {
        logger.info("📦 Intentando cargar packager '{}' desde '{}'", name, path);

        try {
            // Intento 1: Usar ClassPathResource de Spring (más confiable)
            Resource resource = new ClassPathResource(path);

            if (resource.exists()) {
                logger.debug("   ✓ Recurso encontrado en classpath");
                try (InputStream is = resource.getInputStream()) {
                    GenericPackager packager = new GenericPackager(is);
                    packagers.put(name, packager);
                    logger.info("   ✅ Packager '{}' cargado exitosamente desde classpath", name);
                    return;
                }
            } else {
                logger.warn("   ⚠️ Recurso '{}' no encontrado en classpath", path);
            }
        } catch (Exception e) {
            logger.warn("   ⚠️ Error con ClassPathResource: {}", e.getMessage());
        }

        // Intento 2: Método directo de jPOS
        try {
            logger.debug("   Intentando carga directa con jPOS...");
            GenericPackager packager = new GenericPackager(path);
            packagers.put(name, packager);
            logger.info("   ✅ Packager '{}' cargado con método directo", name);
            return;
        } catch (ISOException e) {
            logger.warn("   ⚠️ Carga directa falló: {}", e.getMessage());
        }

        // Intento 3: Desde JAR
        try {
            logger.debug("   Intentando desde JAR...");
            String jarPath = "jar:" + path;
            GenericPackager packager = new GenericPackager(jarPath);
            packagers.put(name, packager);
            logger.info("   ✅ Packager '{}' cargado desde JAR", name);
            return;
        } catch (ISOException e) {
            logger.warn("   ⚠️ Carga desde JAR falló: {}", e.getMessage());
        }

        // Todos los intentos fallaron
        logger.error("   ❌ FALLO: No se pudo cargar packager '{}' por ningún método", name);
        logger.error("   💡 Verifica que el archivo existe en: src/main/resources/{}", path);
    }

    /**
     * Obtiene un packager por nombre
     */
    public GenericPackager getPackager(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getDefaultPackager();
        }

        GenericPackager packager = packagers.get(name.toLowerCase());
        if (packager == null) {
            logger.warn("⚠️ Packager '{}' no encontrado, usando por defecto '{}'",
                    name, defaultPackagerName);
            return getDefaultPackager();
        }

        logger.debug("✓ Usando packager: {}", name);
        return packager;
    }

    /**
     * Obtiene el packager por defecto
     */
    public GenericPackager getDefaultPackager() {
        GenericPackager packager = packagers.get(defaultPackagerName);
        if (packager == null && !packagers.isEmpty()) {
            // Usar el primer packager disponible
            String firstName = packagers.keySet().iterator().next();
            logger.warn("⚠️ Packager por defecto '{}' no encontrado, usando '{}'",
                    defaultPackagerName, firstName);
            packager = packagers.values().iterator().next();
        }
        return packager;
    }
    /**
     * Intenta cargar packager desde JAR
     */
    private void tryLoadFromJar(String name, String path) {
        try {
            String jarPath = "jar:" + path;
            GenericPackager packager = new GenericPackager(jarPath);
            packagers.put(name, packager);
            logger.info("✅ Packager '{}' cargado desde JAR: {}", name, jarPath);
        } catch (ISOException e) {
            logger.error("❌ No se pudo cargar packager '{}': {}", name, e.getMessage());
        }
    }
    /**
     * Registra un nuevo packager dinámicamente
     */
    public void registerPackager(String name, GenericPackager packager) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de packager no puede estar vacío");
        }
        if (packager == null) {
            throw new IllegalArgumentException("Packager no puede ser null");
        }

        packagers.put(name.toLowerCase(), packager);
        logger.info("✅ Packager '{}' registrado dinámicamente", name);
    }

    /**
     * Registra un packager desde InputStream
     */
    public void registerPackagerFromInputStream(String name, InputStream is) throws ISOException {
        try {
            GenericPackager packager = new GenericPackager(is);
            registerPackager(name, packager);
        } catch (Exception e) {
            throw new ISOException("Error cargando packager desde InputStream: " + e.getMessage(), e);
        }
    }
    /**
     * Registra un packager desde un archivo
     */
    public void registerPackagerFromFile(String name, String filePath) throws ISOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new ISOException("Archivo no encontrado: " + filePath);
        }

        try (InputStream is = new FileInputStream(file)) {
            GenericPackager packager = new GenericPackager(is);
            registerPackager(name, packager);
        } catch (Exception e) {
            throw new ISOException("Error cargando packager desde archivo: " + e.getMessage(), e);
        }
    }
    /**
     * Lista todos los packagers disponibles
     */
    public Set<String> getAvailablePackagers() {
        return packagers.keySet();
    }

    /**
     * Verifica si un packager existe
     */
    public boolean hasPackager(String name) {
        return packagers.containsKey(name.toLowerCase());
    }

    /**
     * Establece el packager por defecto
     */
    public void setDefaultPackager(String name) {
        if (!hasPackager(name)) {
            throw new IllegalArgumentException("Packager no encontrado: " + name);
        }
        this.defaultPackagerName = name.toLowerCase();
        logger.info("🔧 Packager por defecto cambiado a: {}", name);
    }

    /**
     * Obtiene el nombre del packager por defecto
     */
    public String getDefaultPackagerName() {
        return defaultPackagerName;
    }
    public PackagerInfo getPackagerInfo(String name) {
        GenericPackager packager = getPackager(name);
        if (packager == null) {
            return null;
        }

        PackagerInfo info = new PackagerInfo();
        info.setName(name);
        info.setClassName(packager.getClass().getName());
        info.setIsDefault(name.equalsIgnoreCase(defaultPackagerName));

        // Contar campos
        int fieldCount = 0;
        for (int i = 0; i <= 128; i++) {
            if (packager.getFieldPackager(i) != null) {
                fieldCount++;
            }
        }
        info.setFieldCount(fieldCount);

        return info;
    }
    /**  NUEVO en REVISIÖN
     * Obtiene información de un packager
     */
    /*public PackagerInfo getPackagerInfo(String name) {
        GenericPackager packager = packagers.get(name.toLowerCase());
        if (packager == null) {
            return null;
        }

        PackagerInfo info = new PackagerInfo();
        info.setName(name);
        info.setClassName(packager.getClass().getName());
        info.setIsDefault(name.equalsIgnoreCase(defaultPackagerName));

        // Contar campos
        int fieldCount = 0;
        for (int i = 0; i <= 128; i++) {
            if (packager.getFieldPackager(i) != null) {
                fieldCount++;
            }
        }
        info.setFieldCount(fieldCount);

        return info;
    }
    */
    /**
     * Diagnóstico completo del estado de packagers
     */
    public String getDiagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("PackagerManager Diagnostics\n");
        sb.append("═══════════════════════════\n");
        sb.append(String.format("Total packagers loaded: %d\n", packagers.size()));
        sb.append(String.format("Default packager: %s\n", defaultPackagerName));
        sb.append("\nPackagers:\n");

        packagers.forEach((name, packager) -> {
            sb.append(String.format("  - %s: %s\n", name, packager.getClass().getSimpleName()));
        });

        return sb.toString();
    }

    /**
     * Clase de información de packager
     */
    public static class PackagerInfo {
        private String name;
        private String className;
        private boolean isDefault;
        private int fieldCount;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public boolean isDefault() { return isDefault; }
        public void setIsDefault(boolean isDefault) { this.isDefault = isDefault; }

        public int getFieldCount() { return fieldCount; }
        public void setFieldCount(int fieldCount) { this.fieldCount = fieldCount; }
    }
}