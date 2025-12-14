package dev.zxnoragami.clientreflex.ui;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Gestor de auto-reconexión inteligente.
 * Maneja la lógica de reconexión automática cuando se pierde la conexión.
 */
public class AutoReconnectManager {
    private static AutoReconnectManager instance;
    private ServerInfo lastServerInfo;
    private String lastServerAddress;
    private int lastServerPort;
    private boolean isReconnecting = false;
    private int reconnectAttempts = 0;
    private Timer reconnectTimer;

    private AutoReconnectManager() {
    }

    public static AutoReconnectManager getInstance() {
        if (instance == null) {
            instance = new AutoReconnectManager();
        }
        return instance;
    }

    /**
     * Guarda la información del servidor al que se está conectando.
     */
    public void saveServerInfo(ServerInfo serverInfo) {
        this.lastServerInfo = serverInfo;
        if (serverInfo != null && serverInfo.address != null) {
            String[] parts = serverInfo.address.split(":");
            this.lastServerAddress = parts[0];
            this.lastServerPort = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;
        }
    }

    /**
     * Guarda la información del servidor desde una dirección directa.
     */
    public void saveServerAddress(String address, int port) {
        this.lastServerAddress = address;
        this.lastServerPort = port;
        this.lastServerInfo = null;
    }

    /**
     * Inicia el proceso de auto-reconexión si está habilitado.
     * 
     * @param reason Razón de la desconexión
     * @return true si se inició la auto-reconexión, false en caso contrario
     */
    public boolean startAutoReconnect(Text reason) {
        if (!ClientReflexConfig.getConfig().autoReconnectEnabled) {
            return false;
        }

        // Verificar si la razón de desconexión es recuperable
        if (!isRecoverableDisconnect(reason)) {
            ClientReflexMod.LOGGER.info("Desconexión no recuperable, no se intentará reconectar");
            return false;
        }

        // Verificar límite de intentos
        int maxAttempts = ClientReflexConfig.getConfig().maxReconnectAttempts;
        if (reconnectAttempts >= maxAttempts) {
            ClientReflexMod.LOGGER.info("Se alcanzó el límite de intentos de reconexión");
            reset();
            return false;
        }

        if (lastServerAddress == null && lastServerInfo == null) {
            ClientReflexMod.LOGGER.warn("No hay información del servidor guardada");
            return false;
        }

        if (isReconnecting) {
            return true; // Ya se está reconectando
        }

        isReconnecting = true;
        reconnectAttempts++;

        int delaySeconds = ClientReflexConfig.getConfig().autoReconnectDelaySeconds;
        ClientReflexMod.LOGGER.info("Iniciando auto-reconexión en {} segundos (intento {}/{})", 
            delaySeconds, reconnectAttempts, maxAttempts);

        // Programar la reconexión
        reconnectTimer = new Timer();
        reconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                MinecraftClient.getInstance().execute(() -> {
                    attemptReconnect();
                });
            }
        }, delaySeconds * 1000L);

        return true;
    }

    /**
     * Intenta reconectar al servidor guardado.
     * 
     * NOTA: La reconexión es semi-automática por diseño. Abre la pantalla de multijugador
     * donde el usuario puede confirmar la reconexión. Esto evita comportamientos tipo "auto-bot"
     * y es más seguro. Si se desea una reconexión completamente automática, se requeriría
     * acceso a métodos internos de Minecraft que podrían cambiar entre versiones.
     */
    private void attemptReconnect() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.world != null) {
            // Ya estamos conectados
            reset();
            return;
        }

        try {
            if (lastServerInfo != null) {
                // Reconectar usando ServerInfo
                // Abre la pantalla de multijugador donde el usuario puede confirmar
                ClientReflexMod.LOGGER.info("Reconectando a servidor: {}", lastServerInfo.name);
                client.setScreen(new MultiplayerScreen(new TitleScreen()));
                // La reconexión real se manejará desde la pantalla de multiplayer
                // o desde el botón "Reintentar ahora" en DisconnectedScreenMixin
            } else if (lastServerAddress != null) {
                // Reconectar usando dirección directa
                ClientReflexMod.LOGGER.info("Reconectando a: {}:{}", lastServerAddress, lastServerPort);
                // Esto requeriría acceso a métodos internos, se manejará desde el mixin
                // Por ahora, también abre la pantalla de multijugador
                client.setScreen(new MultiplayerScreen(new TitleScreen()));
            }
        } catch (Exception e) {
            ClientReflexMod.LOGGER.error("Error al intentar reconectar: ", e);
            isReconnecting = false;
        }
    }

    /**
     * Verifica si una desconexión es recuperable (se puede intentar reconectar).
     */
    private boolean isRecoverableDisconnect(Text reason) {
        if (reason == null) {
            return true; // Si no hay razón, asumimos que es recuperable
        }

        String reasonStr = reason.getString().toLowerCase();
        
        // Desconexiones recuperables
        String[] recoverablePatterns = {
            "timeout",
            "timed out",
            "tiempo de espera",
            "readtimeout",
            "connection lost",
            "conexión perdida",
            "connection reset",
            "conexión reseteada"
        };

        for (String pattern : recoverablePatterns) {
            if (reasonStr.contains(pattern)) {
                return true;
            }
        }

        // Desconexiones no recuperables (el servidor rechazó explícitamente)
        String[] nonRecoverablePatterns = {
            "banned",
            "baneado",
            "kicked",
            "expulsado",
            "you are not whitelisted",
            "no estás en la lista blanca"
        };

        for (String pattern : nonRecoverablePatterns) {
            if (reasonStr.contains(pattern)) {
                return false;
            }
        }

        // Por defecto, asumimos que es recuperable
        return true;
    }

    /**
     * Resetea el estado de auto-reconexión.
     */
    public void reset() {
        isReconnecting = false;
        reconnectAttempts = 0;
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
            reconnectTimer = null;
        }
    }

    /**
     * Cancela la auto-reconexión.
     */
    public void cancel() {
        reset();
    }

    public boolean isReconnecting() {
        return isReconnecting;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    public int getRemainingAttempts() {
        return ClientReflexConfig.getConfig().maxReconnectAttempts - reconnectAttempts;
    }
}

