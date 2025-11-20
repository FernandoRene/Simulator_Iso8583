package com.iso8583.simulator.core.parser.catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Catálogo de campos ISO8583
 * Proporciona información descriptiva para cada campo
 */
public class FieldCatalog {

    private static final Map<Integer, FieldDefinition> FIELD_DEFINITIONS = new HashMap<>();

    static {
        // MTI y campos primarios
        addField(0, "Message Type Indicator", "Indica el tipo y propósito del mensaje", "n", 4);
        addField(1, "Bitmap", "Indica qué campos están presentes", "b", 16);

        // Información de la tarjeta (2-23)
        addField(2, "Primary Account Number (PAN)", "Número de tarjeta", "n", 19);
        addField(3, "Processing Code", "Tipo de transacción", "n", 6);
        addField(4, "Amount, Transaction", "Monto de la transacción", "n", 12);
        addField(5, "Amount, Settlement", "Monto de liquidación", "n", 12);
        addField(6, "Amount, Cardholder Billing", "Monto facturado al tarjetahabiente", "n", 12);
        addField(7, "Transmission Date & Time", "Fecha y hora de transmisión", "n", 10);
        addField(8, "Amount, Cardholder Billing Fee", "Comisión al tarjetahabiente", "n", 8);
        addField(9, "Conversion Rate, Settlement", "Tasa de conversión de liquidación", "n", 8);
        addField(10, "Conversion Rate, Cardholder Billing", "Tasa de conversión facturación", "n", 8);
        addField(11, "System Trace Audit Number (STAN)", "Número secuencial único", "n", 6);
        addField(12, "Time, Local Transaction", "Hora local de la transacción", "n", 6);
        addField(13, "Date, Local Transaction", "Fecha local de la transacción", "n", 4);
        addField(14, "Date, Expiration", "Fecha de vencimiento de la tarjeta", "n", 4);
        addField(15, "Date, Settlement", "Fecha de liquidación", "n", 4);
        addField(16, "Date, Conversion", "Fecha de conversión", "n", 4);
        addField(17, "Date, Capture", "Fecha de captura", "n", 4);
        addField(18, "Merchant Type", "Tipo de comercio", "n", 4);
        addField(19, "Acquiring Institution Country Code", "Código de país del adquirente", "n", 3);
        addField(20, "PAN Extended Country Code", "Código de país extendido del PAN", "n", 3);
        addField(21, "Forwarding Institution Country Code", "Código de país de la institución", "n", 3);
        addField(22, "Point of Service Entry Mode", "Modo de entrada del punto de servicio", "n", 3);
        addField(23, "Card Sequence Number", "Número de secuencia de tarjeta", "n", 3);

        // Información de red (24-31)
        addField(24, "Network International Identifier", "Identificador internacional de red", "n", 3);
        addField(25, "Point of Service Condition Code", "Código de condición del punto de servicio", "n", 2);
        addField(26, "Point of Service Capture Code", "Código de captura del punto de servicio", "n", 2);
        addField(28, "Amount, Transaction Fee", "Comisión de la transacción", "n", 8);
        addField(30, "Amount, Transaction Processing Fee", "Comisión de procesamiento", "n", 8);
        addField(32, "Acquiring Institution ID", "Identificación de la institución adquirente", "n", 11);

        // Identificadores (33-43)
        addField(33, "Forwarding Institution ID", "ID de institución reenviadora", "n", 11);
        addField(35, "Track 2 Data", "Datos de banda magnética 2", "z", 37);
        addField(37, "Retrieval Reference Number", "Número de referencia de recuperación", "an", 12);
        addField(38, "Authorization ID Response", "Código de autorización", "an", 6);
        addField(39, "Response Code", "Código de respuesta", "an", 2);
        addField(40, "Service Restriction Code", "Código de restricción de servicio", "an", 3);
        addField(41, "Card Acceptor Terminal ID", "Identificación de terminal", "ans", 8);
        addField(42, "Card Acceptor ID Code", "Código de identificación del comercio", "ans", 15);
        addField(43, "Card Acceptor Name/Location", "Nombre y ubicación del comercio", "ans", 40);

        // Datos adicionales (44-59)
        addField(44, "Additional Response Data", "Datos adicionales de respuesta", "an", 25);
        addField(45, "Track 1 Data", "Datos de banda magnética 1", "an", 76);
        addField(48, "Additional Data", "Datos adicionales", "ans", 999);
        addField(49, "Currency Code, Transaction", "Código de moneda de transacción", "n", 3);
        addField(50, "Currency Code, Settlement", "Código de moneda de liquidación", "n", 3);
        addField(51, "Currency Code, Cardholder Billing", "Código de moneda de facturación", "n", 3);
        addField(52, "Personal ID Number (PIN)", "PIN encriptado", "b", 8);
        addField(53, "Security Related Control Information", "Información de control de seguridad", "n", 16);
        addField(54, "Additional Amounts", "Montos adicionales", "ans", 120);
        addField(55, "ICC Data", "Datos de chip EMV", "ans", 999);

        // Datos privados y de mensaje (60-99)
        addField(60, "Reserved Private", "Uso privado", "ans", 999);
        addField(61, "Reserved Private", "Uso privado", "ans", 999);
        addField(62, "Reserved Private", "Uso privado", "ans", 999);
        addField(63, "Reserved Private", "Uso privado", "ans", 999);
        addField(64, "Message Authentication Code (MAC)", "Código de autenticación del mensaje", "b", 8);
        addField(70, "Network Management Information Code", "Código de información de gestión de red", "n", 3);
        addField(90, "Original Data Elements", "Elementos de datos originales", "n", 42);
        addField(95, "Replacement Amounts", "Montos de reemplazo", "ans", 42);
        addField(97, "Amount, Net Settlement", "Monto neto de liquidación", "n", 16);
        addField(98, "Payee", "Beneficiario", "ans", 25);
        addField(100, "Receiving Institution ID", "ID de institución receptora", "n", 11);
        addField(101, "File Name", "Nombre de archivo", "ans", 17);
        addField(102, "Account ID 1", "Cuenta origen", "ans", 28);
        addField(103, "Account ID 2", "Cuenta destino", "ans", 28);
        addField(123, "Receipt Number", "Número de recibo", "ans", 15);
        addField(127, "Network Data", "Datos de red", "ans", 999);
        addField(128, "Message Authentication Code", "MAC", "b", 8);
    }

    private static void addField(int number, String name, String description, String type, int maxLength) {
        FIELD_DEFINITIONS.put(number, new FieldDefinition(number, name, description, type, maxLength));
    }

    public static FieldDefinition getDefinition(int fieldNumber) {
        return FIELD_DEFINITIONS.get(fieldNumber);
    }

    public static Map<Integer, FieldDefinition> getAllDefinitions() {
        return new HashMap<>(FIELD_DEFINITIONS);
    }

    public static boolean hasDefinition(int fieldNumber) {
        return FIELD_DEFINITIONS.containsKey(fieldNumber);
    }

    /**
     * Definición de un campo ISO8583
     */
    public static class FieldDefinition {
        private final int number;
        private final String name;
        private final String description;
        private final String type; // n=numeric, an=alphanumeric, ans=alphanumeric+special, b=binary, z=track2
        private final int maxLength;

        public FieldDefinition(int number, String name, String description, String type, int maxLength) {
            this.number = number;
            this.name = name;
            this.description = description;
            this.type = type;
            this.maxLength = maxLength;
        }

        // Getters
        public int getNumber() { return number; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public int getMaxLength() { return maxLength; }

        public String getTypeDescription() {
            switch (type) {
                case "n": return "Numérico";
                case "an": return "Alfanumérico";
                case "ans": return "Alfanumérico + Especiales";
                case "b": return "Binario";
                case "z": return "Track 2 (ISO 7813)";
                default: return "Desconocido";
            }
        }
    }
}