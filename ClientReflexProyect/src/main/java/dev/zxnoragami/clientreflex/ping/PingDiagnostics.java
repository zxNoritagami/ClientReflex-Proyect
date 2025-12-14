package dev.zxnoragami.clientreflex.ping;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Herramientas de diagnóstico para el módulo de optimización de ping.
 * 
 * Proporciona información detallada sobre el estado de la conexión,
 * configuración de Netty y métricas de rendimiento.
 */
public class PingDiagnostics {
    private static PingDiagnostics instance;
    
    private PingDiagnostics() {
    }
    
    public static PingDiagnostics getInstance() {
        if (instance == null) {
            instance = new PingDiagnostics();
        }
        return instance;
    }
    
    /**
     * Genera un reporte completo de diagnóstico.
     */
    public List<Text> generateDiagnosticReport(PingMetrics metrics, 
                                               PingProfileManager profileManager) {
        List<Text> report = new ArrayList<>();
        
        var config = ClientReflexConfig.getConfig();
        
        report.add(Text.literal("=== Diagnóstico de Ping ==="));
        report.add(Text.literal(""));
        
        // Métricas de ping
        report.add(Text.literal("Métricas de Ping:"));
        report.add(Text.literal(String.format("  Actual: %d ms", metrics.getCurrentPingMs())));
        report.add(Text.literal(String.format("  Promedio: %.1f ms", metrics.getAveragePingMs())));
        report.add(Text.literal(String.format("  Mínimo: %d ms", metrics.getMinPingMs())));
        report.add(Text.literal(String.format("  Máximo: %d ms", metrics.getMaxPingMs())));
        report.add(Text.literal(String.format("  Jitter: %.1f ms", metrics.getJitterMs())));
        
        PingMetrics.PingTrend trend = metrics.getTrend();
        String trendStr = trend == PingMetrics.PingTrend.INCREASING ? "↑ Subiendo" :
                         trend == PingMetrics.PingTrend.DECREASING ? "↓ Bajando" : "→ Estable";
        report.add(Text.literal(String.format("  Tendencia: %s", trendStr)));
        report.add(Text.literal(""));
        
        // Perfil actual
        PingProfileManager.PingRange range = profileManager.getCurrentRange();
        report.add(Text.literal("Perfil de Conexión: " + range));
        report.add(Text.literal(""));
        
        // Configuración de Netty
        report.add(Text.literal("Configuración de Red:"));
        report.add(Text.literal(String.format("  TCP_NODELAY: %s", 
            config.tcpNoDelayOverride ? "Habilitado" : "Por defecto")));
        report.add(Text.literal(String.format("  SO_KEEPALIVE: %s", 
            config.soKeepAliveOverride ? "Habilitado" : "Por defecto")));
        report.add(Text.literal(String.format("  PriorityWriteHandler: %s", 
            config.enablePriorityWriteHandler ? "Habilitado" : "Deshabilitado")));
        report.add(Text.literal(""));
        
        // Umbrales configurados
        report.add(Text.literal("Umbrales de Ping:"));
        report.add(Text.literal(String.format("  Bajo: < %d ms", config.pingLowThresholdMs)));
        report.add(Text.literal(String.format("  Medio: %d - %d ms", 
            config.pingLowThresholdMs, config.pingMediumThresholdMs)));
        report.add(Text.literal(String.format("  Alto: %d - %d ms", 
            config.pingMediumThresholdMs, config.pingHighThresholdMs)));
        report.add(Text.literal(String.format("  Crítico: > %d ms", config.pingHighThresholdMs)));
        report.add(Text.literal(""));
        
        // Sugerencias
        report.add(Text.literal("Sugerencias:"));
        if (metrics.getJitterMs() > 50 && metrics.getAveragePingMs() < 100) {
            report.add(Text.literal("  ⚠ Jitter alto con ping bajo: revisa tu conexión Wi-Fi"));
        }
        if (metrics.getAveragePingMs() > 200) {
            report.add(Text.literal("  ⚠ Ping alto: considera usar un servidor más cercano"));
        }
        if (metrics.getJitterMs() < 10 && metrics.getAveragePingMs() < 50) {
            report.add(Text.literal("  ✓ Conexión excelente"));
        }
        
        return report;
    }
    
    /**
     * Obtiene estadísticas de paquetes (simplificado).
     * En una implementación completa, esto se integraría con el NetworkMonitor.
     */
    public String getPacketStats() {
        // Esto se puede integrar con NetworkMonitor para obtener estadísticas reales
        return "Estadísticas de paquetes no disponibles en esta versión";
    }
}

