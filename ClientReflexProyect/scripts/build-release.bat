@echo off
REM ============================================================================
REM SmartPingBeta - Build Script de Release (Windows)
REM ============================================================================
REM Mod: SmartPingBeta
REM Entorno: Fabric 1.20.1
REM Tarea: smartPingRelease
REM Salida: build/releases/SmartPingBeta-1.0.0-release.jar
REM ============================================================================

setlocal

echo.
echo ========================================
echo SmartPingBeta - Build de Release
echo ========================================
echo.

REM Cambiar al directorio del proyecto
cd /d "%~dp0\.."

REM Ejecutar build de release
call gradlew.bat clean smartPingRelease --no-daemon --parallel --stacktrace -Penv=release

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo ERROR: Build fallido
    echo ========================================
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================
echo Build de release completado exitosamente
echo ========================================
echo.
echo JAR de produccion generado en:
echo   build\releases\SmartPingBeta-1.0.0-release.jar
echo.

endlocal

