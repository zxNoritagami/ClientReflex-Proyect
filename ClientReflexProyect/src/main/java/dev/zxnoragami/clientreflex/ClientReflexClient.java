package dev.zxnoragami.clientreflex;

import dev.zxnoragami.clientreflex.client.WeakConnectionManager;
import dev.zxnoragami.clientreflex.command.ClientReflexCommand;
import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.net.NetworkMonitor;
import dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectManager;
import dev.zxnoragami.clientreflex.ping.*;
import dev.zxnoragami.clientreflex.prediction.PredictionManager;
import dev.zxnoragami.clientreflex.prediction.modules.*;
import dev.zxnoragami.clientreflex.ui.NetworkHUD;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Cliente del mod ClientReflex.
 * Se inicializa cuando el mod se carga en el cliente.
 * Aquí se registran los keybindings y los eventos del cliente.
 */
public class ClientReflexClient implements ClientModInitializer {
    private static KeyBinding hudToggleKey;
    private static NetworkHUD networkHUD;
    private static NetworkMonitor networkMonitor;
    private static PingMetrics pingMetrics;
    private static PingProfileManager pingProfileManager;
    private static InputSmoothingManager inputSmoothingManager;
    private static RemoteEntityInterpolator remoteEntityInterpolator;
    
    // Contador de ticks para actualización de perfiles de ping
    // Se actualiza cada 100 ticks (~5 segundos) independientemente del worldTime
    private static int ticksSinceLastProfileUpdate = 0;

    @Override
    public void onInitializeClient() {
        // #region agent log
        try {
            String logPath = System.getProperty("user.dir") + "\\.cursor\\debug.log";
            String logEntry = String.format("{\"id\":\"log_client_init_%d\",\"timestamp\":%d,\"location\":\"ClientReflexClient.java:39\",\"message\":\"Client initialization started\",\"data\":{},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n", System.currentTimeMillis(), System.currentTimeMillis());
            Files.write(Paths.get(logPath), logEntry.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion
        
        ClientReflexMod.LOGGER.info("ClientReflex cliente inicializado");

        // Inicializar AntiDisconnectManager
        AntiDisconnectManager antiDisconnectManager = AntiDisconnectManager.getInstance();
        if (ClientReflexConfig.getConfig().antiDisconnect.enabled) {
            antiDisconnectManager.startMonitoring();
        }

        // Inicializar el monitor de red
        networkMonitor = new NetworkMonitor();
        
        // Inicializar métricas de ping
        var config = ClientReflexConfig.getConfig();
        pingMetrics = new PingMetrics(config.pingMetricsHistorySize);
        
        // Inicializar gestores de optimización de ping
        pingProfileManager = PingProfileManager.getInstance();
        inputSmoothingManager = InputSmoothingManager.getInstance();
        remoteEntityInterpolator = RemoteEntityInterpolator.getInstance();
        remoteEntityInterpolator.setEnabled(config.enableRemoteEntityInterpolation);
        
        // Inicializar el HUD
        networkHUD = new NetworkHUD(networkMonitor);
        
        // Inicializar el sistema de predicción
        PredictionManager predictionManager = PredictionManager.getInstance();
        predictionManager.registerModule(new CrystalPredictionModule(predictionManager));
        predictionManager.registerModule(new BedPredictionModule(predictionManager));
        predictionManager.registerModule(new AnchorPredictionModule(predictionManager));
        predictionManager.registerModule(new TntMinecartPredictionModule(predictionManager));

        // Registrar el keybinding para mostrar/ocultar el HUD
        hudToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.clientreflex.togglehud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.clientreflex.general"
        ));

        // Registrar el evento de tick del cliente para manejar el HUD y el modo conexión débil
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Toggle del HUD
            while (hudToggleKey.wasPressed()) {
                networkHUD.toggle();
            }

            // Actualizar el monitor de red
            if (client.world != null && client.getNetworkHandler() != null) {
                networkMonitor.update();
                
                // Marcar conexión como estable para AntiDisconnect
                AntiDisconnectManager.getInstance().markStable();
                
                // Actualizar métricas de ping desde el monitor de red
                long currentPing = networkMonitor.getCurrentPing();
                if (currentPing > 0) {
                    pingMetrics.recordPing(currentPing);
                }
                
                // Actualizar el gestor de modo conexión débil
                WeakConnectionManager.getInstance().update();
                
                // Actualizar perfiles de ping cada 100 ticks (~5 segundos)
                // Usamos un contador propio en lugar de world.getTime() para garantizar
                // actualizaciones regulares independientemente de cambios en el mundo
                ticksSinceLastProfileUpdate++;
                if (ticksSinceLastProfileUpdate >= 100) {
                    ticksSinceLastProfileUpdate = 0;
                    pingProfileManager.update(pingMetrics);
                }
                
                // Actualizar interpolador de entidades remotas
                var pingConfig = ClientReflexConfig.getConfig();
                if (pingConfig.enableRemoteEntityInterpolation) {
                    remoteEntityInterpolator.adjustInterpolationWindow(pingMetrics);
                }
                
                // Actualizar suavizado de input
                if (pingConfig.enableInputSmoothing) {
                    inputSmoothingManager.tick();
                }
                
                // Actualizar el sistema de predicción
                PredictionManager.getInstance().tick();
            }
        });

        // Registrar el renderizado del HUD
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            networkHUD.render(drawContext, tickCounter);
        });

        // Registrar comandos del cliente
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ClientReflexCommand.register(dispatcher);
        });
    }

    public static NetworkHUD getNetworkHUD() {
        return networkHUD;
    }

    public static NetworkMonitor getNetworkMonitor() {
        return networkMonitor;
    }
    
    public static PingMetrics getPingMetrics() {
        return pingMetrics;
    }
    
    public static PingProfileManager getPingProfileManager() {
        return pingProfileManager;
    }
}
