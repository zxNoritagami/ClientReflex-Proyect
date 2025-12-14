package dev.zxnoragami.clientreflex.ping;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Métricas avanzadas de ping y jitter.
 * 
 * Esta clase mide y modela el ping de forma precisa para permitir optimizaciones
 * basadas en la latencia real de la conexión.
 * 
 * IMPORTANTE: La "percepción de latencia" es diferente del ping real.
 * - Ping real: tiempo físico de ida y vuelta de los paquetes (no se puede reducir)
 * - Percepción de latencia: cómo se siente el delay en el juego (se puede optimizar)
 * 
 * Este módulo optimiza la percepción, no el ping físico.
 */
public class PingMetrics {
    private final Deque<Long> pingHistory = new ArrayDeque<>();
    private final int maxHistorySize;
    
    private long currentPingMs = 0;
    private long minPingMs = Long.MAX_VALUE;
    private long maxPingMs = 0;
    private double averagePingMs = 0.0;
    private double jitterMs = 0.0;
    
    private long lastKeepAliveSent = 0;
    private PingTrend trend = PingTrend.STABLE;
    
    /**
     * Tendencia del ping (subiendo, bajando, estable).
     */
    public enum PingTrend {
        DECREASING,  // Ping bajando (mejorando)
        STABLE,      // Ping estable
        INCREASING   // Ping subiendo (empeorando)
    }
    
    public PingMetrics(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Registra el envío de un keep-alive.
     * Se debe llamar cuando se envía un paquete keep-alive al servidor.
     */
    public void onKeepAliveSent() {
        lastKeepAliveSent = System.currentTimeMillis();
    }
    
    /**
     * Registra la recepción de una respuesta keep-alive.
     * Calcula el RTT (Round Trip Time) y actualiza las métricas.
     */
    public void onKeepAliveReceived() {
        if (lastKeepAliveSent == 0) {
            return; // No hay timestamp de envío
        }
        
        long rtt = System.currentTimeMillis() - lastKeepAliveSent;
        recordPing(rtt);
        lastKeepAliveSent = 0; // Reset
    }
    
    /**
     * Registra un valor de ping directamente.
     * Útil cuando se obtiene el ping de otras fuentes (ej. tab list).
     */
    public void recordPing(long pingMs) {
        if (pingMs < 0) {
            return; // Ping inválido
        }
        
        currentPingMs = pingMs;
        
        // Añadir al historial
        pingHistory.addLast(pingMs);
        while (pingHistory.size() > maxHistorySize) {
            pingHistory.removeFirst();
        }
        
        // Actualizar estadísticas
        updateStatistics();
        updateTrend();
    }
    
    /**
     * Actualiza las estadísticas (min, max, promedio, jitter).
     */
    private void updateStatistics() {
        if (pingHistory.isEmpty()) {
            return;
        }
        
        long sum = 0;
        minPingMs = Long.MAX_VALUE;
        maxPingMs = 0;
        
        for (Long ping : pingHistory) {
            sum += ping;
            if (ping < minPingMs) {
                minPingMs = ping;
            }
            if (ping > maxPingMs) {
                maxPingMs = ping;
            }
        }
        
        averagePingMs = (double) sum / pingHistory.size();
        
        // Calcular jitter (desviación absoluta media)
        if (pingHistory.size() >= 2) {
            double jitterSum = 0;
            Iterator<Long> it = pingHistory.iterator();
            long prev = it.next();
            int count = 0;
            
            while (it.hasNext()) {
                long current = it.next();
                jitterSum += Math.abs(current - prev);
                prev = current;
                count++;
            }
            
            jitterMs = count > 0 ? jitterSum / count : 0.0;
        } else {
            jitterMs = 0.0;
        }
    }
    
    /**
     * Actualiza la tendencia del ping comparando los últimos valores.
     */
    private void updateTrend() {
        if (pingHistory.size() < 10) {
            trend = PingTrend.STABLE;
            return;
        }
        
        // Comparar el promedio de los últimos 5 con los 5 anteriores
        long[] recent = new long[5];
        long[] previous = new long[5];
        
        Iterator<Long> it = pingHistory.descendingIterator();
        for (int i = 0; i < 5 && it.hasNext(); i++) {
            recent[i] = it.next();
        }
        for (int i = 0; i < 5 && it.hasNext(); i++) {
            previous[i] = it.next();
        }
        
        double recentAvg = 0, previousAvg = 0;
        for (long v : recent) recentAvg += v;
        for (long v : previous) previousAvg += v;
        recentAvg /= 5;
        previousAvg /= 5;
        
        double diff = recentAvg - previousAvg;
        if (diff > 10) {
            trend = PingTrend.INCREASING;
        } else if (diff < -10) {
            trend = PingTrend.DECREASING;
        } else {
            trend = PingTrend.STABLE;
        }
    }
    
    /**
     * Obtiene el ping actual en milisegundos.
     */
    public long getCurrentPingMs() {
        return currentPingMs;
    }
    
    /**
     * Obtiene el ping mínimo registrado.
     */
    public long getMinPingMs() {
        return minPingMs == Long.MAX_VALUE ? 0 : minPingMs;
    }
    
    /**
     * Obtiene el ping máximo registrado.
     */
    public long getMaxPingMs() {
        return maxPingMs;
    }
    
    /**
     * Obtiene el ping promedio.
     */
    public double getAveragePingMs() {
        return averagePingMs;
    }
    
    /**
     * Obtiene el jitter (variación del ping) en milisegundos.
     */
    public double getJitterMs() {
        return jitterMs;
    }
    
    /**
     * Obtiene la tendencia del ping.
     */
    public PingTrend getTrend() {
        return trend;
    }
    
    /**
     * Obtiene el historial completo de pings.
     */
    public Deque<Long> getHistory() {
        return new ArrayDeque<>(pingHistory);
    }
    
    /**
     * Limpia el historial y resetea las estadísticas.
     */
    public void reset() {
        pingHistory.clear();
        currentPingMs = 0;
        minPingMs = Long.MAX_VALUE;
        maxPingMs = 0;
        averagePingMs = 0.0;
        jitterMs = 0.0;
        trend = PingTrend.STABLE;
    }
}

