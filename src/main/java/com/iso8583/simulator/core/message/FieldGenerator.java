package com.iso8583.simulator.core.message;

import com.iso8583.simulator.core.config.FieldGenerationConfig;
import com.iso8583.simulator.core.enums.FieldGenerationType;
import org.springframework.stereotype.Component;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FieldGenerator {

    private final Map<String, AtomicInteger> sequentialCounters = new ConcurrentHashMap<>();
    private final Map<String, String> lastResetDate = new ConcurrentHashMap<>();

    public String generateField(int fieldNumber, FieldGenerationConfig config,
                                Map<String, String> csvData, Map<String, String> messageContext) {

        // Priority 1: CSV Override
        String csvOverride = csvData.get(getFieldOverrideKey(fieldNumber));
        if (csvOverride != null && !csvOverride.isEmpty()) {
            return csvOverride;
        }

        // Priority 2: Generate according to configuration
        switch (config.getType()) {
            case STATIC:
                return config.getValue();

            case DYNAMIC:
                return generateDynamicField(config);

            case SEQUENTIAL:
                return generateSequentialField(fieldNumber, config);

            case TEMPLATE:
                return generateTemplateField(config, csvData, messageContext);

            case REFERENCE:
                return messageContext.get("field_" + config.getFieldReference());

            default:
                return csvData.get(getFieldKey(fieldNumber));
        }
    }

    private String generateDynamicField(FieldGenerationConfig config) {
        SimpleDateFormat sdf = new SimpleDateFormat(config.getFormat());
        return sdf.format(new Date());
    }

    private String generateSequentialField(int fieldNumber, FieldGenerationConfig config) {
        String counterKey = "field_" + fieldNumber;

        // Check if daily reset is needed
        if (Boolean.TRUE.equals(config.getResetDaily())) {
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String lastReset = lastResetDate.get(counterKey);

            if (!today.equals(lastReset)) {
                sequentialCounters.put(counterKey, new AtomicInteger(config.getStartValue()));
                lastResetDate.put(counterKey, today);
            }
        }

        AtomicInteger counter = sequentialCounters.computeIfAbsent(
                counterKey, k -> new AtomicInteger(config.getStartValue())
        );

        DecimalFormat df = new DecimalFormat(config.getFormat());
        return df.format(counter.getAndAdd(config.getIncrement()));
    }

    private String generateTemplateField(FieldGenerationConfig config,
                                         Map<String, String> csvData,
                                         Map<String, String> messageContext) {

        String template = config.getTemplate();

        if (config.getComponents() != null) {
            for (Map.Entry<String, FieldGenerationConfig.ComponentConfig> entry : config.getComponents().entrySet()) {
                String componentName = entry.getKey();
                FieldGenerationConfig.ComponentConfig componentConfig = entry.getValue();

                String componentValue = generateComponentValue(componentConfig, csvData, messageContext);
                template = template.replace("{" + componentName + "}", componentValue);
            }
        }

        // Truncate if max length is specified
        if (config.getMaxLength() != null && template.length() > config.getMaxLength()) {
            template = template.substring(0, config.getMaxLength());
        }

        return template;
    }

    private String generateComponentValue(FieldGenerationConfig.ComponentConfig componentConfig,
                                          Map<String, String> csvData,
                                          Map<String, String> messageContext) {

        switch (componentConfig.getType()) {
            case DYNAMIC:
                SimpleDateFormat sdf = new SimpleDateFormat(componentConfig.getFormat());
                return sdf.format(new Date());

            case REFERENCE:
                return messageContext.get("field_" + componentConfig.getField());

            default:
                return "";
        }
    }

    private String getFieldOverrideKey(int fieldNumber) {
        return getFieldKey(fieldNumber) + "_OVERRIDE";
    }

    private String getFieldKey(int fieldNumber) {
        return "FIELD_" + fieldNumber;
    }

    // Reset all sequential counters (useful for testing)
    public void resetSequentialCounters() {
        sequentialCounters.clear();
        lastResetDate.clear();
    }

    // Reset specific field counter
    public void resetFieldCounter(int fieldNumber) {
        String counterKey = "field_" + fieldNumber;
        sequentialCounters.remove(counterKey);
        lastResetDate.remove(counterKey);
    }
}