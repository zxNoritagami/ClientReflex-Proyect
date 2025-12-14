package dev.zxnoragami.clientreflex.ping;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de perfiles de conexión según el ping.
 * 
 * Aplica ajustes automáticos de opciones del cliente según el ping actual
 * para optimizar la experiencia en diferentes condiciones de red.
 * 
 * Este sistema es una versión avanzada del "modo conexión débil" que define
 * perfiles específicos para diferentes rangos de ping.
 */
public class PingProfileManager {
    private static PingProfileManager instance;
    
    /**
     * Rangos de ping y sus perfiles correspondientes.
     */
    public enum PingRange {
        LOW,        // < pingLowThreshold
        MEDIUM,     // pingLowThreshold - pingMediumThreshold
        HIGH,       // pingMediumThreshold - pingHighThreshold
        CRITICAL    // > pingHighThreshold
    }
    
    /**
     * Perfil de configuración para un rango de ping.
     */
    public static class PingProfile {
        public int renderDistance;
        public double entityDistanceScaling;
        public int particleQuality; // 0 = mínimo, 1 = reducido, 2 = todos
        public boolean enableVignette;
        public boolean enableFog;
        
        public PingProfile(int renderDistance, double entityDistanceScaling, 
                          int particleQuality, boolean enableVignette, boolean enableFog) {
            this.renderDistance = renderDistance;
            this.entityDistanceScaling = entityDistanceScaling;
            this.particleQuality = particleQuality;
            this.enableVignette = enableVignette;
            this.enableFog = enableFog;
        }
    }
    
    private final Map<PingRange, PingProfile> profiles = new HashMap<>();
    private PingRange currentRange = PingRange.LOW;
    private PingRange lastAppliedRange = null;
    private long lastProfileChange = 0;
    private static final long PROFILE_CHANGE_COOLDOWN = 5000; // 5 segundos de histeresis
    
    // Valores originales guardados
    private int originalRenderDistance = -1;
    private double originalEntityDistanceScaling = -1;
    
    private PingProfileManager() {
        initializeProfiles();
    }
    
    public static PingProfileManager getInstance() {
        if (instance == null) {
            instance = new PingProfileManager();
        }
        return instance;
    }
    
    /**
     * Inicializa los perfiles por defecto.
     */
    private void initializeProfiles() {
        // Perfil LOW: ping bajo, máxima calidad
        profiles.put(PingRange.LOW, new PingProfile(
            12,  // renderDistance
            1.0, // entityDistanceScaling
            2,   // particleQuality (todos)
            true, // enableVignette
            true  // enableFog
        ));
        
        // Perfil MEDIUM: ping medio, calidad reducida
        profiles.put(PingRange.MEDIUM, new PingProfile(
            10,  // renderDistance
            0.8, // entityDistanceScaling
            1,   // particleQuality (reducido)
            true, // enableVignette
            true  // enableFog
        ));
        
        // Perfil HIGH: ping alto, calidad mínima
        profiles.put(PingRange.HIGH, new PingProfile(
            8,   // renderDistance
            0.6, // entityDistanceScaling
            0,   // particleQuality (mínimo)
            false, // enableVignette
            false  // enableFog
        ));
        
        // Perfil CRITICAL: ping crítico, calidad muy reducida
        profiles.put(PingRange.CRITICAL, new PingProfile(
            6,   // renderDistance
            0.4, // entityDistanceScaling
            0,   // particleQuality (mínimo)
            false, // enableVignette
            false  // enableFog
        ));
    }
    
    /**
     * Actualiza el perfil según el ping actual.
     * Debe llamarse periódicamente (cada 5 segundos aproximadamente).
     */
    public void update(PingMetrics metrics) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) {
            return;
        }
        
        long currentPing = metrics.getCurrentPingMs();
        var config = ClientReflexConfig.getConfig();
        
        // Determinar el rango actual
        PingRange newRange;
        if (currentPing < config.pingLowThresholdMs) {
            newRange = PingRange.LOW;
        } else if (currentPing < config.pingMediumThresholdMs) {
            newRange = PingRange.MEDIUM;
        } else if (currentPing < config.pingHighThresholdMs) {
            newRange = PingRange.HIGH;
        } else {
            newRange = PingRange.CRITICAL;
        }
        
        currentRange = newRange;
        
        // Aplicar perfil si cambió y pasó el cooldown
        long now = System.currentTimeMillis();
        if (newRange != lastAppliedRange && 
            (now - lastProfileChange) >= PROFILE_CHANGE_COOLDOWN) {
            applyProfile(newRange, client.options);
            lastAppliedRange = newRange;
            lastProfileChange = now;
            ClientReflexMod.LOGGER.info("Perfil de ping aplicado: " + newRange + " (ping: " + currentPing + "ms)");
        }
    }
    
    /**
     * Aplica un perfil de configuración.
     */
    private void applyProfile(PingRange range, GameOptions options) {
        PingProfile profile = profiles.get(range);
        if (profile == null) {
            return;
        }
        
        // Guardar valores originales la primera vez
        if (originalRenderDistance == -1) {
            originalRenderDistance = options.getViewDistance().getValue();
            originalEntityDistanceScaling = options.getEntityDistanceScaling().getValue();
            // particleQuality no está directamente en GameOptions, se maneja diferente
        }
        
        // Aplicar perfil
        options.getViewDistance().setValue(profile.renderDistance);
        options.getEntityDistanceScaling().setValue(profile.entityDistanceScaling);
        
        // Nota: particleQuality y otros ajustes requieren acceso a más opciones
        // que pueden no estar disponibles directamente en GameOptions
    }
    
    /**
     * Restaura los valores originales.
     */
    public void restoreOriginal() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null || originalRenderDistance == -1) {
            return;
        }
        
        client.options.getViewDistance().setValue(originalRenderDistance);
        client.options.getEntityDistanceScaling().setValue(originalEntityDistanceScaling);
        
        lastAppliedRange = null;
        ClientReflexMod.LOGGER.info("Valores originales restaurados");
    }
    
    /**
     * Obtiene el rango de ping actual.
     */
    public PingRange getCurrentRange() {
        return currentRange;
    }
    
    /**
     * Obtiene el perfil para un rango específico.
     */
    public PingProfile getProfile(PingRange range) {
        return profiles.get(range);
    }
}

