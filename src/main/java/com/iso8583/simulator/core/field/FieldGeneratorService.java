package com.iso8583.simulator.core.field;

import com.iso8583.simulator.core.field.model.FieldGenerationRequest;
import com.iso8583.simulator.core.field.model.FieldGenerationRequest.GenerationMode;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio para generación automática de campos ISO8583
 * Útil para testing y creación rápida de mensajes
 */
@Service
public class FieldGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(FieldGeneratorService.class);

    // Generadores secuenciales
    private final AtomicInteger stanSequence = new AtomicInteger(1);
    private final Random random = new Random();

    /**
     * Genera campos automáticamente en un mensaje
     */
    public ISOMsg generateFields(ISOMsg message, FieldGenerationRequest request) throws ISOException {
        if (request.getFieldsToGenerate() == null || request.getFieldsToGenerate().isEmpty()) {
            throw new IllegalArgumentException("Debe especificar al menos un campo para generar");
        }

        GenerationMode mode = request.getMode() != null ? request.getMode() : GenerationMode.STANDARD;

        for (Integer fieldNumber : request.getFieldsToGenerate()) {
            String generatedValue = generateFieldValue(fieldNumber, mode, request.getBaseValue());
            if (generatedValue != null) {
                message.set(fieldNumber, generatedValue);
                logger.debug("Campo {} generado: {}", fieldNumber, generatedValue);
            }
        }

        return message;
    }

    /**
     * Genera un valor para un campo específico
     */
    private String generateFieldValue(int fieldNumber, GenerationMode mode, String baseValue) {
        switch (fieldNumber) {
            case 7:  return generateTransmissionDateTime();
            case 11: return generateStan(mode);
            case 12: return generateLocalTime();
            case 13: return generateLocalDate();
            case 15: return generateSettlementDate();
            case 37: return generateRrn(mode);
            case 2:  return generatePan(mode);
            case 4:  return generateAmount(mode, baseValue);
            case 41: return generateTerminalId(mode);
            case 42: return generateMerchantId(mode);
            default: return generateGenericField(fieldNumber, mode, baseValue);
        }
    }

    /**
     * Genera Transmission Date & Time (Campo 7)
     * Formato: MMDDhhmmss
     */
    public String generateTransmissionDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    }

    /**
     * Genera STAN (Campo 11)
     * Formato: 6 dígitos (000001-999999)
     */
    public String generateStan(GenerationMode mode) {
        if (mode == GenerationMode.RANDOM) {
            return String.format("%06d", random.nextInt(999999) + 1);
        } else {
            // SEQUENTIAL o STANDARD
            int stan = stanSequence.getAndIncrement();
            if (stan > 999999) {
                stanSequence.set(1);
                stan = 1;
            }
            return String.format("%06d", stan);
        }
    }

    /**
     * Genera Local Time (Campo 12)
     * Formato: hhmmss
     */
    public String generateLocalTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    /**
     * Genera Local Date (Campo 13)
     * Formato: MMDD
     */
    public String generateLocalDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
    }

    /**
     * Genera Settlement Date (Campo 15)
     * Formato: MMDD
     */
    public String generateSettlementDate() {
        return generateLocalDate(); // Típicamente igual a local date
    }

    /**
     * Genera RRN (Campo 37)
     * Formato: 12 caracteres (Julian Date + STAN típicamente)
     */
    public String generateRrn(GenerationMode mode) {
        String julian = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyDDD"));
        String stan = generateStan(mode);
        String rrn = julian + stan;

        // Asegurar exactamente 12 caracteres
        if (rrn.length() > 12) {
            rrn = rrn.substring(0, 12);
        } else if (rrn.length() < 12) {
            rrn = rrn + "0".repeat(12 - rrn.length());
        }

        return rrn;
    }

    /**
     * Genera PAN (Campo 2)
     * Genera un número de tarjeta válido con Luhn
     */
    public String generatePan(GenerationMode mode) {
        if (mode == GenerationMode.RANDOM) {
            // Generar PAN aleatorio con Luhn válido
            String bin = "421828"; // BIN de ejemplo
            String accountNumber = String.format("%09d", random.nextInt(999999999));
            String panWithoutCheck = bin + accountNumber;
            int checkDigit = calculateLuhnCheckDigit(panWithoutCheck);
            return panWithoutCheck + checkDigit;
        } else {
            // PAN de ejemplo estándar
            return "4218283014136073";
        }
    }

    /**
     * Genera Amount (Campo 4)
     * Formato: 12 dígitos (centavos)
     */
    public String generateAmount(GenerationMode mode, String baseValue) {
        long amount;

        if (mode == GenerationMode.CUSTOM && baseValue != null) {
            try {
                amount = Long.parseLong(baseValue);
            } catch (NumberFormatException e) {
                amount = 10000; // Default 100.00
            }
        } else if (mode == GenerationMode.RANDOM) {
            amount = random.nextInt(100000) + 100; // Entre 1.00 y 1000.00
        } else {
            amount = 10000; // Default 100.00
        }

        return String.format("%012d", amount);
    }

    /**
     * Genera Terminal ID (Campo 41)
     * Formato: 8 caracteres
     */
    public String generateTerminalId(GenerationMode mode) {
        if (mode == GenerationMode.RANDOM) {
            return String.format("%08d", random.nextInt(99999999));
        } else {
            return "00000001"; // Terminal ID de ejemplo
        }
    }

    /**
     * Genera Merchant ID (Campo 42)
     * Formato: 15 caracteres
     */
    public String generateMerchantId(GenerationMode mode) {
        if (mode == GenerationMode.RANDOM) {
            return String.format("%-15s", String.format("%09d", random.nextInt(999999999)));
        } else {
            return "000000000000001"; // Merchant ID de ejemplo
        }
    }

    /**
     * Genera valor genérico para campos no especializados
     */
    private String generateGenericField(int fieldNumber, GenerationMode mode, String baseValue) {
        if (mode == GenerationMode.CUSTOM && baseValue != null) {
            return baseValue;
        }

        // Generar valor de ejemplo según el número de campo
        return "FIELD_" + fieldNumber + "_VALUE";
    }

    /**
     * Calcula dígito de control Luhn
     */
    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }

    /**
     * Genera múltiples campos comunes automáticamente
     */
    public Map<Integer, String> generateCommonFields() {
        Map<Integer, String> fields = new HashMap<>();

        fields.put(7, generateTransmissionDateTime());
        fields.put(11, generateStan(GenerationMode.SEQUENTIAL));
        fields.put(12, generateLocalTime());
        fields.put(13, generateLocalDate());
        fields.put(15, generateSettlementDate());
        fields.put(37, generateRrn(GenerationMode.SEQUENTIAL));

        return fields;
    }

    /**
     * Resetea el contador de STAN (útil para testing)
     */
    public void resetStanSequence() {
        stanSequence.set(1);
        logger.info("Secuencia STAN reseteada a 1");
    }

    /**
     * Obtiene el valor actual del STAN sin incrementar
     */
    public int getCurrentStan() {
        return stanSequence.get();
    }
}