#!/bin/bash

# Script de inicio para ISO8583 Simulator
echo "🚀 Iniciando ISO8583 Simulator..."

# Verificar que Java está instalado
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java no está instalado o no está en el PATH"
    echo "   Instala Java 17+ y ejecuta este script nuevamente"
    exit 1
fi

# Verificar que Maven está instalado
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven no está instalado o no está en el PATH"
    echo "   Instala Maven 3.8+ y ejecuta este script nuevamente"
    exit 1
fi

# Mostrar versiones
echo "✅ Java: $(java -version 2>&1 | head -1)"
echo "✅ Maven: $(mvn -version | head -1)"

# Crear directorios necesarios
echo "📁 Creando estructura de directorios..."
mkdir -p frontend/src/components/{Dashboard,MessageEditor,TestRunner,Configuration,ui}
mkdir -p frontend/src/{services,types,utils}
mkdir -p frontend/public
mkdir -p src/main/resources/{config,packagers,test-data,static/web}

# Función para mostrar progress
show_progress() {
    echo "⏳ $1..."
}

# Compilar el proyecto
show_progress "Compilando proyecto backend"
if mvn clean compile -q; then
    echo "✅ Backend compilado exitosamente"
else
    echo "❌ Error compilando backend"
    exit 1
fi

# Verificar que el frontend existe
if [ ! -d "frontend" ]; then
    echo "📂 Creando directorio frontend..."
    mkdir -p frontend
fi

# Mostrar información de conexión
echo ""
echo "🔗 Configuración de conexión:"
echo "   Host: $(grep -o 'host: [^[:space:]]*' src/main/resources/application.yml | cut -d' ' -f2)"
echo "   Puerto: $(grep -o 'port: [^[:space:]]*' src/main/resources/application.yml | cut -d' ' -f2)"

# Ofrecer opciones de ejecución
echo ""
echo "🎯 Opciones de ejecución:"
echo "1. Ejecutar solo backend (desarrollo)"
echo "2. Compilar todo y ejecutar (producción)"
echo "3. Ejecutar solo frontend (desarrollo separado)"
echo "4. Salir"
echo ""

read -p "Selecciona una opción [1-4]: " option

case $option in
    1)
        echo "🚀 Ejecutando backend en modo desarrollo..."
        echo "📱 Accede a: http://localhost:8080"
        echo "📚 Swagger UI: http://localhost:8080/swagger-ui.html"
        echo "🔍 API Docs: http://localhost:8080/api-docs"
        echo ""
        mvn spring-boot:run -Dspring-boot.run.profiles=dev
        ;;
    2)
        echo "🔨 Compilando proyecto completo..."
        if mvn clean install -q; then
            echo "✅ Proyecto compilado exitosamente"
            echo ""
            echo "🚀 Ejecutando aplicación completa..."
            echo "📱 Accede a: http://localhost:8080"
            echo "📚 Swagger UI: http://localhost:8080/swagger-ui.html"
            echo ""
            mvn spring-boot:run
        else
            echo "❌ Error en la compilación"
            exit 1
        fi
        ;;
    3)
        echo "🎨 Para ejecutar el frontend por separado:"
        echo "   cd frontend"
        echo "   npm install"
        echo "   npm start"
        echo ""
        echo "📱 El frontend estará disponible en: http://localhost:3000"
        echo "🔌 Asegúrate de que el backend esté ejecutándose en: http://localhost:8080"
        ;;
    4)
        echo "👋 ¡Hasta luego!"
        exit 0
        ;;
    *)
        echo "❌ Opción inválida"
        exit 1
        ;;
esac