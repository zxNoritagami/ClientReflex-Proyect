#!/usr/bin/env bash
# ============================================================================
# SmartPingBeta - Build Script de Desarrollo (Linux/macOS)
# ============================================================================
# Mod: SmartPingBeta
# Entorno: Fabric 1.20.1
# Tarea: smartPingDebug
# Salida: build/libs/SmartPingBeta-1.0.0-dev.jar
# ============================================================================
#
# Uso:
#   chmod +x scripts/build-dev.sh
#   ./scripts/build-dev.sh
# ============================================================================

set -euo pipefail

# Obtener el directorio del script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo ""
echo "========================================"
echo "SmartPingBeta - Build de Desarrollo"
echo "========================================"
echo ""

# Cambiar al directorio del proyecto
cd "$PROJECT_DIR"

# Ejecutar build de desarrollo
./gradlew clean smartPingDebug --stacktrace -Penv=debug

echo ""
echo "========================================"
echo "Build de desarrollo completado exitosamente"
echo "========================================"
echo ""

