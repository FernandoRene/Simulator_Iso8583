#!/bin/bash

# ============================================================================
# PRUEBAS RÁPIDAS INTERACTIVAS - ISO8583 SIMULATOR
# ============================================================================

BASE_URL="http://localhost:8081"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     PRUEBAS RÁPIDAS - ISO8583 SIMULATOR                       ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# MENÚ DE PRUEBAS
# ============================================================================

show_menu() {
    echo ""
    echo "Selecciona una prueba:"
    echo ""
    echo "  PARSER:"
    echo "    1) Decode mensaje ISO8583"
    echo "    2) Encode JSON a ISO8583"
    echo "    3) Pretty print mensaje"
    echo "    4) Listar packagers"
    echo ""
    echo "  FIELD EDITOR:"
    echo "    5) Generar campos comunes (STAN, RRN, timestamps)"
    echo "    6) Validar PAN"
    echo "    7) Ver metadata de campos"
    echo ""
    echo "  TEMPLATES:"
    echo "    8) Listar templates"
    echo "    9) Crear template de prueba"
    echo "   10) Aplicar template"
    echo "   11) Estadísticas de templates"
    echo ""
    echo "  TRANSACTIONS:"
    echo "   12) Balance Inquiry (MOCK)"
    echo "   13) Cash Advance (MOCK)"
    echo "   14) Purchase (MOCK)"
    echo ""
    echo "  SYSTEM:"
    echo "   15) Status completo del sistema"
    echo "   16) Health check"
    echo ""
    echo "    0) Salir"
    echo ""
    echo -n "Opción: "
}

# ============================================================================
# FUNCIONES DE PRUEBA
# ============================================================================

test_decode() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  DECODE MENSAJE ISO8583"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "Usando mensaje de prueba (Purchase 0200)..."
    echo ""
    
    curl -X POST "$BASE_URL/api/v1/parser/decode" \
        -H "Content-Type: application/json" \
        -d '{
            "message": "02003238000000C000040000000000000002164218283014136073000000000000009800101612000000010101161201001000000001000000000000001068",
            "packager": "linkser"
        }' | jq '.'
}

test_encode() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  ENCODE JSON A ISO8583"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -X POST "$BASE_URL/api/v1/parser/encode" \
        -H "Content-Type: application/json" \
        -d '{
            "fields": {
                "0": "0200",
                "2": "4218283014136073",
                "3": "000000",
                "4": "000000009800",
                "7": "1016120000",
                "11": "000001",
                "12": "120000",
                "13": "1016",
                "41": "00000001",
                "42": "000000000000001",
                "49": "068"
            },
            "packager": "linkser"
        }' | jq '.'
}

test_pretty_print() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  PRETTY PRINT MENSAJE"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    result=$(curl -s -X POST "$BASE_URL/api/v1/parser/pretty-print" \
        -H "Content-Type: application/json" \
        -d '{
            "fields": {
                "0": "0200",
                "2": "4218283014136073",
                "3": "000000",
                "4": "000000009800",
                "11": "000001"
            },
            "packager": "linkser"
        }')
    
    echo "$result" | jq -r '.prettyPrint'
}

test_list_packagers() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  PACKAGERS DISPONIBLES"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -s -X GET "$BASE_URL/api/v1/parser/packagers" | jq '.'
}

test_generate_common_fields() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  GENERAR CAMPOS COMUNES"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "Generando: STAN, RRN, Timestamps..."
    echo ""
    
    curl -s -X POST "$BASE_URL/api/v1/fields/generate/common" | jq '.'
}

test_validate_pan() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  VALIDAR PAN"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "Validando PAN: 4218283014136073"
    echo ""
    
    curl -s -X POST "$BASE_URL/api/v1/fields/validate" \
        -H "Content-Type: application/json" \
        -d '{
            "fieldNumber": 2,
            "value": "4218283014136073"
        }' | jq '.'
}

test_field_metadata() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  METADATA DE CAMPOS"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "Mostrando primeros 10 campos..."
    echo ""
    
    curl -s -X GET "$BASE_URL/api/v1/fields/metadata" | jq 'to_entries[:10]'
}

test_list_templates() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  TEMPLATES DISPONIBLES"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -s -X GET "$BASE_URL/api/v1/templates" | jq '.[] | {id, name, transactionType, category}'
}

test_create_template() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  CREAR TEMPLATE DE PRUEBA"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -X POST "$BASE_URL/api/v1/templates" \
        -H "Content-Type: application/json" \
        -d '{
            "id": "quick-test-template",
            "name": "Quick Test Template",
            "description": "Template de prueba rápida",
            "transactionType": "PURCHASE",
            "category": "USER",
            "predefined": false,
            "fields": {
                "0": "0200",
                "2": "4000000000000002",
                "3": "000000",
                "4": "000000005000",
                "41": "TESTTERM",
                "42": "TESTMERCHANT",
                "49": "068"
            }
        }' | jq '.'
}

test_apply_template() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  APLICAR TEMPLATE"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "Aplicando template quick-test-template..."
    echo ""
    
    curl -s -X POST "$BASE_URL/api/v1/templates/quick-test-template/apply" \
        -H "Content-Type: application/json" \
        -d '{
            "userInputs": {
                "2": "5100000000000003",
                "4": "000000015000"
            }
        }' | jq '.'
}

test_template_stats() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  ESTADÍSTICAS DE TEMPLATES"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -s -X GET "$BASE_URL/api/v1/templates/stats" | jq '.'
}

test_balance_inquiry() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  BALANCE INQUIRY (MOCK)"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -X POST "$BASE_URL/api/v1/transactions/balance-inquiry" \
        -H "Content-Type: application/json" \
        -d '{
            "pan": "4218283014136073",
            "track2": "4218283014136073=25121011234567890",
            "terminalId": "00000001",
            "cardAcceptorId": "000000000000001",
            "account": "00"
        }' | jq '.'
}

test_cash_advance() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  CASH ADVANCE (MOCK)"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -X POST "$BASE_URL/api/v1/transactions/cash-advance" \
        -H "Content-Type: application/json" \
        -d '{
            "pan": "4218283014136073",
            "track2": "4218283014136073=25121011234567890",
            "amount": "10000",
            "terminalId": "00000001",
            "cardAcceptorId": "000000000000001"
        }' | jq '.'
}

test_purchase() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  PURCHASE (MOCK)"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -X POST "$BASE_URL/api/v1/transactions/purchase" \
        -H "Content-Type: application/json" \
        -d '{
            "pan": "4218283014136073",
            "track2": "4218283014136073=25121011234567890",
            "amount": "25000",
            "terminalId": "00000001",
            "cardAcceptorId": "000000000000001"
        }' | jq '.'
}

test_system_status() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  SYSTEM STATUS"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    echo "Connection Status:"
    curl -s "$BASE_URL/api/v1/connection/status" | jq '.'
    echo ""
    echo "Transaction Service Status:"
    curl -s "$BASE_URL/api/v1/transactions/status" | jq '.'
    echo ""
    echo "Simulator Status:"
    curl -s "$BASE_URL/api/v1/simulator/status" | jq '.'
}

test_health() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  HEALTH CHECK"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    curl -s "$BASE_URL/actuator/health" | jq '.'
}

# ============================================================================
# MAIN LOOP
# ============================================================================

while true; do
    show_menu
    read -r option
    
    case $option in
        1) test_decode ;;
        2) test_encode ;;
        3) test_pretty_print ;;
        4) test_list_packagers ;;
        5) test_generate_common_fields ;;
        6) test_validate_pan ;;
        7) test_field_metadata ;;
        8) test_list_templates ;;
        9) test_create_template ;;
        10) test_apply_template ;;
        11) test_template_stats ;;
        12) test_balance_inquiry ;;
        13) test_cash_advance ;;
        14) test_purchase ;;
        15) test_system_status ;;
        16) test_health ;;
        0)
            echo ""
            echo "¡Hasta luego!"
            echo ""
            exit 0
            ;;
        *)
            echo ""
            echo "❌ Opción inválida"
            ;;
    esac
    
    echo ""
    echo "Presiona ENTER para continuar..."
    read -r
done