#!/bin/bash

# ============================================================================
# SCRIPT DE PRUEBAS - ISO8583 SIMULATOR BACKEND
# Universidad Unión Bolivariana - Fernando
# ============================================================================

BASE_URL="http://localhost:8081"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Contador de pruebas
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# ============================================================================
# FUNCIONES AUXILIARES
# ============================================================================

print_header() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════════"
    echo "  $1"
    echo "═══════════════════════════════════════════════════════════════════"
    echo ""
}

print_test() {
    echo -e "${YELLOW}[TEST]${NC} $1"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

print_success() {
    echo -e "${GREEN}✅ PASS${NC} - $1"
    PASSED_TESTS=$((PASSED_TESTS + 1))
}

print_fail() {
    echo -e "${RED}❌ FAIL${NC} - $1"
    FAILED_TESTS=$((FAILED_TESTS + 1))
}

test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    print_test "$description"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        print_success "HTTP $http_code"
        echo "Response: $(echo "$body" | jq -C '.' 2>/dev/null || echo "$body" | head -c 200)"
        return 0
    else
        print_fail "HTTP $http_code"
        echo "Response: $body"
        return 1
    fi
}

# ============================================================================
# SECCIÓN 1: ISO MESSAGE PARSER
# ============================================================================

test_parser_endpoints() {
    print_header "1. ISO MESSAGE PARSER - Decode/Encode"
    
    # Test 1.1: Decode mensaje simple
    test_endpoint "POST" "/api/v1/parser/decode" \
        '{
            "message": "02003238000000C000040000000000000002164218283014136073000000000000009800101612000000010101161201001000000001000000000000001068",
            "packager": "iso87ascii"
        }' \
        "Decode mensaje ISO8583 con packager iso87ascii"
    
    # Test 1.2: Encode mensaje
    test_endpoint "POST" "/api/v1/parser/encode" \
        '{
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
            "packager": "iso87ascii"
        }' \
        "Encode campos a mensaje ISO8583"
    
    # Test 1.3: Pretty Print
    test_endpoint "POST" "/api/v1/parser/pretty-print" \
        '{
            "fields": {
                "0": "0200",
                "2": "4218283014136073",
                "3": "000000",
                "4": "000000009800",
                "11": "000001"
            },
            "packager": "iso87ascii"
        }' \
        "Pretty print de mensaje ISO8583"
    
    # Test 1.4: Listar packagers disponibles
    test_endpoint "GET" "/api/v1/parser/packagers" "" \
        "Listar packagers disponibles"
    
    # Test 1.5: Diagnostics de packagers
    test_endpoint "GET" "/api/v1/parser/packagers/diagnostics" "" \
        "Obtener diagnósticos de packagers"
}

# ============================================================================
# SECCIÓN 2: FIELD EDITOR
# ============================================================================

test_field_editor_endpoints() {
    print_header "2. FIELD EDITOR - Generación y Validación"
    
    # Test 2.1: Obtener metadata de todos los campos
    test_endpoint "GET" "/api/v1/fields/metadata" "" \
        "Obtener metadata de todos los campos ISO8583"
    
    # Test 2.2: Metadata de campo específico (Campo 2 - PAN)
    test_endpoint "GET" "/api/v1/fields/metadata/2" "" \
        "Obtener metadata del campo 2 (PAN)"
    
    # Test 2.3: Campos requeridos para Purchase
    test_endpoint "GET" "/api/v1/fields/required?processingCode=000000" "" \
        "Obtener campos requeridos para Purchase"
    
    # Test 2.4: Validar campo PAN
    test_endpoint "POST" "/api/v1/fields/validate" \
        '{
            "fieldNumber": 2,
            "value": "4218283014136073"
        }' \
        "Validar formato de PAN"
    
    # Test 2.5: Generar campos comunes (STAN, RRN, timestamps)
    test_endpoint "POST" "/api/v1/fields/generate/common" "" \
        "Auto-generar campos comunes (STAN, RRN, timestamps)"
    
    # Test 2.6: Generar campos específicos
    test_endpoint "POST" "/api/v1/fields/generate" \
        '{
            "fieldsToGenerate": [7, 11, 12, 13, 37],
            "generationMode": "SEQUENTIAL"
        }' \
        "Generar campos específicos con modo secuencial"
    
    # Test 2.7: Auto-completar campos faltantes
    test_endpoint "POST" "/api/v1/fields/autocomplete" "" \
        "Auto-completar campos faltantes en mensaje"
}

# ============================================================================
# SECCIÓN 3: TRANSACTION TEMPLATES
# ============================================================================

test_template_endpoints() {
    print_header "3. TRANSACTION TEMPLATES - CRUD"
    
    # Test 3.1: Listar todos los templates
    test_endpoint "GET" "/api/v1/templates" "" \
        "Listar todos los templates disponibles"
    
    # Test 3.2: Estadísticas de templates
    test_endpoint "GET" "/api/v1/templates/stats" "" \
        "Obtener estadísticas de uso de templates"
    
    # Test 3.3: Crear template de usuario
    test_endpoint "POST" "/api/v1/templates" \
        '{
            "id": "test-purchase-001",
            "name": "Purchase Test Template",
            "description": "Template de prueba para compras",
            "transactionType": "PURCHASE",
            "category": "USER",
            "predefined": false,
            "fields": {
                "0": "0200",
                "2": "4218283014136073",
                "3": "000000",
                "4": "000000010000",
                "41": "TERM0001",
                "42": "MERCHANT001",
                "49": "068"
            }
        }' \
        "Crear template de usuario para Purchase"
    
    # Test 3.4: Obtener template específico
    test_endpoint "GET" "/api/v1/templates/authorization" "" \
        "Obtener template por ID"
    
    # Test 3.5: Aplicar template
    test_endpoint "POST" "/api/v1/templates/authorization/apply" \
        '{
            "userInputs": {
                "2": "5100000000000003",
                "4": "000000025000"
            }
        }' \
        "Aplicar template con campos personalizados"
    
    # Test 3.6: Clonar template
    test_endpoint "POST" "/api/v1/templates/authorization/clone" \
        '{
            "newName": "Purchase Test Template - Clonado"
        }' \
        "Clonar template existente"
    
    # Test 3.7: Exportar template
    test_endpoint "GET" "/api/v1/templates/authorization/export" "" \
        "Exportar template a JSON"
    
    # Test 3.8: Eliminar template de prueba
    echo ""
    echo -e "${YELLOW}[CLEANUP]${NC} Eliminando template de prueba..."
    curl -s -X DELETE "$BASE_URL/api/v1/templates/authorization_delete" > /dev/null
}

# ============================================================================
# SECCIÓN 4: PACKAGER TEST
# ============================================================================

test_packager_test_endpoints() {
    print_header "4. PACKAGER TEST - Testing y Debugging"
    
    # Test 4.1: Listar packagers disponibles
    test_endpoint "GET" "/api/v1/packager/available" "" \
        "Listar packagers disponibles"
    
    # Test 4.2: Decode con packager específico
    test_endpoint "POST" "/api/v1/packager/decode" \
        '{
            "rawMessage": "08002000000000000000101612000000010000010301",
            "packager": "iso87ascii",
            "format": "ascii"
        }' \
        "Decode mensaje network management (0800)"
    
    # Test 4.3: Quick test de packager
    test_endpoint "GET" "/api/v1/packager/test/iso87ascii" "" \
        "Quick test del packager iso87ascii"
    
    # Test 4.4: Info de campo específico
    test_endpoint "GET" "/api/v1/packager/iso87ascii/field/2" "" \
        "Obtener info del campo 2 en packager iso87ascii"
}

# ============================================================================
# SECCIÓN 5: TRANSACTIONS (Existentes)
# ============================================================================

test_transaction_endpoints() {
    print_header "5. TRANSACTIONS - Balance Inquiry (Verificación rápida)"
    
    # Test 5.1: Balance Inquiry
    test_endpoint "POST" "/api/v1/transactions/balance-inquiry" \
        '{
            "pan": "4218283014136073",
            "track2": "4218283014136073=25121011234567890",
            "terminalId": "00000001",
            "cardAcceptorId": "000000000000001",
            "account": "00"
        }' \
        "Ejecutar Balance Inquiry"
    
    # Test 5.2: Status del servicio de transacciones
    test_endpoint "GET" "/api/v1/transactions/status" "" \
        "Obtener status del servicio de transacciones"
}

# ============================================================================
# SECCIÓN 6: CONNECTION (Existentes)
# ============================================================================

test_connection_endpoints() {
    print_header "6. CONNECTION - Estado de conexión"
    
    # Test 6.1: Status de conexión
    test_endpoint "GET" "/api/v1/connection/status" "" \
        "Obtener status de conexión"
    
    # Test 6.2: Connection ready
    test_endpoint "GET" "/api/v1/transactions/ready" "" \
        "Verificar si servicio está listo"
}

# ============================================================================
# EJECUCIÓN DE TODAS LAS PRUEBAS
# ============================================================================

main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║     TEST SUITE - ISO8583 SIMULATOR BACKEND                    ║"
    echo "║     Universidad Unión Bolivariana - Fernando                  ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Backend URL: $BASE_URL"
    echo ""
    
    # Verificar que el backend está corriendo
    if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${RED}❌ ERROR: Backend no está corriendo en $BASE_URL${NC}"
        echo ""
        echo "Inicia el backend con:"
        echo "  mvn spring-boot:run"
        echo ""
        exit 1
    fi
    
    echo -e "${GREEN}✅ Backend está corriendo${NC}"
    echo ""
    
    # Ejecutar todas las secciones de pruebas
    test_parser_endpoints
    test_field_editor_endpoints
    test_template_endpoints
    test_packager_test_endpoints
    test_transaction_endpoints
    test_connection_endpoints
    
    # Resumen final
    print_header "RESUMEN DE PRUEBAS"
    echo "Total de pruebas: $TOTAL_TESTS"
    echo -e "${GREEN}✅ Exitosas: $PASSED_TESTS${NC}"
    echo -e "${RED}❌ Fallidas: $FAILED_TESTS${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 ¡TODAS LAS PRUEBAS PASARON!${NC}"
        echo ""
        echo "El backend está listo para integrar con el frontend."
        exit 0
    else
        echo -e "${RED}⚠️  Algunas pruebas fallaron. Revisa los logs.${NC}"
        exit 1
    fi
}

# Ejecutar main
main