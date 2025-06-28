@echo off
cls
echo =========================================================
echo 🚀 ISO8583 Simulator - Script de Inicio para Windows
echo =========================================================

REM Verificar Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Java no esta instalado o no esta en el PATH
    echo    Instala Java 17+ y ejecuta este script nuevamente
    echo    Descarga desde: https://adoptium.net/
    pause
    exit /b 1
)

REM Verificar Maven
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Maven no esta instalado o no esta en el PATH
    echo    Instala Maven 3.8+ y ejecuta este script nuevamente
    echo    Descarga desde: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo ✅ Java detectado
echo ✅ Maven detectado
echo.

echo 🎯 Selecciona una opcion:
echo.
echo [1] Compilar y ejecutar (Recomendado)
echo [2] Solo compilar
echo [3] Solo ejecutar (sin compilar)
echo [4] Limpiar proyecto
echo [5] Ver logs detallados
echo [6] Salir
echo.

set /p choice="Ingresa tu opcion [1-6]: "

if "%choice%"=="1" goto compile_and_run
if "%choice%"=="2" goto compile_only
if "%choice%"=="3" goto run_only
if "%choice%"=="4" goto clean_project
if "%choice%"=="5" goto detailed_logs
if "%choice%"=="6" goto exit_script

echo ❌ Opcion invalida
pause
goto start

:compile_and_run
echo.
echo 🔨 Compilando proyecto...
mvn clean compile
if %errorlevel% neq 0 (
    echo.
    echo ❌ Error en la compilacion
    echo 💡 Revisa los errores arriba y vuelve a intentar
    pause
    exit /b 1
)
echo ✅ Compilacion exitosa
echo.
echo 🚀 Ejecutando aplicacion...
echo.
echo =========================================================
echo 📱 La aplicacion estara disponible en:
echo    http://localhost:8080
echo    http://localhost:8080/swagger-ui.html
echo.
echo 💡 Presiona Ctrl+C para detener la aplicacion
echo =========================================================
echo.
mvn spring-boot:run
goto end

:compile_only
echo.
echo 🔨 Compilando proyecto...
mvn clean compile
if %errorlevel% neq 0 (
    echo ❌ Error en la compilacion
) else (
    echo ✅ Compilacion exitosa
)
pause
goto end

:run_only
echo.
echo 🚀 Ejecutando aplicacion...
echo.
echo =========================================================
echo 📱 La aplicacion estara disponible en:
echo    http://localhost:8080
echo    http://localhost:8080/swagger-ui.html
echo =========================================================
echo.
mvn spring-boot:run
goto end

:clean_project
echo.
echo 🧹 Limpiando proyecto...
mvn clean
if %errorlevel% neq 0 (
    echo ❌ Error limpiando proyecto
) else (
    echo ✅ Proyecto limpiado exitosamente
)
pause
goto end

:detailed_logs
echo.
echo 📋 Ejecutando con logs detallados...
mvn clean compile -X
pause
goto end

:exit_script
echo.
echo 👋 ¡Hasta luego!
goto end

:end
echo.
echo ⏹️ Script finalizado
pause