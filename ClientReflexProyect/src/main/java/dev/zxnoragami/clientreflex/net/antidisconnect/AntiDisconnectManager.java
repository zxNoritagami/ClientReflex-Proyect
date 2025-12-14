package dev.zxnoragami.clientreflex.net.antidisconnect;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.net.ChatNotifier;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerInfo;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gestor del sistema AntiDisconnect con state machine.
 * Maneja detección, recuperación y reconexión automática.
 */
public class AntiDisconnectManager {
    private static AntiDisconnectManager instance;
    
    private final AtomicReference<AntiDisconnectState> state = new AtomicReference<>(AntiDisconnectState.IDLE);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "clientreflex-AntiDisconnect");
        t.setDaemon(true);
        return t;
    });
    
    private ScheduledFuture<?> currentTask;
    private String currentInstanceId;
    private int currentAttempt = 0;
    private long lastStableTime = 0;
    private ServerInfo lastServerInfo;
    private String lastServerAddress;
    private int lastServerPort;
    
    private final Random random = new Random();
    
    private AntiDisconnectManager() {
    }
    
    public static AntiDisconnectManager getInstance() {
        if (instance == null) {
            instance = new AntiDisconnectManager();
        }
        return instance;
    }
    
    /**
     * Obtiene el estado actual.
     */
    public AntiDisconnectState getState() {
        return state.get();
    }
    
    /**
     * Guarda la información del servidor.
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
     * Marca la conexión como estable (resetea contador de intentos).
     */
    public void markStable() {
        var config = ClientReflexConfig.getConfig().antiDisconnect;
        long now = System.currentTimeMillis();
        
        if (now - lastStableTime > config.stableResetSeconds * 1000L) {
            currentAttempt = 0;
            lastStableTime = now;
            ClientReflexMod.LOGGER.debug("Conexión estable, reset de intentos de AntiDisconnect");
        }
    }
    
    /**
     * Detecta un error recuperable e inicia el proceso de recuperación.
     * 
     * @param error El mensaje de error o excepción
     */
    public void handleRecoverableError(String error) {
        if (!RecoverableErrorClassifier.isRecoverable(error)) {
            return;
        }
        
        var config = ClientReflexConfig.getConfig();
        if (!config.antiDisconnect.enabled) {
            transitionTo(AntiDisconnectState.DISABLED);
            ChatNotifier.getInstance().notify("antidisconnect_disabled", 
                "AntiDisconnect está deshabilitado en la configuración.");
            return;
        }
        
        AntiDisconnectState current = state.get();
        if (current == AntiDisconnectState.RECONNECTING || 
            current == AntiDisconnectState.RECOVERING ||
            current == AntiDisconnectState.COOLDOWN) {
            // Ya estamos en proceso de recuperación
            return;
        }
        
        // Iniciar nueva instancia
        currentInstanceId = ChatNotifier.getInstance().generateInstanceId();
        currentAttempt++;
        
        var antiConfig = config.antiDisconnect;
        if (currentAttempt > antiConfig.maxAttempts) {
            ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "max_attempts",
                "Máximo de intentos alcanzado. AntiDisconnect se detiene para evitar bucle.");
            transitionTo(AntiDisconnectState.IDLE);
            currentAttempt = 0;
            return;
        }
        
        // Notificar detección
        ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "detection_" + currentInstanceId,
            "Se detectó un problema recuperable: " + (error != null ? error : "desconocido") + ". Iniciando recuperación…");
        
        // Intentar recuperación sin desconectar primero
        attemptRecovery();
    }
    
    /**
     * Maneja una excepción recuperable.
     */
    public void handleRecoverableException(Throwable exception) {
        String error = exception != null ? exception.getClass().getSimpleName() + ": " + exception.getMessage() : "Excepción desconocida";
        handleRecoverableError(error);
    }
    
    /**
     * Intenta recuperar la conexión sin desconectar.
     */
    private void attemptRecovery() {
        transitionTo(AntiDisconnectState.RECOVERING);
        
        ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "recovery_" + currentInstanceId,
            "Intentando estabilizar la conexión sin reconectar…");
        
        // Esperar un poco antes de intentar reconexión
        var config = ClientReflexConfig.getConfig().antiDisconnect;
        long delay = calculateBackoffDelay();
        
        cancelCurrentTask();
        currentTask = scheduler.schedule(() -> {
            MinecraftClient.getInstance().execute(() -> {
                if (state.get() == AntiDisconnectState.RECOVERING) {
                    // Si aún estamos en recovery, pasar a reconexión
                    attemptReconnect();
                }
            });
        }, delay, TimeUnit.MILLISECONDS);
        
        ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "recovery_scheduled_" + currentInstanceId,
            "Reconexión programada en " + (delay / 1000) + "s (intento " + currentAttempt + "/" + config.maxAttempts + ").");
    }
    
    /**
     * Intenta reconectar al servidor.
     */
    private void attemptReconnect() {
        transitionTo(AntiDisconnectState.RECONNECTING);
        
        var config = ClientReflexConfig.getConfig().antiDisconnect;
        ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "reconnecting_" + currentInstanceId,
            "Reconectando al servidor… (intento " + currentAttempt + "/" + config.maxAttempts + ")");
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            transitionTo(AntiDisconnectState.IDLE);
            return;
        }
        
        if (client.world != null) {
            // Ya estamos conectados
            ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "reconnect_success_" + currentInstanceId,
                "Reconexión exitosa. Restableciendo estado y métricas.");
            transitionTo(AntiDisconnectState.IDLE);
            currentAttempt = 0;
            markStable();
            return;
        }
        
        try {
            if (lastServerInfo != null) {
                ClientReflexMod.LOGGER.info("Reconectando a servidor: {}", lastServerInfo.name);
                client.setScreen(new MultiplayerScreen(new TitleScreen()));
            } else if (lastServerAddress != null) {
                ClientReflexMod.LOGGER.info("Reconectando a: {}:{}", lastServerAddress, lastServerPort);
                client.setScreen(new MultiplayerScreen(new TitleScreen()));
            } else {
                ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "reconnect_failed_" + currentInstanceId,
                    "Reconexión fallida: No hay información del servidor guardada.");
                transitionTo(AntiDisconnectState.IDLE);
            }
        } catch (Exception e) {
            ClientReflexMod.LOGGER.error("Error al intentar reconectar: ", e);
            handleReconnectFailure(e.getMessage());
        }
    }
    
    /**
     * Maneja un fallo de reconexión.
     */
    private void handleReconnectFailure(String error) {
        var config = ClientReflexConfig.getConfig().antiDisconnect;
        
        ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "reconnect_failed_" + currentInstanceId,
            "Reconexión fallida: " + (error != null ? error : "desconocido") + ". Próximo intento en " + 
            (calculateBackoffDelay() / 1000) + "s.");
        
        transitionTo(AntiDisconnectState.COOLDOWN);
        
        long delay = calculateBackoffDelay();
        cancelCurrentTask();
        currentTask = scheduler.schedule(() -> {
            MinecraftClient.getInstance().execute(() -> {
                if (state.get() == AntiDisconnectState.COOLDOWN) {
                    currentAttempt++;
                    if (currentAttempt > config.maxAttempts) {
                        ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "max_attempts",
                            "Máximo de intentos alcanzado. AntiDisconnect se detiene para evitar bucle.");
                        transitionTo(AntiDisconnectState.IDLE);
                        currentAttempt = 0;
                    } else {
                        attemptReconnect();
                    }
                }
            });
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Calcula el delay de backoff exponencial.
     */
    private long calculateBackoffDelay() {
        var config = ClientReflexConfig.getConfig().antiDisconnect;
        long baseDelay = config.baseDelayMs;
        long maxDelay = config.maxDelayMs;
        double multiplier = config.multiplier;
        int jitterPct = config.jitterPct;
        
        // Backoff exponencial: baseDelay * (multiplier ^ (attempt - 1))
        long delay = (long) (baseDelay * Math.pow(multiplier, currentAttempt - 1));
        delay = Math.min(delay, maxDelay);
        
        // Añadir jitter aleatorio
        if (jitterPct > 0) {
            long jitter = (long) (delay * jitterPct / 100.0 * (random.nextDouble() * 2 - 1));
            delay += jitter;
        }
        
        return Math.max(delay, baseDelay);
    }
    
    /**
     * Cancela la reconexión manualmente.
     */
    public void cancel() {
        if (state.get() == AntiDisconnectState.IDLE || state.get() == AntiDisconnectState.DISABLED) {
            return;
        }
        
        cancelCurrentTask();
        transitionTo(AntiDisconnectState.STOPPED_BY_USER);
        
        if (currentInstanceId != null) {
            ChatNotifier.getInstance().notifyWithInstance(currentInstanceId, "cancel_" + currentInstanceId,
                "Reconexión cancelada por el usuario.");
        }
        
        currentAttempt = 0;
        currentInstanceId = null;
    }
    
    /**
     * Inicia el monitoreo.
     */
    public void startMonitoring() {
        var config = ClientReflexConfig.getConfig();
        if (!config.antiDisconnect.enabled) {
            transitionTo(AntiDisconnectState.DISABLED);
            return;
        }
        
        if (state.get() == AntiDisconnectState.IDLE || state.get() == AntiDisconnectState.DISABLED) {
            transitionTo(AntiDisconnectState.MONITORING);
            ChatNotifier.getInstance().notify("monitoring_start",
                "Monitoreo activo: detectando lag/timeouts para evitar desconexión.");
        }
    }
    
    /**
     * Detiene el monitoreo.
     */
    public void stopMonitoring() {
        cancelCurrentTask();
        transitionTo(AntiDisconnectState.IDLE);
        currentAttempt = 0;
        currentInstanceId = null;
    }
    
    /**
     * Transición de estado.
     */
    private void transitionTo(AntiDisconnectState newState) {
        AntiDisconnectState oldState = state.getAndSet(newState);
        if (oldState != newState) {
            ClientReflexMod.LOGGER.debug("AntiDisconnect: {} -> {}", oldState, newState);
        }
    }
    
    /**
     * Cancela la tarea actual si existe.
     */
    private void cancelCurrentTask() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(false);
            currentTask = null;
        }
    }
    
    /**
     * Limpia recursos al cerrar.
     */
    public void shutdown() {
        cancelCurrentTask();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public String getCurrentInstanceId() {
        return currentInstanceId;
    }
    
    public int getCurrentAttempt() {
        return currentAttempt;
    }
}

