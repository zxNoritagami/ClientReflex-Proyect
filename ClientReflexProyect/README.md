# Smart Connection - Mod de Cliente para Minecraft

## Descripción

Smart Connection es un mod de cliente para Minecraft (Fabric) diseñado para mejorar la tolerancia a problemas de conexión y proporcionar herramientas de diagnóstico de red al jugador. El mod ayuda a prevenir desconexiones por timeouts, facilita la reconexión automática y proporciona información detallada sobre el estado de la conexión.

## Características Principales

### 1. Timeout de Lectura Configurable
- Permite configurar el tiempo de espera antes de desconectar por inactividad
- Valores configurables entre 10 y 180 segundos (por defecto: 45 segundos)
- Más tolerante a lagazos temporales y microcortes de conexión
- Optimizado para ping normal de ~120ms: balance entre tolerancia y evitar que todo se sienta "pegado"

### 2. Auto-Reconexión Inteligente
- Reconexión automática cuando se pierde la conexión por timeouts o errores recuperables
- Configurable: tiempo de espera entre intentos (por defecto: 3 segundos) y número máximo de intentos (por defecto: 8)
- Botones en la pantalla de desconexión para reconectar manualmente o activar auto-reconexión
- Detecta automáticamente si una desconexión es recuperable (no intenta reconectar si fuiste baneado, por ejemplo)
- Optimizado para reconexión rápida en PvP con mejor tolerancia a caídas en cadena

### 3. HUD de Monitor de Red en Tiempo Real
- Muestra información detallada de la conexión:
  - Ping actual, promedio y máximo
  - Jitter (variación del ping)
  - Tiempo desde el último paquete recibido
  - Estadísticas de paquetes recibidos
- Se puede mostrar/ocultar con la tecla **H** (configurable)
- Posición configurable en la pantalla

### 4. Ajustes Automáticos del Cliente
- **Modo Conexión Débil**: Se activa automáticamente cuando:
  - El ping supera un umbral configurable (por defecto: 200ms)
  - Hay inestabilidad en la conexión
- Reduce automáticamente:
  - Distancia de renderizado (por defecto: 6 chunks)
  - Distancia de entidades
- Restaura automáticamente los valores originales cuando la conexión se estabiliza

### 5. Keep-Alive Robusto
- Asegura respuestas rápidas a los paquetes keep-alive del servidor
- No modifica el protocolo, solo optimiza el manejo de los paquetes

### 6. Sistema de Logging de Errores
- Registra todos los errores de conexión en `.minecraft/SmartConnection.logs/connection-errors.log`
- Incluye información detallada sobre timeouts, desconexiones y excepciones
- Permite revisar qué causó las desconexiones para encontrar soluciones
- Formato legible con timestamps y stack traces completos

### 7. Sistema de Predicción de Entidades (Tipo Marlow Crystal)
- **Predicción Cliente-Side**: Reduce el delay percibido en interacciones rápidas en alto ping
- **End Crystals**: Predice la destrucción de crystals al atacarlas, eliminándolas visualmente inmediatamente
- **Camas y Anchors**: Predice la destrucción de camas/anchor en dimensiones explosivas
- **TNT Minecarts**: Predice la destrucción de minecarts al activarlos
- **Reconciliación Automática**: Se sincroniza con el servidor para corregir predicciones incorrectas
- **No Modifica el Protocolo**: Solo adelanta la eliminación visual, nunca envía paquetes ilegales
- **Telemetría**: Registra estadísticas de aciertos y rollbacks
- **Configurable**: Activa/desactiva módulos individualmente y ajusta timeouts

### 8. Módulo de Optimización de Ping (Percepción de Latencia)
- **Medición Avanzada de Ping**: Sistema preciso de métricas con historial, jitter y tendencias
- **Priorización de Paquetes**: PriorityWriteHandler que envía primero los paquetes críticos (movimiento, ataque)
- **Optimización de Netty**: Configura TCP_NODELAY y SO_KEEPALIVE para reducir latencia
- **Input & Visual Smoothing**: Feedback visual instantáneo en ataques y acciones
- **Interpolación de Entidades Remotas**: Suaviza el movimiento de otros jugadores en alto ping
- **Perfiles Avanzados según Ping**: Ajusta automáticamente calidad gráfica según el ping (LOW/MEDIUM/HIGH/CRITICAL)
- **Herramientas de Diagnóstico**: Comando `/smartconnection pingdiag` con información detallada
- **Camas y Anchors**: Predice la destrucción de camas/anchor en dimensiones explosivas
- **TNT Minecarts**: Predice la destrucción de minecarts al activarlos
- **Reconciliación Automática**: Se sincroniza con el servidor para corregir predicciones incorrectas
- **No Modifica el Protocolo**: Solo adelanta la eliminación visual, nunca envía paquetes ilegales
- **Telemetría**: Registra estadísticas de aciertos y rollbacks
- **Configurable**: Activa/desactiva módulos individualmente y ajusta timeouts

## Instalación

1. Asegúrate de tener **Fabric Loader** instalado para Minecraft 1.20.1
2. Descarga el archivo JAR del mod desde [releases](https://github.com/tuusuario/smart-connection/releases)
3. Coloca el archivo JAR en la carpeta `mods` de tu instalación de Minecraft
4. Inicia el juego

## Compilación

SmartPingBeta es un mod de cliente para Fabric 1.20.1. Para compilar el mod desde el código fuente:

### Requisitos Previos

- **Java 17** o superior
- **Gradle** (se incluye el wrapper en el proyecto)

### Build de Desarrollo

Para compilar una versión de desarrollo (rápida, con información de debug):

**Windows:**
```batch
scripts\build-dev.bat
```

**Linux/macOS:**
```bash
chmod +x scripts/build-dev.sh
./scripts/build-dev.sh
```

El JAR de desarrollo se generará en: `build/libs/SmartPingBeta-1.0.0-dev.jar`

### Build de Release

Para compilar una versión de producción optimizada:

**Windows:**
```batch
scripts\build-release.bat
```

**Linux/macOS:**
```bash
chmod +x scripts/build-release.sh
./scripts/build-release.sh
```

El JAR de producción se generará en: `build/releases/SmartPingBeta-1.0.0-release.jar`

Este JAR está listo para distribución y debe copiarse a `.minecraft/mods/` para su uso.

### Notas de Compilación

- El build de release incluye optimizaciones agresivas del compilador y está deshabilitada la información de debug
- El build de desarrollo es más rápido y útil para testing local
- Puedes controlar el modo de build con la propiedad `-Penv=debug` o `-Penv=release`
- El proyecto usa Java 17 y requiere Fabric Loom 1.8.9 o superior

## Configuración

El mod crea un archivo de configuración en: `.minecraft/config/smartconnection.json`

### Valores Configurables

```json
{
  "readTimeoutSeconds": 45,
  "maxReadTimeoutSeconds": 180,
  "autoReconnectEnabled": true,
  "autoReconnectDelaySeconds": 3,
  "maxReconnectAttempts": 8,
  "hudToggleKey": "key.keyboard.h",
  "hudEnabled": false,
  "hudPosition": "top_left",
  "weakConnectionPingThreshold": 200,
  "weakConnectionRenderDistance": 6,
  "weakConnectionStablePingThreshold": 150,
  "pingHistorySize": 100,
  "networkStatsWindowSeconds": 10,
  "predictCrystals": true,
  "predictBeds": true,
  "predictAnchors": true,
  "predictTntMinecarts": true,
  "crystalPredictionTimeoutMs": 500,
  "bedPredictionTimeoutMs": 400,
  "anchorPredictionTimeoutMs": 400,
  "tntMinecartPredictionTimeoutMs": 500,
  "predictionMode": "SAFE",
  "showPredictionStats": true,
  "pingLowThresholdMs": 60,
  "pingMediumThresholdMs": 120,
  "pingHighThresholdMs": 200,
  "pingCriticalThresholdMs": 320,
  "tcpNoDelayOverride": true,
  "soKeepAliveOverride": true,
  "enablePriorityWriteHandler": true,
  "enableMovementSmoothing": true,
  "enableRemoteEntityInterpolation": true,
  "enableInputSmoothing": true,
  "pingMetricsHistorySize": 60
}
```

### Explicación de Parámetros

- **readTimeoutSeconds**: Tiempo en segundos antes de desconectar por inactividad (10-180, por defecto: 45)
  - Optimizado para ~120ms de ping: balance entre tolerancia a cortes cortos y evitar que todo se sienta "pegado"
- **autoReconnectEnabled**: Activar/desactivar auto-reconexión
- **autoReconnectDelaySeconds**: Segundos de espera entre intentos de reconexión (por defecto: 3)
  - Reconexión más rápida, especialmente útil en PvP
- **maxReconnectAttempts**: Número máximo de intentos de reconexión (por defecto: 8)
  - Mejor tolerancia a caídas en cadena sin bucles infinitos
- **weakConnectionPingThreshold**: Ping en ms que activa el modo conexión débil
- **weakConnectionRenderDistance**: Distancia de renderizado cuando está en modo débil
- **predictCrystals/Beds/Anchors/TntMinecarts**: Activar/desactivar predicción por tipo
- **crystalPredictionTimeoutMs**: Timeout para predicciones de crystals (ms, por defecto: 500)
  - Ajustado para ~120ms de ping: ~120ms ida + ~120ms vuelta + margen para jitter/spike
- **bedPredictionTimeoutMs / anchorPredictionTimeoutMs**: Timeout para predicciones (ms, por defecto: 400)
  - Menor timeout para reducir desincronización visual
- **predictionMode**: Modo de predicción (OFF, SAFE, AGGRESSIVE)
- **showPredictionStats**: Mostrar estadísticas de predicción en el HUD
- **pingLowThresholdMs**: Umbral bajo (por defecto: 60ms)
- **pingMediumThresholdMs**: Umbral medio (por defecto: 120ms, centrado en ping normal)
- **pingHighThresholdMs**: Umbral alto (por defecto: 200ms)
- **pingCriticalThresholdMs**: Umbral crítico (por defecto: 320ms)
  - Umbrales adaptados para ping normal de ~120ms: tu "normal" se ve como medio, no como casi alto
- **tcpNoDelayOverride**: Forzar TCP_NODELAY para reducir latencia
- **soKeepAliveOverride**: Forzar SO_KEEPALIVE para mantener conexión activa
- **enablePriorityWriteHandler**: Priorizar paquetes críticos (movimiento, ataque)
- **enableMovementSmoothing**: Suavizar movimiento remoto
- **enableRemoteEntityInterpolation**: Interpolar entidades remotas para movimiento más fluido
- **enableInputSmoothing**: Feedback visual instantáneo en acciones

## Comandos

El mod añade el comando `/smartconnection` con las siguientes opciones:

- `/smartconnection reload` - Recarga la configuración desde el archivo
- `/smartconnection timeout <segundos>` - Establece el timeout de lectura (10-180)
- `/smartconnection info` - Muestra información sobre la configuración actual
- `/smartconnection predictionstats` - Muestra estadísticas de predicción (aciertos, rollbacks, etc.)
- `/smartconnection pingdiag` - Muestra diagnóstico completo de ping y optimizaciones

## Uso

### Mostrar/Ocultar HUD de Red
Presiona **H** (o la tecla configurada) para mostrar u ocultar el HUD de información de red.

### Auto-Reconexión
Cuando te desconectes:
1. Aparecerá una pantalla de desconexión con dos botones
2. **"Reintentar ahora"**: Reconecta inmediatamente
3. **"Auto-reconectar"**: Activa la reconexión automática con los parámetros configurados

### Modo Conexión Débil
Se activa automáticamente cuando detecta problemas de conexión. No requiere acción del usuario, pero puedes desactivarlo manualmente cambiando las opciones de video.

### Sistema de Predicción
El sistema de predicción funciona automáticamente cuando está habilitado. Reduce el delay percibido en acciones rápidas como:
- Atacar end crystals
- Usar camas en el Nether/End
- Usar respawn anchors sin carga
- Activar TNT minecarts

Las entidades/bloques se eliminan visualmente inmediatamente, y el sistema se sincroniza con el servidor para corregir cualquier discrepancia.

**Nota importante**: Este sistema es solo visual y no modifica el protocolo. Si una predicción es incorrecta, se revierte automáticamente.

### Logs de Errores
Todos los errores de conexión se guardan en `.minecraft/SmartConnection.logs/connection-errors.log`. Revisa este archivo si experimentas desconexiones frecuentes para identificar la causa.

### Optimización de Ping
El módulo de optimización de ping funciona automáticamente cuando está habilitado:

- **Priorización de Paquetes**: Los paquetes críticos (movimiento, ataque) se envían primero
- **Perfiles Automáticos**: La calidad gráfica se ajusta según el ping para mantener fluidez
- **Suavizado Visual**: El movimiento de otros jugadores se interpola para verse más fluido
- **Feedback Instantáneo**: Las acciones del jugador tienen respuesta visual inmediata

**Nota importante**: Este módulo optimiza la **percepción de latencia**, no el ping real. El ping físico no se puede reducir (está limitado por la velocidad de la luz y la distancia al servidor), pero el mod mejora cómo se siente el juego optimizando el uso de la conexión y adelantando feedback visual.

### Diagnóstico de Ping
Usa `/smartconnection pingdiag` para ver:
- Métricas detalladas de ping (actual, promedio, min, max, jitter, tendencia)
- Perfil de conexión actual (LOW/MEDIUM/HIGH/CRITICAL)
- Estado de optimizaciones de Netty (TCP_NODELAY, SO_KEEPALIVE)
- Umbrales configurados
- Sugerencias basadas en las métricas

## Requisitos

- **Minecraft**: 1.20.1
- **Fabric Loader**: 0.14.22 o superior
- **Fabric API**: 0.83.0+1.20.1 o superior
- **Java**: 17 o superior

## Desarrollo

### Compilar desde el Código Fuente

1. Clona el repositorio
2. Ejecuta `./gradlew build` (o `gradlew.bat build` en Windows)
3. El JAR se generará en `build/libs/`

### Estructura del Proyecto

```
src/main/java/com/example/smartconnection/
├── SmartConnectionMod.java          # Clase principal del mod
├── SmartConnectionClient.java        # Cliente del mod
├── config/
│   └── SmartConnectionConfig.java    # Sistema de configuración
├── net/
│   ├── NetworkMonitor.java           # Monitor de métricas de red
│   ├── ConfigurableReadTimeoutHandler.java  # Handler de timeout
│   └── RobustKeepAliveHandler.java   # Handler de keep-alive
├── ui/
│   ├── NetworkHUD.java               # HUD de información de red
│   └── AutoReconnectManager.java     # Gestor de auto-reconexión
├── client/
│   └── WeakConnectionManager.java    # Gestor de modo conexión débil
├── mixin/
│   ├── ClientConnectionMixin.java    # Mixin para la conexión del cliente
│   └── DisconnectedScreenMixin.java  # Mixin para la pantalla de desconexión
└── command/
    └── SmartConnectionCommand.java   # Comandos del mod
```

## Licencia

Este proyecto está licenciado bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## Contribuciones

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu característica
3. Realiza tus cambios
4. Envía un Pull Request

## Notas Técnicas

- El mod es **solo de cliente**, no necesita estar instalado en el servidor
- No modifica el protocolo de Minecraft, solo optimiza el manejo de la conexión del lado del cliente
- Los mixins se usan para interceptar y modificar el comportamiento de las clases de Minecraft
- El timeout configurable reemplaza el ReadTimeoutHandler por defecto de Netty con uno personalizado

## Solución de Problemas

### El mod no se carga
- Verifica que tienes Fabric Loader y Fabric API instalados
- Asegúrate de que la versión de Minecraft coincide (1.20.1)

### El HUD no aparece
- Presiona la tecla H (o la configurada)
- Verifica que estás conectado a un servidor

### La auto-reconexión no funciona
- Verifica que `autoReconnectEnabled` está en `true` en la configuración
- Algunas desconexiones (como baneos) no son recuperables y no intentarán reconectar

## Autor

Tu Nombre

## Agradecimientos

- Fabric Team por el framework
- La comunidad de modding de Minecraft
