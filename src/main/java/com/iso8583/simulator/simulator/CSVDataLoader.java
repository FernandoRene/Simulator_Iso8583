package com.iso8583.simulator.simulator;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Cargador de datos CSV para escenarios de prueba
 */
@Component
public class CSVDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(CSVDataLoader.class);

    /**
     * Carga datos de test desde un archivo CSV usando OpenCSV
     */
    public List<Map<String, String>> loadTestDataFromCSV(String filePath) throws IOException, CsvException {
        List<Map<String, String>> testData = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();

            if (records.isEmpty()) {
                return testData;
            }

            // Primera fila como headers
            String[] headers = records.get(0);

            // Procesar datos
            for (int i = 1; i < records.size(); i++) {
                String[] row = records.get(i);
                Map<String, String> rowData = new HashMap<>();

                for (int j = 0; j < headers.length && j < row.length; j++) {
                    rowData.put(headers[j].trim(), row[j].trim());
                }

                testData.add(rowData);
            }

            logger.info("Cargados {} registros desde {}", testData.size(), filePath);

        } catch (Exception e) {
            logger.error("Error cargando CSV: {}", e.getMessage());
            throw e;
        }

        return testData;
    }

    /**
     * Carga datos usando Apache Commons CSV (alternativo)
     */
    public List<Map<String, String>> loadTestDataFromCSVApache(String filePath) throws IOException {
        List<Map<String, String>> testData = new ArrayList<>();

        try (Reader reader = new FileReader(filePath);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : parser) {
                Map<String, String> rowData = new HashMap<>();

                for (String header : parser.getHeaderNames()) {
                    rowData.put(header, record.get(header));
                }

                testData.add(rowData);
            }

            logger.info("Cargados {} registros desde {} (Apache CSV)", testData.size(), filePath);

        } catch (Exception e) {
            logger.error("Error cargando CSV con Apache: {}", e.getMessage());
            throw e;
        }

        return testData;
    }

    /**
     * Carga datos desde un InputStream (para recursos internos)
     */
    public List<Map<String, String>> loadTestDataFromInputStream(InputStream inputStream) throws IOException, CsvException {
        List<Map<String, String>> testData = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            List<String[]> records = reader.readAll();

            if (records.isEmpty()) {
                return testData;
            }

            String[] headers = records.get(0);

            for (int i = 1; i < records.size(); i++) {
                String[] row = records.get(i);
                Map<String, String> rowData = new HashMap<>();

                for (int j = 0; j < headers.length && j < row.length; j++) {
                    rowData.put(headers[j].trim(), row[j].trim());
                }

                testData.add(rowData);
            }

        } catch (Exception e) {
            logger.error("Error cargando CSV desde InputStream: {}", e.getMessage());
            throw e;
        }

        return testData;
    }

    /**
     * Genera datos de prueba de ejemplo
     */
    public List<Map<String, String>> generateSampleData() {
        List<Map<String, String>> sampleData = new ArrayList<>();

        // Ejemplo 1: Transacción de compra
        Map<String, String> transaction1 = new HashMap<>();
        transaction1.put("messageType", "0200");
        transaction1.put("pan", "4000000000000002");
        transaction1.put("processingCode", "000000");
        transaction1.put("amount", "000000001000");
        transaction1.put("terminalId", "TERM0001");
        transaction1.put("merchantId", "MERCHANT001");
        transaction1.put("description", "Compra de prueba");
        sampleData.add(transaction1);

        // Ejemplo 2: Consulta de saldo
        Map<String, String> transaction2 = new HashMap<>();
        transaction2.put("messageType", "0200");
        transaction2.put("pan", "4000000000000002");
        transaction2.put("processingCode", "300000");
        transaction2.put("amount", "000000000000");
        transaction2.put("terminalId", "TERM0001");
        transaction2.put("merchantId", "MERCHANT001");
        transaction2.put("description", "Consulta de saldo");
        sampleData.add(transaction2);

        // Ejemplo 3: Reverso
        Map<String, String> transaction3 = new HashMap<>();
        transaction3.put("messageType", "0400");
        transaction3.put("pan", "4000000000000002");
        transaction3.put("processingCode", "000000");
        transaction3.put("amount", "000000001000");
        transaction3.put("terminalId", "TERM0001");
        transaction3.put("merchantId", "MERCHANT001");
        transaction3.put("originalData", "0200000001120000000001000001");
        transaction3.put("description", "Reverso de transacción");
        sampleData.add(transaction3);

        logger.info("Generados {} registros de ejemplo", sampleData.size());
        return sampleData;
    }

    /**
     * Valida el formato de datos CSV
     */
    public boolean validateCSVData(List<Map<String, String>> data) {
        if (data == null || data.isEmpty()) {
            logger.warn("Datos CSV vacíos o nulos");
            return false;
        }

        Set<String> requiredFields = Set.of("messageType", "pan", "processingCode");

        for (Map<String, String> record : data) {
            for (String field : requiredFields) {
                if (!record.containsKey(field) || record.get(field).isEmpty()) {
                    logger.warn("Campo requerido faltante: {} en registro: {}", field, record);
                    return false;
                }
            }
        }

        logger.info("Validación CSV exitosa para {} registros", data.size());
        return true;
    }
}