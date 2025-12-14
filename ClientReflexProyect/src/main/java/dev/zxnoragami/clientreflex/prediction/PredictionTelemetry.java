package dev.zxnoragami.clientreflex.prediction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Telemetría de predicciones.
 * 
 * Registra estadísticas sobre las predicciones realizadas:
 * - Cuántas se hicieron
 * - Cuántas fueron correctas
 * - Cuántas requirieron rollback
 */
public class PredictionTelemetry {
    private final ConcurrentHashMap<String, TypeStats> statsByType = new ConcurrentHashMap<>();
    
    /**
     * Registra una nueva predicción.
     */
    public void recordPrediction(String type) {
        statsByType.computeIfAbsent(type, k -> new TypeStats()).predictions.incrementAndGet();
    }
    
    /**
     * Registra una predicción exitosa (confirmada por el servidor).
     */
    public void recordSuccess(String type) {
        statsByType.computeIfAbsent(type, k -> new TypeStats()).successes.incrementAndGet();
    }
    
    /**
     * Registra un rollback (predicción incorrecta).
     */
    public void recordRollback(String type) {
        statsByType.computeIfAbsent(type, k -> new TypeStats()).rollbacks.incrementAndGet();
    }
    
    /**
     * Registra una predicción expirada (timeout sin confirmación).
     */
    public void recordExpired(String type) {
        statsByType.computeIfAbsent(type, k -> new TypeStats()).expired.incrementAndGet();
    }
    
    /**
     * Obtiene las estadísticas de un tipo específico.
     */
    public TypeStats getStats(String type) {
        return statsByType.getOrDefault(type, new TypeStats());
    }
    
    /**
     * Obtiene todas las estadísticas.
     */
    public ConcurrentHashMap<String, TypeStats> getAllStats() {
        return statsByType;
    }
    
    /**
     * Calcula el porcentaje de acierto para un tipo.
     */
    public double getSuccessRate(String type) {
        TypeStats stats = getStats(type);
        long total = stats.predictions.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) stats.successes.get() / total * 100.0;
    }
    
    /**
     * Resetea todas las estadísticas.
     */
    public void reset() {
        statsByType.clear();
    }
    
    /**
     * Estadísticas por tipo de predicción.
     */
    public static class TypeStats {
        public final AtomicLong predictions = new AtomicLong(0);
        public final AtomicLong successes = new AtomicLong(0);
        public final AtomicLong rollbacks = new AtomicLong(0);
        public final AtomicLong expired = new AtomicLong(0);
    }
}

