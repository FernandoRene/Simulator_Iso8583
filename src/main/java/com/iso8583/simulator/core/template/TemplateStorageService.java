package com.iso8583.simulator.core.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iso8583.simulator.core.template.model.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
//import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para almacenamiento de templates en archivos JSON
 * Maneja templates predefinidos y templates de usuario
 */
@Service
public class TemplateStorageService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateStorageService.class);

    private static final String USER_TEMPLATES_DIR = "templates/user";
    private static final String PREDEFINED_TEMPLATES_DIR = "templates/predefined";

    private final ObjectMapper objectMapper;
    private final Path userTemplatesPath;
    private final Path predefinedTemplatesPath;

    public TemplateStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Configurar paths
        this.userTemplatesPath = Paths.get(USER_TEMPLATES_DIR);
        this.predefinedTemplatesPath = Paths.get(PREDEFINED_TEMPLATES_DIR);
    }

    @PostConstruct
    public void initialize() {
        try {
            // Crear directorios si no existen
            Files.createDirectories(userTemplatesPath);
            logger.info("Directorio de templates de usuario inicializado: {}",
                    userTemplatesPath.toAbsolutePath());

            // Templates predefinidos se cargan de resources
            logger.info("Templates predefinidos disponibles en: {}",
                    PREDEFINED_TEMPLATES_DIR);

        } catch (IOException e) {
            logger.error("Error inicializando directorios de templates: {}", e.getMessage());
        }
    }

    /**
     * Guarda un template de usuario
     */
    public void saveUserTemplate(TransactionTemplate template) throws IOException {
        if (template.isPredefined()) {
            throw new IllegalArgumentException("No se pueden modificar templates predefinidos");
        }

        String filename = sanitizeFilename(template.getId()) + ".json";
        Path filePath = userTemplatesPath.resolve(filename);

        objectMapper.writeValue(filePath.toFile(), template);
        logger.info("Template guardado: {} en {}", template.getName(), filePath);
    }

    /**
     * Carga un template de usuario por ID
     */
    public Optional<TransactionTemplate> loadUserTemplate(String templateId) {
        try {
            String filename = sanitizeFilename(templateId) + ".json";
            Path filePath = userTemplatesPath.resolve(filename);

            if (!Files.exists(filePath)) {
                return Optional.empty();
            }

            TransactionTemplate template = objectMapper.readValue(
                    filePath.toFile(),
                    TransactionTemplate.class
            );

            return Optional.of(template);

        } catch (IOException e) {
            logger.error("Error cargando template {}: {}", templateId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Carga un template predefinido por ID
     */
    public Optional<TransactionTemplate> loadPredefinedTemplate(String templateId) {
        try {
            // Los templates predefinidos están en resources
            String resourcePath = PREDEFINED_TEMPLATES_DIR + "/" +
                    sanitizeFilename(templateId) + ".json";

            var resource = getClass().getClassLoader().getResourceAsStream(resourcePath);

            if (resource == null) {
                logger.warn("Template predefinido no encontrado: {}", resourcePath);
                return Optional.empty();
            }

            TransactionTemplate template = objectMapper.readValue(
                    resource,
                    TransactionTemplate.class
            );

            template.setPredefined(true);
            return Optional.of(template);

        } catch (IOException e) {
            logger.error("Error cargando template predefinido {}: {}",
                    templateId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Carga todos los templates de usuario
     */
    public List<TransactionTemplate> loadAllUserTemplates() {
        List<TransactionTemplate> templates = new ArrayList<>();

        try {
            if (!Files.exists(userTemplatesPath)) {
                return templates;
            }

            Files.list(userTemplatesPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            TransactionTemplate template = objectMapper.readValue(
                                    path.toFile(),
                                    TransactionTemplate.class
                            );
                            templates.add(template);
                        } catch (IOException e) {
                            logger.warn("Error cargando template desde {}: {}",
                                    path, e.getMessage());
                        }
                    });

        } catch (IOException e) {
            logger.error("Error listando templates de usuario: {}", e.getMessage());
        }

        return templates;
    }

    /**
     * Carga todos los templates predefinidos
     */
    public List<TransactionTemplate> loadAllPredefinedTemplates() {
        List<TransactionTemplate> templates = new ArrayList<>();

        // Lista de templates predefinidos conocidos
        String[] predefinedIds = {
                "purchase",
                "cash_advance",
                "balance_inquiry",
                "transfer",
                "authorization"
        };

        for (String id : predefinedIds) {
            loadPredefinedTemplate(id).ifPresent(templates::add);
        }

        return templates;
    }

    /**
     * Elimina un template de usuario
     */
    public boolean deleteUserTemplate(String templateId) {
        try {
            String filename = sanitizeFilename(templateId) + ".json";
            Path filePath = userTemplatesPath.resolve(filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Template eliminado: {}", templateId);
                return true;
            }

            return false;

        } catch (IOException e) {
            logger.error("Error eliminando template {}: {}", templateId, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si existe un template de usuario
     */
    public boolean existsUserTemplate(String templateId) {
        String filename = sanitizeFilename(templateId) + ".json";
        Path filePath = userTemplatesPath.resolve(filename);
        return Files.exists(filePath);
    }

    /**
     * Exporta un template a JSON string
     */
    public String exportTemplate(TransactionTemplate template) throws IOException {
        return objectMapper.writeValueAsString(template);
    }

    /**
     * Importa un template desde JSON string
     */
    public TransactionTemplate importTemplate(String json) throws IOException {
        return objectMapper.readValue(json, TransactionTemplate.class);
    }

    /**
     * Sanitiza nombre de archivo
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}