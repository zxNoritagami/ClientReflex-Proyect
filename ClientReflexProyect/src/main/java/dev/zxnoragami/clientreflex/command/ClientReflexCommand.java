package dev.zxnoragami.clientreflex.command;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.ClientReflexClient;
import dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectManager;
import dev.zxnoragami.clientreflex.net.PriorityPolicy;
import dev.zxnoragami.clientreflex.ping.PingDiagnostics;
import dev.zxnoragami.clientreflex.ping.PingMetrics;
import dev.zxnoragami.clientreflex.ping.PingProfileManager;
import dev.zxnoragami.clientreflex.prediction.PredictionManager;
import dev.zxnoragami.clientreflex.prediction.PredictionTelemetry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Comando del cliente para gestionar la configuración de ClientReflex.
 * Permite recargar la configuración y ajustar algunos valores en tiempo real.
 */
public class ClientReflexCommand {
    
    /**
     * Registra los comandos del mod.
     */
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("clientreflex")
            .then(ClientCommandManager.literal("reload")
                .executes(ClientReflexCommand::reloadConfig))
            .then(ClientCommandManager.literal("cancelreconnect")
                .executes(ClientReflexCommand::cancelReconnect))
            .then(ClientCommandManager.literal("timeout")
                .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(10, 180))
                    .executes(ClientReflexCommand::setTimeout)))
            .then(ClientCommandManager.literal("info")
                .executes(ClientReflexCommand::showInfo))
            .then(ClientCommandManager.literal("predictionstats")
                .executes(ClientReflexCommand::showPredictionStats))
            .then(ClientCommandManager.literal("pingdiag")
                .executes(ClientReflexCommand::showPingDiagnostics))
        );
    }
    
    /**
     * Recarga la configuración desde el archivo.
     */
    private static int reloadConfig(CommandContext<FabricClientCommandSource> context) {
        ClientReflexConfig.reload();
        
        // Actualizar políticas y handlers en tiempo de ejecución
        // Nota: PriorityWriteHandler se actualizará automáticamente cuando se detecte en el pipeline
        // La política se creará nueva en la próxima conexión
        
        context.getSource().sendFeedback(Text.translatable("clientreflex.command.reload.success"));
        return 1;
    }
    
    /**
     * Cancela la reconexión automática en curso.
     */
    private static int cancelReconnect(CommandContext<FabricClientCommandSource> context) {
        AntiDisconnectManager manager = AntiDisconnectManager.getInstance();
        if (manager.getState() == dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectState.IDLE ||
            manager.getState() == dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectState.DISABLED) {
            context.getSource().sendFeedback(Text.translatable("clientreflex.command.cancelreconnect.notactive"));
            return 0;
        }
        
        manager.cancel();
        context.getSource().sendFeedback(Text.translatable("clientreflex.command.cancelreconnect.success"));
        return 1;
    }
    
    /**
     * Establece el timeout de lectura.
     */
    private static int setTimeout(CommandContext<FabricClientCommandSource> context) {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        ClientReflexConfig.getConfig().readTimeoutSeconds = seconds;
        ClientReflexConfig.save();
        context.getSource().sendFeedback(
            Text.translatable("clientreflex.command.timeout.set", seconds)
        );
        return 1;
    }
    
    /**
     * Muestra información sobre la configuración actual.
     */
    private static int showInfo(CommandContext<FabricClientCommandSource> context) {
        var config = ClientReflexConfig.getConfig();
        context.getSource().sendFeedback(
            Text.translatable("clientreflex.command.info.timeout", config.readTimeoutSeconds)
        );
        context.getSource().sendFeedback(
            Text.translatable("clientreflex.command.info.autoreconnect", 
                config.autoReconnectEnabled ? "ON" : "OFF")
        );
        context.getSource().sendFeedback(
            Text.translatable("clientreflex.command.info.attempts", 
                config.maxReconnectAttempts)
        );
        return 1;
    }
    
    /**
     * Muestra estadísticas de predicción.
     */
    private static int showPredictionStats(CommandContext<FabricClientCommandSource> context) {
        PredictionManager manager = PredictionManager.getInstance();
        PredictionTelemetry telemetry = manager.getTelemetry();
        var allStats = telemetry.getAllStats();
        
        if (allStats.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("No hay estadísticas de predicción disponibles"));
            return 1;
        }
        
        context.getSource().sendFeedback(Text.literal("=== Estadísticas de Predicción ==="));
        
        for (var entry : allStats.entrySet()) {
            String type = entry.getKey();
            PredictionTelemetry.TypeStats stats = entry.getValue();
            
            long predictions = stats.predictions.get();
            long successes = stats.successes.get();
            long rollbacks = stats.rollbacks.get();
            long expired = stats.expired.get();
            
            double successRate = predictions > 0 ? (double) successes / predictions * 100.0 : 0.0;
            
            context.getSource().sendFeedback(Text.literal(
                String.format("%s: %d predicciones, %.1f%% acierto (%d éxitos, %d rollbacks, %d expiradas)",
                    type, predictions, successRate, successes, rollbacks, expired)
            ));
        }
        
        return 1;
    }
    
    /**
     * Muestra diagnóstico completo de ping.
     */
    private static int showPingDiagnostics(CommandContext<FabricClientCommandSource> context) {
        // Obtener instancias (se inicializan en ClientReflexClient)
        PingMetrics metrics = ClientReflexClient.getPingMetrics();
        PingProfileManager profileManager = PingProfileManager.getInstance();
        
        if (metrics == null) {
            context.getSource().sendFeedback(Text.literal("Sistema de ping no inicializado"));
            return 1;
        }
        
        PingDiagnostics diagnostics = PingDiagnostics.getInstance();
        List<Text> report = diagnostics.generateDiagnosticReport(metrics, profileManager);
        
        for (Text line : report) {
            context.getSource().sendFeedback(line);
        }
        
        return 1;
    }
}

