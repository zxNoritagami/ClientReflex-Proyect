# ============================================================================
# SmartPingBeta - Build Script de Desarrollo (PowerShell)
# ============================================================================
# Mod: SmartPingBeta
# Entorno: Fabric 1.20.1
# Tarea: smartPingDebug
# Salida: build/libs/SmartPingBeta-1.0.0.jar (remapeado y listo para usar)
# ============================================================================

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "========================================"
Write-Host "SmartPingBeta - Build de Desarrollo"
Write-Host "========================================"
Write-Host ""

# Cambiar al directorio del proyecto
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Join-Path $scriptPath ".."
Set-Location $projectRoot

# Ejecutar build de desarrollo
Write-Host "Ejecutando build..."
& .\gradlew.bat clean smartPingDebug --stacktrace -Penv=debug

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "========================================"
    Write-Host "ERROR: Build fallido"
    Write-Host "========================================"
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "========================================"
Write-Host "Build de desarrollo completado exitosamente"
Write-Host "========================================"
Write-Host ""
Write-Host "El JAR remapeado está en: build\libs\"
Write-Host "Este es el JAR que debes usar en Minecraft"
Write-Host ""

