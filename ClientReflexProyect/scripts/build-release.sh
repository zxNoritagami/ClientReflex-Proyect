#!/usr/bin/env bash
# ============================================================================
# SmartPingBeta - Build Script de Release (Linux/macOS)
# ============================================================================
# Mod: SmartPingBeta
# Entorno: Fabric 1.20.1
# Tarea: smartPingRelease
# Salida: build/releases/SmartPingBeta-1.0.0-release.jar
# ============================================================================
#
# Uso:
#   chmod +x scripts/build-release.sh
#   ./scripts/build-release.sh
# ============================================================================

set -euo pipefail

# Obtener el directorio del script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo ""
echo "========================================"
echo "SmartPingBeta - Build de Release"
echo "========================================"
echo ""

# Cambiar al directorio del proyecto
cd "$PROJECT_DIR"

# Ejecutar build de release
./gradlew clean smartPingRelease --no-daemon --parallel --stacktrace -Penv=release

echo ""
echo "========================================"
echo "Build de release completado exitosamente"
echo "========================================"
echo ""
echo "JAR de producción generado en:"
echo "  build/releases/SmartPingBeta-1.0.0-release.jar"
echo ""

