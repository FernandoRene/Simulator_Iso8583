@echo off
echo ========================================
echo PRUEBA RAPIDA DE CONEXION REAL
echo ========================================

set BASE_URL=http://localhost:8081/api/v1

echo.
echo ¿Cual es la IP del autorizador? (Enter para usar 172.16.1.211)
set /p AUTHORIZER_IP=IP:
if "%AUTHORIZER_IP%"=="" set AUTHORIZER_IP=172.16.1.211

echo.
echo ¿Cual es el puerto del autorizador? (Enter para usar 5105)
set /p AUTHORIZER_PORT=Puerto:
if "%AUTHORIZER_PORT%"=="" set AUTHORIZER_PORT=5105

echo.
echo Configuracion: %AUTHORIZER_IP%:%AUTHORIZER_PORT%
echo.

echo 1. Verificando simulador...
curl -s %BASE_URL%/simulator/health | findstr "status"
echo.

echo 2. Cambiando a modo REAL...
curl -X POST %BASE_URL%/simulator/mode/real
echo.

echo 3. Verificando configuracion actual...
curl -s %BASE_URL%/simulator/config | findstr "host\|port"
echo.

echo 4. Probando conectividad de red...
echo Testing network connectivity to %AUTHORIZER_IP%:%AUTHORIZER_PORT%...
telnet %AUTHORIZER_IP% %AUTHORIZER_PORT%
echo.

echo 5. Conectando al autorizador...
curl -X POST %BASE_URL%/real-connection/connect
echo.

echo 6. Verificando estado de conexion...
curl -s %BASE_URL%/real-connection/status
echo.

echo 7. Test de conexion (mensaje 0800)...
curl -X POST %BASE_URL%/real-connection/test
echo.

echo 8. Simulacion JMX (transaccion real)...
curl -X POST %BASE_URL%/real-connection/jmx-simulation
echo.

echo ========================================
echo PRUEBA COMPLETADA
echo ========================================

echo.
echo Si hay errores de conexion, verifica:
echo 1. IP y puerto correctos
echo 2. Firewall/red permite conexion
echo 3. Autorizador esta activo
echo 4. Formato de packager correcto
echo.

pause