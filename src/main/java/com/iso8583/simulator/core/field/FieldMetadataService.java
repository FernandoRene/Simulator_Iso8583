package com.iso8583.simulator.core.field;

import com.iso8583.simulator.core.field.model.FieldMetadata;
import com.iso8583.simulator.core.field.model.FieldMetadata.FieldType;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio que proporciona metadata completa de campos ISO8583
 * Catálogo educativo con descripciones y validaciones
 */
@Service
public class FieldMetadataService {

    private final Map<Integer, FieldMetadata> fieldCatalog;

    public FieldMetadataService() {
        this.fieldCatalog = new HashMap<>();
        initializeFieldCatalog();
    }

    /**
     * Obtiene metadata de un campo específico
     */
    public FieldMetadata getFieldMetadata(int fieldNumber) {
        return fieldCatalog.get(fieldNumber);
    }

    /**
     * Obtiene metadata de todos los campos
     */
    public Map<Integer, FieldMetadata> getAllFieldsMetadata() {
        return new HashMap<>(fieldCatalog);
    }

    /**
     * Obtiene lista de campos requeridos para un tipo de transacción
     */
    public List<Integer> getRequiredFieldsForTransaction(String processingCode) {
        List<Integer> requiredFields = new ArrayList<>();

        // Campos básicos siempre requeridos
        requiredFields.addAll(Arrays.asList(0, 2, 3, 4, 7, 11, 12, 13));

        // Campos adicionales según processing code
        if (processingCode != null) {
            if (processingCode.startsWith("00")) { // Purchase
                requiredFields.addAll(Arrays.asList(14, 22, 25, 41, 42, 49));
            } else if (processingCode.startsWith("01")) { // Cash Advance
                requiredFields.addAll(Arrays.asList(14, 18, 22, 25, 41, 42, 49));
            } else if (processingCode.startsWith("30")) { // Balance Inquiry
                requiredFields.addAll(Arrays.asList(22, 41, 42, 49));
            }
        }

        return requiredFields;
    }

    /**
     * Inicializa el catálogo completo de campos ISO8583
     */
    private void initializeFieldCatalog() {
        // Campo 0 - MTI
        FieldMetadata mti = new FieldMetadata(
                0,
                "Message Type Indicator",
                "Indica el tipo y propósito del mensaje",
                FieldType.NUMERIC,
                4
        );
        mti.setRequired(true);
        mti.setExample("0200");
        mti.setEducationalNote("Primeros 4 dígitos: versión, clase, función y origen del mensaje");
        mti.setAllowedValues(Arrays.asList("0100", "0110", "0200", "0210", "0400", "0410", "0800", "0810"));
        fieldCatalog.put(0, mti);

        // Campo 2 - PAN
        FieldMetadata pan = new FieldMetadata(
                2,
                "Primary Account Number (PAN)",
                "Número de tarjeta del titular",
                FieldType.NUMERIC,
                19
        );
        pan.setRequired(true);
        pan.setExample("4218283014136073");
        pan.setEducationalNote("Debe pasar validación Luhn. Primeros 6 dígitos = BIN del emisor");
        pan.setValidationRules(Arrays.asList("LUHN_CHECK", "LENGTH_13_19", "NUMERIC_ONLY"));
        fieldCatalog.put(2, pan);

        // Campo 3 - Processing Code
        FieldMetadata processingCode = new FieldMetadata(
                3,
                "Processing Code",
                "Tipo de transacción y cuentas involucradas",
                FieldType.NUMERIC,
                6
        );
        processingCode.setRequired(true);
        processingCode.setExample("000000");
        processingCode.setEducationalNote("Formato: TTFFTT (Tipo Transacción + From Account + To Account)");
        fieldCatalog.put(3, processingCode);

        // Campo 4 - Amount
        FieldMetadata amount = new FieldMetadata(
                4,
                "Amount, Transaction",
                "Monto de la transacción en centavos",
                FieldType.NUMERIC,
                12
        );
        amount.setRequired(true);
        amount.setExample("000000009800");
        amount.setEducationalNote("Siempre en centavos. Ej: 98.00 BOB = 000000009800");
        amount.setValidationRules(Arrays.asList("NUMERIC_ONLY", "FIXED_LENGTH_12"));
        fieldCatalog.put(4, amount);

        // Campo 7 - Transmission Date & Time
        FieldMetadata dateTime = new FieldMetadata(
                7,
                "Transmission Date & Time",
                "Fecha y hora de transmisión del mensaje",
                FieldType.NUMERIC,
                10
        );
        dateTime.setRequired(true);
        dateTime.setExample("0611041342");
        dateTime.setEducationalNote("Formato: MMDDhhmmss (Mes, Día, hora, minuto, segundo)");
        fieldCatalog.put(7, dateTime);

        // Campo 11 - STAN
        FieldMetadata stan = new FieldMetadata(
                11,
                "System Trace Audit Number (STAN)",
                "Número secuencial único para auditoría",
                FieldType.NUMERIC,
                6
        );
        stan.setRequired(true);
        stan.setExample("963912");
        stan.setEducationalNote("Número secuencial que se reinicia cada día. Rango: 000001-999999");
        stan.setValidationRules(Arrays.asList("NUMERIC_ONLY", "FIXED_LENGTH_6", "RANGE_1_999999"));
        fieldCatalog.put(11, stan);

        // Campo 12 - Local Time
        FieldMetadata localTime = new FieldMetadata(
                12,
                "Time, Local Transaction",
                "Hora local de la transacción",
                FieldType.NUMERIC,
                6
        );
        localTime.setRequired(true);
        localTime.setExample("001343");
        localTime.setEducationalNote("Formato: hhmmss (hora del terminal)");
        fieldCatalog.put(12, localTime);

        // Campo 13 - Local Date
        FieldMetadata localDate = new FieldMetadata(
                13,
                "Date, Local Transaction",
                "Fecha local de la transacción",
                FieldType.NUMERIC,
                4
        );
        localDate.setRequired(true);
        localDate.setExample("0611");
        localDate.setEducationalNote("Formato: MMDD (mes y día del terminal)");
        fieldCatalog.put(13, localDate);

        // Campo 14 - Expiration Date
        FieldMetadata expiryDate = new FieldMetadata(
                14,
                "Date, Expiration",
                "Fecha de vencimiento de la tarjeta",
                FieldType.NUMERIC,
                4
        );
        expiryDate.setExample("2903");
        expiryDate.setEducationalNote("Formato: YYMM (año y mes de vencimiento)");
        fieldCatalog.put(14, expiryDate);

        // Campo 18 - Merchant Type
        FieldMetadata merchantType = new FieldMetadata(
                18,
                "Merchant Type",
                "Código de categoría del comercio (MCC)",
                FieldType.NUMERIC,
                4
        );
        merchantType.setExample("5814");
        merchantType.setEducationalNote("MCC define el tipo de negocio. 5814 = Restaurantes de comida rápida");
        fieldCatalog.put(18, merchantType);

        // Campo 22 - POS Entry Mode
        FieldMetadata posEntryMode = new FieldMetadata(
                22,
                "Point of Service Entry Mode",
                "Modo de ingreso de datos en el terminal",
                FieldType.NUMERIC,
                3
        );
        posEntryMode.setExample("051");
        posEntryMode.setEducationalNote("Indica cómo se leyó la tarjeta: 051=Chip+PIN, 010=Manual, 021=Contactless");
        posEntryMode.setAllowedValues(Arrays.asList("010", "021", "051", "071", "081"));
        fieldCatalog.put(22, posEntryMode);

        // Campo 25 - POS Condition Code
        FieldMetadata posCondition = new FieldMetadata(
                25,
                "Point of Service Condition Code",
                "Condición del terminal al momento de la transacción",
                FieldType.NUMERIC,
                2
        );
        posCondition.setExample("00");
        posCondition.setEducationalNote("00=Normal, 01=Cardholder not present, 08=E-commerce");
        posCondition.setAllowedValues(Arrays.asList("00", "01", "02", "08", "59"));
        fieldCatalog.put(25, posCondition);

        // Campo 35 - Track 2 Data
        FieldMetadata track2 = new FieldMetadata(
                35,
                "Track 2 Data",
                "Datos de la banda magnética de la tarjeta",
                FieldType.ALPHANUMERIC,
                37
        );
        track2.setExample("4218283014136073=29031011234567890");
        track2.setEducationalNote("Formato: PAN=YYMM[Service Code][Discretionary Data]");
        fieldCatalog.put(35, track2);

        // Campo 37 - RRN
        FieldMetadata rrn = new FieldMetadata(
                37,
                "Retrieval Reference Number",
                "Número de referencia único para la transacción",
                FieldType.ALPHANUMERIC,
                12
        );
        rrn.setRequired(true);
        rrn.setExample("516104963912");
        rrn.setEducationalNote("Formato típico: Julian Date (YYDDD) + STAN. Usado para búsquedas y reversas");
        fieldCatalog.put(37, rrn);

        // Campo 39 - Response Code
        FieldMetadata responseCode = new FieldMetadata(
                39,
                "Response Code",
                "Código de respuesta de la autorización",
                FieldType.ALPHANUMERIC,
                2
        );
        responseCode.setExample("00");
        responseCode.setEducationalNote("00=Aprobada, 51=Fondos insuficientes, 91=Switch/emisor no disponible");
        responseCode.setAllowedValues(Arrays.asList("00", "05", "51", "54", "55", "57", "91"));
        fieldCatalog.put(39, responseCode);

        // Campo 41 - Terminal ID
        FieldMetadata terminalId = new FieldMetadata(
                41,
                "Card Acceptor Terminal ID",
                "Identificador único del terminal",
                FieldType.ALPHANUMERIC,
                8
        );
        terminalId.setRequired(true);
        terminalId.setExample("00447416");
        terminalId.setEducationalNote("Identifica el terminal físico donde se realizó la transacción");
        fieldCatalog.put(41, terminalId);

        // Campo 42 - Card Acceptor ID
        FieldMetadata merchantId = new FieldMetadata(
                42,
                "Card Acceptor ID Code",
                "Código de identificación del comercio",
                FieldType.ALPHANUMERIC,
                15
        );
        merchantId.setRequired(true);
        merchantId.setExample("447416         ");
        merchantId.setEducationalNote("Identifica al comercio en la red del adquirente");
        fieldCatalog.put(42, merchantId);

        // Campo 43 - Card Acceptor Name/Location
        FieldMetadata merchantName = new FieldMetadata(
                43,
                "Card Acceptor Name/Location",
                "Nombre y ubicación del comercio",
                FieldType.ALPHANUMERIC,
                40
        );
        merchantName.setExample("PedidosYa                5566556655   BO");
        merchantName.setEducationalNote("Formato: Nombre (25) + Ciudad (13) + País (2)");
        fieldCatalog.put(43, merchantName);

        // Campo 49 - Currency Code
        FieldMetadata currencyCode = new FieldMetadata(
                49,
                "Currency Code, Transaction",
                "Código de moneda de la transacción (ISO 4217)",
                FieldType.NUMERIC,
                3
        );
        currencyCode.setRequired(true);
        currencyCode.setExample("068");
        currencyCode.setEducationalNote("Código numérico ISO 4217. Bolivia = 068 (BOB)");
        currencyCode.setAllowedValues(Arrays.asList("068", "840", "986"));
        fieldCatalog.put(49, currencyCode);

        // Campo 52 - PIN Data
        FieldMetadata pinData = new FieldMetadata(
                52,
                "Personal Identification Number Data",
                "PIN cifrado del tarjetahabiente",
                FieldType.BINARY,
                8
        );
        pinData.setExample("1234567890ABCDEF");
        pinData.setEducationalNote("PIN cifrado con clave de zona. Formato binario de 8 bytes");
        fieldCatalog.put(52, pinData);

        // Campo 62 - Private Use
        FieldMetadata privateUse = new FieldMetadata(
                62,
                "Reserved Private",
                "Datos de uso privado definidos por el adquirente",
                FieldType.ALPHANUMERIC,
                999
        );
        privateUse.setExample("!B0881000007!H000");
        privateUse.setEducationalNote("Campo de uso privado, varía según implementación del adquirente");
        fieldCatalog.put(62, privateUse);

        // Campo 90 - Original Data Elements
        FieldMetadata originalData = new FieldMetadata(
                90,
                "Original Data Elements",
                "Datos originales para reversas y ajustes",
                FieldType.NUMERIC,
                42
        );
        originalData.setExample("020000001234560611041342123456789012");
        originalData.setEducationalNote("Usado en reversas. Contiene MTI, STAN, DateTime y RRN originales");
        fieldCatalog.put(90, originalData);

        // Campo 102 - Account ID 1
        FieldMetadata account1 = new FieldMetadata(
                102,
                "Account Identification 1",
                "Identificación de cuenta origen",
                FieldType.ALPHANUMERIC,
                28
        );
        account1.setExample("1234567890");
        account1.setEducationalNote("Número de cuenta del tarjetahabiente");
        fieldCatalog.put(102, account1);

        // Campo 103 - Account ID 2
        FieldMetadata account2 = new FieldMetadata(
                103,
                "Account Identification 2",
                "Identificación de cuenta destino",
                FieldType.ALPHANUMERIC,
                28
        );
        account2.setExample("9876543210");
        account2.setEducationalNote("Usado en transferencias para identificar cuenta destino");
        fieldCatalog.put(103, account2);
    }
}