package dev.zxnoragami.clientreflex.config;

import dev.zxnoragami.clientreflex.ClientReflexMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sistema de configuración del mod clientreflex.
 * Guarda y carga la configuración desde un archivo JSON en la carpeta config de Minecraft.
 */
public class ClientReflexConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
        FabricLoader.getInstance().getConfigDir().toFile(),
        "clientreflex.json"
    );

    private static final AtomicReference<ConfigData> config = 
        new AtomicReference<>(new ConfigData());

    /**
     * Datos de configuración del mod.
     * Todos los valores tienen defaults razonables.
     */
    public static class ConfigData {
        // Timeout de lectura configurable
        // 45s: balance entre tolerancia a cortes cortos y evitar que todo se sienta "pegado"
        // Con ~120ms de ping, 30s puede ser justo; 45s da margen sin llegar a extremos (60-90s)
        public int readTimeoutSeconds = 45;
        public int maxReadTimeoutSeconds = 180;

        // Auto-reconexión (legacy, mantenido para compatibilidad)
        public boolean autoReconnectEnabled = true;
        // 3s: reconexión más rápida, especialmente útil en PvP
        public int autoReconnectDelaySeconds = 3;
        // 8 intentos: mejor tolerancia a caídas en cadena sin bucles infinitos
        public int maxReconnectAttempts = 8;
        
        // AntiDisconnect: sistema mejorado de anti-desconexión
        public AntiDisconnectConfig antiDisconnect = new AntiDisconnectConfig();
        
        // Notificaciones en chat
        public ChatNotificationsConfig chatNotifications = new ChatNotificationsConfig();
        
        // Priorización de paquetes
        public PacketPriorityConfig packetPriority = new PacketPriorityConfig();

        // HUD
        public String hudToggleKey = "key.keyboard.h";
        public boolean hudEnabled = false;
        public String hudPosition = "top_left"; // top_left, top_right, bottom_left, bottom_right

        // Modo conexión débil
        public int weakConnectionPingThreshold = 200; // ms
        public int weakConnectionRenderDistance = 6;
        public int weakConnectionStablePingThreshold = 150; // ms para considerar conexión estable

        // Monitor de red
        public int pingHistorySize = 100; // Número de valores de ping a guardar
        public int networkStatsWindowSeconds = 10; // Ventana de tiempo para estadísticas

        // Sistema de predicción
        public boolean predictCrystals = true;
        public boolean predictBeds = true;
        public boolean predictAnchors = true;
        public boolean predictTntMinecarts = true;
        
        // Timeouts de predicción ajustados para ~120ms de ping
        // 500ms para crystals: ~120ms ida + ~120ms vuelta + margen para jitter/spike
        // 400ms para beds/anchors: suelen ir muy pegadas a la interacción, menos desincronización visual
        public long crystalPredictionTimeoutMs = 500;
        public long bedPredictionTimeoutMs = 400;
        public long anchorPredictionTimeoutMs = 400;
        public long tntMinecartPredictionTimeoutMs = 500;
        
        // Modo de predicción: OFF, SAFE (solo visual), AGGRESSIVE (visual + colisiones)
        public String predictionMode = "SAFE";
        public boolean showPredictionStats = true; // Mostrar estadísticas en HUD

        // Optimización de ping
        // Umbrales adaptados para ping normal de ~120ms
        // Medium centrado en 120ms: tu "normal" se ve como medio, no como casi alto
        // High 200ms y Critical 320ms marcan claramente cuando está realmente peor
        public int pingLowThresholdMs = 60;       // < 60ms = LOW
        public int pingMediumThresholdMs = 120;   // 60-120ms = MEDIUM (centrado en tu ping normal)
        public int pingHighThresholdMs = 200;     // 120-200ms = HIGH
        public int pingCriticalThresholdMs = 320; // > 200ms = CRITICAL
        
        // Configuración de Netty
        public boolean tcpNoDelayOverride = true;      // Forzar TCP_NODELAY
        public boolean soKeepAliveOverride = true;     // Forzar SO_KEEPALIVE
        public boolean enablePriorityWriteHandler = true; // Priorizar paquetes
        
        // Suavizado de input y movimiento
        public boolean enableMovementSmoothing = true;        // Suavizar movimiento remoto
        public boolean enableRemoteEntityInterpolation = true; // Interpolar entidades remotas
        public boolean enableInputSmoothing = true;           // Feedback visual instantáneo
        
        // Historial de ping
        public int pingMetricsHistorySize = 60; // Número de valores de ping a guardar
    }
    
    /**
     * Configuración del sistema AntiDisconnect.
     */
    public static class AntiDisconnectConfig {
        public boolean enabled = true;
        public int maxAttempts = 8;
        public int baseDelayMs = 1000; // 1 segundo base
        public int maxDelayMs = 30000; // 30 segundos máximo
        public double multiplier = 1.5; // Multiplicador exponencial
        public int jitterPct = 10; // 0-50% de jitter aleatorio
        public int stableResetSeconds = 30; // Reset de intentos después de N segundos estables
    }
    
    /**
     * Configuración de notificaciones en chat.
     */
    public static class ChatNotificationsConfig {
        public boolean enabled = true;
        public boolean showInstanceId = true;
        public String verbosity = "NORMAL"; // MINIMAL, NORMAL, VERBOSE
        public int rateLimitMs = 2000; // 2 segundos entre mensajes del mismo tipo
        public String prefixStyle = "BRACKETED"; // SIMPLE, BRACKETED
    }
    
    /**
     * Configuración de priorización de paquetes.
     */
    public static class PacketPriorityConfig {
        public String movementPriority = "HIGH";
        public String attackPriority = "HIGH";
        public String blockPlacePriority = "HIGH";
        public String interactPriority = "MEDIUM";
        public String inventoryPriority = "LOW";
        public String chatPriority = "LOW";
        public String keepAlivePriority = "MEDIUM";
    }

    /**
     * Carga la configuración desde el archivo JSON.
     * Si el archivo no existe, crea uno con valores por defecto.
     */
    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded == null) {
                    config.set(new ConfigData());
                } else {
                    config.set(validateAndNormalize(loaded));
                }
                ClientReflexMod.LOGGER.info("Configuración cargada desde: " + CONFIG_FILE.getAbsolutePath());
            } catch (IOException e) {
                ClientReflexMod.LOGGER.error("Error al cargar la configuración: ", e);
                config.set(new ConfigData());
            } catch (Exception e) {
                ClientReflexMod.LOGGER.error("Error al parsear configuración (usando defaults): ", e);
                config.set(new ConfigData());
            }
        } else {
            save(); // Crear archivo con valores por defecto
        }
    }

    /**
     * Guarda la configuración actual en el archivo JSON.
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config.get(), writer);
            ClientReflexMod.LOGGER.info("Configuración guardada en: " + CONFIG_FILE.getAbsolutePath());
        } catch (IOException e) {
            ClientReflexMod.LOGGER.error("Error al guardar la configuración: ", e);
        }
    }

    /**
     * Recarga la configuración desde el archivo.
     */
    public static void reload() {
        load();
        ClientReflexMod.LOGGER.info("Configuración recargada");
    }

    /**
     * Obtiene la configuración actual.
     */
    public static ConfigData getConfig() {
        return config.get();
    }
    
    /**
     * Valida y normaliza los valores de configuración cargados.
     * Asegura que todos los valores estén en rangos válidos.
     */
    private static ConfigData validateAndNormalize(ConfigData data) {
        if (data == null) {
            return new ConfigData();
        }
        
        // Inicializar sub-objetos si son null
        if (data.antiDisconnect == null) {
            data.antiDisconnect = new AntiDisconnectConfig();
        }
        if (data.chatNotifications == null) {
            data.chatNotifications = new ChatNotificationsConfig();
        }
        if (data.packetPriority == null) {
            data.packetPriority = new PacketPriorityConfig();
        }
        
        // Validar timeouts
        data.readTimeoutSeconds = Math.max(10, Math.min(180, data.readTimeoutSeconds));
        if (data.maxReadTimeoutSeconds < data.readTimeoutSeconds) {
            data.maxReadTimeoutSeconds = data.readTimeoutSeconds;
        }
        data.maxReadTimeoutSeconds = Math.min(300, data.maxReadTimeoutSeconds);
        
        // Validar AutoReconnect (legacy)
        data.autoReconnectDelaySeconds = Math.max(1, Math.min(60, data.autoReconnectDelaySeconds));
        data.maxReconnectAttempts = Math.max(1, Math.min(50, data.maxReconnectAttempts));
        
        // Validar AntiDisconnect
        data.antiDisconnect.maxAttempts = Math.max(1, Math.min(50, data.antiDisconnect.maxAttempts));
        data.antiDisconnect.baseDelayMs = Math.max(100, Math.min(10000, data.antiDisconnect.baseDelayMs));
        data.antiDisconnect.maxDelayMs = Math.max(data.antiDisconnect.baseDelayMs, 
            Math.min(120000, data.antiDisconnect.maxDelayMs));
        data.antiDisconnect.multiplier = Math.max(1.0, Math.min(5.0, data.antiDisconnect.multiplier));
        data.antiDisconnect.jitterPct = Math.max(0, Math.min(50, data.antiDisconnect.jitterPct));
        data.antiDisconnect.stableResetSeconds = Math.max(5, Math.min(300, data.antiDisconnect.stableResetSeconds));
        
        // Validar ChatNotifications
        data.chatNotifications.rateLimitMs = Math.max(0, Math.min(10000, data.chatNotifications.rateLimitMs));
        
        // Validar enums de ChatNotifications
        if (!isValidVerbosity(data.chatNotifications.verbosity)) {
            ClientReflexMod.LOGGER.warn("Verbosity inválido '{}', usando NORMAL", data.chatNotifications.verbosity);
            data.chatNotifications.verbosity = "NORMAL";
        }
        if (!isValidPrefixStyle(data.chatNotifications.prefixStyle)) {
            ClientReflexMod.LOGGER.warn("PrefixStyle inválido '{}', usando BRACKETED", data.chatNotifications.prefixStyle);
            data.chatNotifications.prefixStyle = "BRACKETED";
        }
        
        // Validar prioridades de paquetes
        data.packetPriority.movementPriority = validatePriority(data.packetPriority.movementPriority, "HIGH");
        data.packetPriority.attackPriority = validatePriority(data.packetPriority.attackPriority, "HIGH");
        data.packetPriority.blockPlacePriority = validatePriority(data.packetPriority.blockPlacePriority, "HIGH");
        data.packetPriority.interactPriority = validatePriority(data.packetPriority.interactPriority, "MEDIUM");
        data.packetPriority.inventoryPriority = validatePriority(data.packetPriority.inventoryPriority, "LOW");
        data.packetPriority.chatPriority = validatePriority(data.packetPriority.chatPriority, "LOW");
        data.packetPriority.keepAlivePriority = validatePriority(data.packetPriority.keepAlivePriority, "MEDIUM");
        
        // Validar umbrales de ping (asegurar orden correcto)
        int[] thresholds = {
            Math.max(0, data.pingLowThresholdMs),
            Math.max(0, data.pingMediumThresholdMs),
            Math.max(0, data.pingHighThresholdMs),
            Math.max(0, data.pingCriticalThresholdMs)
        };
        Arrays.sort(thresholds);
        data.pingLowThresholdMs = thresholds[0];
        data.pingMediumThresholdMs = Math.max(thresholds[0] + 1, thresholds[1]);
        data.pingHighThresholdMs = Math.max(thresholds[1] + 1, thresholds[2]);
        data.pingCriticalThresholdMs = Math.max(thresholds[2] + 1, thresholds[3]);
        
        // Validar otros valores
        data.weakConnectionPingThreshold = Math.max(50, Math.min(1000, data.weakConnectionPingThreshold));
        data.weakConnectionRenderDistance = Math.max(2, Math.min(32, data.weakConnectionRenderDistance));
        data.pingHistorySize = Math.max(10, Math.min(1000, data.pingHistorySize));
        data.networkStatsWindowSeconds = Math.max(1, Math.min(60, data.networkStatsWindowSeconds));
        data.pingMetricsHistorySize = Math.max(10, Math.min(1000, data.pingMetricsHistorySize));
        
        // Validar timeouts de predicción
        data.crystalPredictionTimeoutMs = Math.max(100, Math.min(5000, data.crystalPredictionTimeoutMs));
        data.bedPredictionTimeoutMs = Math.max(100, Math.min(5000, data.bedPredictionTimeoutMs));
        data.anchorPredictionTimeoutMs = Math.max(100, Math.min(5000, data.anchorPredictionTimeoutMs));
        data.tntMinecartPredictionTimeoutMs = Math.max(100, Math.min(5000, data.tntMinecartPredictionTimeoutMs));
        
        return data;
    }
    
    /**
     * Valida si un valor de verbosity es válido.
     */
    private static boolean isValidVerbosity(String verbosity) {
        return verbosity != null && 
               (verbosity.equals("MINIMAL") || verbosity.equals("NORMAL") || verbosity.equals("VERBOSE"));
    }
    
    /**
     * Valida si un valor de prefixStyle es válido.
     */
    private static boolean isValidPrefixStyle(String prefixStyle) {
        return prefixStyle != null && 
               (prefixStyle.equals("SIMPLE") || prefixStyle.equals("BRACKETED"));
    }
    
    /**
     * Valida y normaliza un valor de prioridad.
     */
    private static String validatePriority(String priority, String defaultValue) {
        if (priority == null) {
            return defaultValue;
        }
        String upper = priority.toUpperCase();
        if (upper.equals("HIGH") || upper.equals("MEDIUM") || upper.equals("LOW")) {
            return upper;
        }
        ClientReflexMod.LOGGER.warn("Prioridad inválida '{}', usando {}", priority, defaultValue);
        return defaultValue;
    }
}

