package dev.zxnoragami.clientreflex.ui;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.net.NetworkMonitor;
import dev.zxnoragami.clientreflex.ping.PingMetrics;
import dev.zxnoragami.clientreflex.prediction.PredictionManager;
import dev.zxnoragami.clientreflex.ClientReflexClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * HUD que muestra información de red en tiempo real.
 * Se puede mostrar/ocultar con una tecla configurable.
 */
public class NetworkHUD {
    private final NetworkMonitor monitor;
    private boolean enabled = false;

    public NetworkHUD(NetworkMonitor monitor) {
        this.monitor = monitor;
        this.enabled = ClientReflexConfig.getConfig().hudEnabled;
    }

    /**
     * Alterna la visibilidad del HUD.
     */
    public void toggle() {
        enabled = !enabled;
        ClientReflexConfig.getConfig().hudEnabled = enabled;
        ClientReflexConfig.save();
    }

    /**
     * Renderiza el HUD si está habilitado.
     */
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.getNetworkHandler() == null) {
            return;
        }

        int x = 10;
        int y = 10;
        String position = ClientReflexConfig.getConfig().hudPosition;
        
        // Calcular posición según la configuración
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        if (position.contains("right")) {
            x = screenWidth - 200;
        }
        if (position.contains("bottom")) {
            y = screenHeight - 100;
        }

        // Renderizar información de red
        renderNetworkInfo(context, x, y);
    }

    /**
     * Renderiza la información de red en la posición especificada.
     */
    private void renderNetworkInfo(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        var textRenderer = client.textRenderer;
        
        int lineHeight = 10;
        int currentY = y;
        int color = 0xFFFFFF; // Blanco

        // Ping actual
        long currentPing = monitor.getCurrentPing();
        String pingText = String.format("Ping: %d ms", currentPing);
        context.drawTextWithShadow(textRenderer, pingText, x, currentY, color);
        currentY += lineHeight;

        // Ping promedio
        double avgPing = monitor.getAveragePing();
        String avgText = String.format("Ping promedio: %.1f ms", avgPing);
        context.drawTextWithShadow(textRenderer, avgText, x, currentY, color);
        currentY += lineHeight;

        // Ping máximo
        long maxPing = monitor.getMaxPing();
        String maxText = String.format("Ping máximo: %d ms", maxPing);
        context.drawTextWithShadow(textRenderer, maxText, x, currentY, color);
        currentY += lineHeight;

        // Jitter
        double jitter = monitor.getJitter();
        String jitterText = String.format("Jitter: %.1f ms", jitter);
        context.drawTextWithShadow(textRenderer, jitterText, x, currentY, color);
        currentY += lineHeight;

        // Tiempo desde último paquete
        long timeSinceLastPacket = monitor.getTimeSinceLastPacket();
        String timeText = String.format("Último paquete: %d ms", timeSinceLastPacket);
        context.drawTextWithShadow(textRenderer, timeText, x, currentY, color);
        currentY += lineHeight;

        // Paquetes recibidos en ventana
        int packetsInWindow = monitor.getPacketsReceivedInWindow();
        String packetsText = String.format("Paquetes/10s: %d", packetsInWindow);
        context.drawTextWithShadow(textRenderer, packetsText, x, currentY, color);
        currentY += lineHeight;

        // Información avanzada de ping (si está disponible)
        PingMetrics pingMetrics = ClientReflexClient.getPingMetrics();
        if (pingMetrics != null && pingMetrics.getCurrentPingMs() > 0) {
            PingMetrics.PingTrend trend = pingMetrics.getTrend();
            String trendStr = trend == PingMetrics.PingTrend.INCREASING ? "↑" :
                             trend == PingMetrics.PingTrend.DECREASING ? "↓" : "→";
            String trendText = String.format("Tendencia: %s", trendStr);
            context.drawTextWithShadow(textRenderer, trendText, x, currentY, color);
            currentY += lineHeight;
        }

        // Estadísticas de predicción (si está habilitado)
        if (ClientReflexConfig.getConfig().showPredictionStats) {
            PredictionManager predictionManager = PredictionManager.getInstance();
            var telemetry = predictionManager.getTelemetry();
            var allStats = telemetry.getAllStats();
            
            if (!allStats.isEmpty()) {
                // Calcular totales
                long totalPredictions = 0;
                long totalSuccesses = 0;
                for (var stats : allStats.values()) {
                    totalPredictions += stats.predictions.get();
                    totalSuccesses += stats.successes.get();
                }
                
                if (totalPredictions > 0) {
                    double successRate = (double) totalSuccesses / totalPredictions * 100.0;
                    String predictionText = String.format("Predicciones: %d (%.1f%% acierto)", 
                        totalPredictions, successRate);
                    context.drawTextWithShadow(textRenderer, predictionText, x, currentY, color);
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}

