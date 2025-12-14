package dev.zxnoragami.clientreflex.client;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.ClientReflexClient;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Gestor de modo conexión débil.
 * Ajusta automáticamente las opciones del cliente cuando la conexión es inestable
 * para reducir el estrés de la red y mejorar la experiencia.
 */
public class WeakConnectionManager {
    private static WeakConnectionManager instance;
    private boolean weakConnectionMode = false;
    private int originalRenderDistance = -1;
    private Double originalEntityDistance = null; // Double porque getEntityDistanceScaling() devuelve Double
    private long weakConnectionStartTime = 0;
    private long stableConnectionStartTime = 0;
    private static final long WEAK_CONNECTION_THRESHOLD_MS = 5000; // 5 segundos para activar modo débil
    private static final long STABLE_CONNECTION_THRESHOLD_MS = 10000; // 10 segundos para restaurar

    private WeakConnectionManager() {
    }

    public static WeakConnectionManager getInstance() {
        if (instance == null) {
            instance = new WeakConnectionManager();
        }
        return instance;
    }

    /**
     * Actualiza el estado del modo conexión débil basado en las métricas de red.
     * Debe llamarse periódicamente (cada tick del cliente).
     */
    public void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null || client.world == null) {
            return;
        }

        if (ClientReflexClient.getNetworkMonitor() == null) {
            return;
        }

        long currentPing = ClientReflexClient.getNetworkMonitor().getCurrentPing();
        double avgPing = ClientReflexClient.getNetworkMonitor().getAveragePing();
        long timeSinceLastPacket = ClientReflexClient.getNetworkMonitor().getTimeSinceLastPacket();

        int weakThreshold = ClientReflexConfig.getConfig().weakConnectionPingThreshold;

        boolean shouldBeWeak = (currentPing > weakThreshold || avgPing > weakThreshold) 
            || timeSinceLastPacket > 2000; // Más de 2 segundos sin paquetes

        long now = System.currentTimeMillis();

        if (shouldBeWeak) {
            if (!weakConnectionMode) {
                weakConnectionStartTime = now;
            }

            // Activar modo débil si se mantiene durante el umbral
            if (now - weakConnectionStartTime >= WEAK_CONNECTION_THRESHOLD_MS && !weakConnectionMode) {
                activateWeakConnectionMode(client.options);
            }
        } else {
            if (weakConnectionMode) {
                if (stableConnectionStartTime == 0) {
                    stableConnectionStartTime = now;
                }

                // Restaurar configuración si la conexión es estable durante el umbral
                if (now - stableConnectionStartTime >= STABLE_CONNECTION_THRESHOLD_MS) {
                    deactivateWeakConnectionMode(client.options);
                }
            } else {
                stableConnectionStartTime = 0;
            }
        }
    }

    /**
     * Activa el modo conexión débil.
     * Guarda las opciones actuales y las reduce para aliviar la carga de red.
     */
    private void activateWeakConnectionMode(GameOptions options) {
        if (weakConnectionMode) {
            return; // Ya está activo
        }

        ClientReflexMod.LOGGER.info("Activando modo conexión débil");

        // Guardar valores originales
        originalRenderDistance = options.getViewDistance().getValue();
        originalEntityDistance = options.getEntityDistanceScaling().getValue();

        // Aplicar valores reducidos
        int weakRenderDistance = ClientReflexConfig.getConfig().weakConnectionRenderDistance;
        options.getViewDistance().setValue(Math.min(weakRenderDistance, originalRenderDistance));
        options.getEntityDistanceScaling().setValue(0.5); // Reducir distancia de entidades a la mitad

        weakConnectionMode = true;
        weakConnectionStartTime = System.currentTimeMillis();
        stableConnectionStartTime = 0;
    }

    /**
     * Desactiva el modo conexión débil y restaura las opciones originales.
     */
    private void deactivateWeakConnectionMode(GameOptions options) {
        if (!weakConnectionMode) {
            return; // No está activo
        }

        ClientReflexMod.LOGGER.info("Desactivando modo conexión débil, restaurando configuración");

        // Restaurar valores originales
        if (originalRenderDistance > 0) {
            options.getViewDistance().setValue(originalRenderDistance);
        }
        if (originalEntityDistance != null && originalEntityDistance > 0) {
            options.getEntityDistanceScaling().setValue(originalEntityDistance);
        }

        weakConnectionMode = false;
        originalRenderDistance = -1;
        originalEntityDistance = null;
        stableConnectionStartTime = 0;
    }

    /**
     * Fuerza la desactivación del modo conexión débil.
     * Útil cuando el jugador cambia manualmente las opciones.
     */
    public void forceDeactivate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            deactivateWeakConnectionMode(client.options);
        }
    }

    public boolean isWeakConnectionMode() {
        return weakConnectionMode;
    }
}

