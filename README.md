ISO8583 Simulator
Un simulador completo de mensajes ISO8583 desarrollado en Java + Spring Boot con interfaz web React para pruebas y demos de sistemas de pagos.

🚀 Características
Simulador ISO8583 completo con soporte para múltiples tipos de mensaje
Interfaz web moderna desarrollada en React + TypeScript
API REST documentada con Swagger/OpenAPI
Conexión TCP configurable al autorizador
Generación automática de campos ISO8583
Pruebas BDD con Cucumber
Monitoreo en tiempo real con métricas y dashboards
Configuración flexible por perfiles de entorno
📋 Requisitos Previos
Java 17+
Maven 3.8+
Node.js 18+ (para el frontend)
Autorizador ISO8583 en 172.16.1.211:5105 (configurable)
🛠️ Instalación y Configuración
1. Clonar el proyecto
   bash
   git clone <repository-url>
   cd iso8583-simulator
2. Configurar el autorizador
   Editar src/main/resources/application.yml:

yaml
iso8583:
simulator:
switch:
host: 172.16.1.211  # Tu autorizador
port: 5105          # Puerto del autorizador
timeout: 30000      # Timeout en ms
3. Compilar y ejecutar
   Opción A: Ejecución completa (Backend + Frontend)
   bash
# Compilar todo (incluye build del frontend)
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
Opción B: Desarrollo separado
Backend:

bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
Frontend (en otra terminal):

bash
cd frontend
npm install
npm start
4. Acceder a la aplicación
   Aplicación Web: http://localhost:8080
   API REST: http://localhost:8080/api/v1
   Swagger UI: http://localhost:8080/swagger-ui.html
   API Docs: http://localhost:8080/api-docs
   Métricas: http://localhost:8080/actuator
   🎯 Uso Rápido
1. Dashboard
   Vista general del simulador
   Estado de conexión al autorizador
   Estadísticas en tiempo real
   Acciones rápidas
2. Editor de Mensajes
   Crear mensajes ISO8583
   Templates predefinidos (0200, 0400, 0800)
   Modo mock para testing sin autorizador
   Visualización de respuestas
3. Ejecutor de Pruebas
   Pruebas automatizadas
   Escenarios BDD con Cucumber
   Pruebas de carga
   Reportes detallados
4. Configuración
   Configurar conexión al switch
   Generación automática de campos
   Configuración de packagers
   Monitoreo y alertas
   📡 API Endpoints
   Simulador
   POST /api/v1/simulator/send - Enviar mensaje al autorizador
   POST /api/v1/simulator/mock - Generar respuesta mock
   GET /api/v1/simulator/connection/status - Estado de conexión
   POST /api/v1/simulator/connection/test - Probar conexión
   GET /api/v1/simulator/stats - Estadísticas del simulador
   Mensajes
   GET /api/v1/simulator/message-types - Tipos de mensaje disponibles
   GET /api/v1/simulator/message-template/{type} - Template de mensaje
   Ejemplo de uso con curl:
   bash
# Enviar mensaje 0200 (Solicitud Financiera)
curl -X POST http://localhost:8080/api/v1/simulator/send \
-H "Content-Type: application/json" \
-d '{
"messageType": "FINANCIAL_REQUEST_0200",
"fields": {
"2": "4000000000000002",
"3": "000000",
"4": "000000001000",
"11": "000001",
"41": "TERM0001",
"42": "MERCHANT001"
},
"mockResponse": false
}'
🏗️ Estructura del Proyecto
iso8583-simulator/
├── src/main/java/com/iso8583/simulator/
│   ├── core/                    # Lógica central ISO8583
│   │   ├── config/             # Configuraciones
│   │   ├── connection/         # Gestión de conexiones
│   │   ├── message/            # Construcción/parsing de mensajes
│   │   └── enums/              # Enumeraciones
│   ├── simulator/              # Motor del simulador
│   ├── web/                    # Controllers y DTOs REST
│   └── testing/                # Pruebas BDD con Cucumber
├── src/main/resources/
│   ├── config/                 # Configuraciones YAML
│   ├── packagers/              # Packagers ISO8583
│   └── test-data/              # Datos de prueba
├── frontend/                   # Frontend React
│   ├── src/components/         # Componentes React
│   ├── src/services/           # Servicios de API
│   └── src/types/              # Tipos TypeScript
└── target/                     # Build artifacts
🔧 Configuración Avanzada
Perfiles de Entorno
Desarrollo:

bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
Producción:

bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
Testing:

bash
mvn test -Dspring.profiles.active=test
Variables de Entorno
bash
export ISO8583_SWITCH_HOST=172.16.1.211
export ISO8583_SWITCH_PORT=5105
export ISO8583_SWITCH_TIMEOUT=30000
export SPRING_PROFILES_ACTIVE=prod
Docker (Opcional)
bash
# Construir imagen
docker build -t iso8583-simulator .

# Ejecutar contenedor
docker run -p 8080:8080 \
-e ISO8583_SWITCH_HOST=x.x.x.x \
-e ISO8583_SWITCH_PORT=xxxx \
iso8583-simulator
🧪 Testing
Ejecutar todas las pruebas
bash
mvn test
Ejecutar solo pruebas unitarias
bash
mvn test -Dtest="*Test"
Ejecutar pruebas BDD (Cucumber)
bash
mvn test -Dtest="*CucumberTest*"
Ejecutar pruebas de integración
bash
mvn test -Dtest="*IntegrationTest"
📊 Monitoreo
Métricas disponibles en /actuator/metrics:
iso8583.messages.sent - Total de mensajes enviados
iso8583.messages.success - Mensajes exitosos
iso8583.messages.failed - Mensajes fallidos
iso8583.response.time - Tiempo de respuesta promedio
iso8583.connection.status - Estado de conexión
Prometheus
Las métricas están disponibles en formato Prometheus en /actuator/prometheus

🛡️ Seguridad
Validación de entrada en todos los endpoints
CORS configurado para desarrollo
Headers de seguridad configurados
Logs detallados para auditoría
🤝 Contribuir
Fork del proyecto
Crear feature branch (git checkout -b feature/nueva-funcionalidad)
Commit de cambios (git commit -am 'Agregar nueva funcionalidad')
Push al branch (git push origin feature/nueva-funcionalidad)
Crear Pull Request
📝 Licencia
Este proyecto está bajo la Licencia MIT - ver el archivo LICENSE para detalles.

🆘 Soporte
Documentación: http://localhost:8080/swagger-ui.html
Issues: Crear un issue en el repositorio
Email: support@iso8583simulator.com
🗺️ Roadmap
Soporte para más tipos de mensaje ISO8583
Interfaz de administración avanzada
Simulación de múltiples acquirers
Reportes y analytics avanzados
Soporte para ISO8583 v2003
Integración con herramientas de CI/CD
