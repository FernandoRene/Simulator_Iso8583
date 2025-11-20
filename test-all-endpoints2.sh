#!/bin/bash

# ============================================================================
# PRUEBAS ESPECÍFICAS - JSON desde archivos temporales
# ============================================================================

BASE_URL="http://localhost:8081"
TEMP_DIR="/tmp/iso8583-tests"

# Crear directorio temporal
mkdir -p "$TEMP_DIR"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     PRUEBAS ESPECÍFICAS - 3 Endpoints Problemáticos           ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# TEST 1: ENCODE - MTI en campo 0
# ============================================================================

echo "═══════════════════════════════════════════════════════════════"
echo "  TEST 1a: ENCODE - MTI en campo 0"
echo "═══════════════════════════════════════════════════════════════"
echo ""

cat > "$TEMP_DIR/encode1.json" << 'EOF'
{
  "fields": {
    "0": "0200",
    "2": "4218283014136073",
    "3": "000000",
    "4": "000000009800"
  },
  "packager": "linkser"
}
EOF

curl -X POST "$BASE_URL/api/v1/parser/encode" \
    -H "Content-Type: application/json" \
    -d @"$TEMP_DIR/encode1.json" 2>/dev/null | jq '.'

echo ""
echo "-------------------------------------------------------------------"
echo "  TEST 1b: ENCODE - MTI como mti separado"
echo "-------------------------------------------------------------------"
echo ""

cat > "$TEMP_DIR/encode2.json" << 'EOF'
{
  "mti": "0200",
  "fields": {
    "2": "4218283014136073",
    "3": "000000",
    "4": "000000009800"
  },
  "packager": "linkser"
}
EOF

curl -X POST "$BASE_URL/api/v1/parser/encode" \
    -H "Content-Type: application/json" \
    -d @"$TEMP_DIR/encode2.json" 2>/dev/null | jq '.'

echo ""
echo "-------------------------------------------------------------------"
echo "  TEST 1c: ENCODE - MTI en ambos lados"
echo "-------------------------------------------------------------------"
echo ""

cat > "$TEMP_DIR/encode3.json" << 'EOF'
{
  "mti": "0200",
  "fields": {
    "0": "0200",
    "2": "4218283014136073",
    "3": "000000",
    "4": "000000009800"
  },
  "packager": "linkser"
}
EOF

curl -X POST "$BASE_URL/api/v1/parser/encode" \
    -H "Content-Type: application/json" \
    -d @"$TEMP_DIR/encode3.json" 2>/dev/null | jq '.'

# ============================================================================
# TEST 2: PRETTY PRINT
# ============================================================================

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  TEST 2a: PRETTY PRINT - MTI en campo 0"
echo "═══════════════════════════════════════════════════════════════"
echo ""

cat > "$TEMP_DIR/pretty1.json" << 'EOF'
{
  "fields": {
    "0": "0200",
    "2": "4218283014136073",
    "3": "000000"
  },
  "packager": "linkser"
}
EOF

curl -X POST "$BASE_URL/api/v1/parser/pretty-print" \
    -H "Content-Type: application/json" \
    -d @"$TEMP_DIR/pretty1.json" 2>/dev/null | jq '.'

echo ""
echo "-------------------------------------------------------------------"
echo "  TEST 2b: PRETTY PRINT - MTI separado"
echo "-------------------------------------------------------------------"
echo ""

cat > "$TEMP_DIR/pretty2.json" << 'EOF'
{
  "mti": "0200",
  "fields": {
    "2": "4218283014136073",
    "3": "000000"
  },
  "packager": "linkser"
}
EOF

curl -X POST "$BASE_URL/api/v1/parser/pretty-print" \
    -H "Content-Type: application/json" \
    -d @"$TEMP_DIR/pretty2.json" 2>/dev/null | jq '.'

# ============================================================================
# TEST 3: TEMPLATE - Categorías
# ============================================================================

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  TEST 3a: TEMPLATE - Categoría CUSTOM"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Eliminar si existe
curl -s -X DELETE "$BASE_URL/api/v1/templates/test-purchase-001" > /dev/null 2>&1

cat > "$TEMP_DIR/template1.json" << 'EOF'
{
  "id": "test-purchase-001",
  "name": "Purchase Test Template",
  "description": "Template de prueba",
  "transactionType": "PURCHASE",
  "category": "CUSTOM",
  "predefined": false,
  "fields": {
    "0": "0200",
    "2": "4218283014136073",
    "3": "000000",
    "4": "000000010000"
  }
}
EOF

curl -X POST "$BASE_URL/api/v1/templates" \
    -H "Content-Type: application/json" \
    -d @"$TEMP_DIR/template1.json" 2>/dev/null | jq '.'

echo ""
echo "-------------------------------------------------------------------"
echo "  TEST 3b: TEMPLATE - Categoría TESTING"
echo "-------------------------------------------------------------------"
echo ""

# Eliminar si existe
curl -s -X DELETE "$BASE_URL/api/v1/templates/test-purchase-002" > /dev/null 2>&1

cat > "$TEMP_DIR/template2.json" << 'EOF'
{
  "id": "test-purchase-002",
  "name": "Purchase Test Template 2",
  "description": "Template de prueba",
  "transactionType": "PURCHASE",
  "category": "TESTING",
  "predefined": false,
  "fields": {
    "0": "0200",
    "2": "4218283014136073",
    "3": "000000",
    "4": "000000010000"
  }
}
EOF

curl -X POST "$BASE_URL/api/v1/templates" \
    -H "Content-Type: application/json" \
    -d @"$TEMP_DIR/template2.json" 2>/dev/null | jq '.'

echo ""
echo "-------------------------------------------------------------------"
echo "  TEST 3c: TEMPLATE - Categoría PURCHASE"
echo "-------------------------------------------------------------------"
echo ""

# Eliminar si existe
curl -s -X DELETE "$BASE_URL/api/v1/templates/test-purchase-003" > /dev/null 2>&1

cat > "$TEMP_DIR/template3.json" << 'EOF'
{
  "id": "test-purchase-003",
  "name": "Purchase Test Template 3",
  "description": "Template de prueba",
  "transactionType": "PURCHASE",
  "category": "PURCHASE",
  "predefined": false,
  "fields": {
    "0": "0200",
    "2": "4218283014136073",
    "3": "000000",
    "4": "000000010000"
  }
}
EOF

curl -X POST "$BASE_URL/api/v1/templates" \
    -H "Content-Type: application/json" \
    -d @"$TEMP_DIR/template3.json" 2>/dev/null | jq '.'

# ============================================================================
# CLEANUP Y RESUMEN
# ============================================================================

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  RESUMEN"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Archivos JSON creados en: $TEMP_DIR"
echo ""
echo "Busca en los resultados anteriores cuáles tienen 'success': true"
echo ""

# Limpiar templates de prueba
echo "Limpiando templates de prueba..."
curl -s -X DELETE "$BASE_URL/api/v1/templates/test-purchase-001" > /dev/null 2>&1
curl -s -X DELETE "$BASE_URL/api/v1/templates/test-purchase-002" > /dev/null 2>&1
curl -s -X DELETE "$BASE_URL/api/v1/templates/test-purchase-003" > /dev/null 2>&1

echo "¡Listo!"
echo ""