@echo off
REM ============================================================================
REM SmartPingBeta - Build Script de Desarrollo (Windows)
REM ============================================================================
REM Mod: SmartPingBeta
REM Entorno: Fabric 1.20.1
REM Tarea: smartPingDebug
REM Salida: build/libs/SmartPingBeta-1.0.0.jar (remapeado y listo para usar)
REM ============================================================================

setlocal

echo.
echo ========================================
echo SmartPingBeta - Build de Desarrollo
echo ========================================
echo.

REM Cambiar al directorio del proyecto (donde está gradlew.bat)
cd /d "%~dp0\.."

REM Ejecutar build de desarrollo
call gradlew.bat clean smartPingDebug --stacktrace -Penv=debug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo ERROR: Build fallido
    echo ========================================
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================
echo Build de desarrollo completado exitosamente
echo ========================================
echo.

endlocal

