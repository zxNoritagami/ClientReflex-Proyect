package dev.zxnoragami.clientreflex.ping;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Interpolador de movimiento para entidades remotas.
 * 
 * Suaviza el movimiento de otros jugadores y entidades cuando el servidor envía
 * updates a saltos debido al jitter de red. Interpola visualmente entre snapshots
 * para que el movimiento se vea más fluido.
 * 
 * IMPORTANTE: Solo interpola visualmente, no modifica la posición real de las
 * entidades. El servidor sigue siendo la fuente de verdad.
 */
public class RemoteEntityInterpolator {
    private static RemoteEntityInterpolator instance;
    
    /**
     * Snapshot de posición de una entidad en un momento dado.
     */
    private static class PositionSnapshot {
        final Vec3d position;
        @SuppressWarnings("unused")
        final float yaw; // Reservado para futura interpolación de rotación
        @SuppressWarnings("unused")
        final float pitch; // Reservado para futura interpolación de rotación
        final long timestamp;
        
        PositionSnapshot(Vec3d position, float yaw, float pitch, long timestamp) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
        }
    }
    
    private final Map<Integer, PositionSnapshot> lastSnapshots = new HashMap<>();
    private final Map<Integer, PositionSnapshot> currentSnapshots = new HashMap<>();
    private boolean enabled = false;
    private double interpolationWindowMs = 50.0; // Ventana de interpolación en ms
    
    private RemoteEntityInterpolator() {
    }
    
    public static RemoteEntityInterpolator getInstance() {
        if (instance == null) {
            instance = new RemoteEntityInterpolator();
        }
        return instance;
    }
    
    /**
     * Registra una actualización de posición de una entidad.
     * Se debe llamar cuando se recibe un paquete de actualización de entidad.
     */
    public void onEntityUpdate(Entity entity) {
        if (!enabled || entity == null) {
            return;
        }
        
        int entityId = entity.getId();
        long now = System.currentTimeMillis();
        
        // Guardar snapshot anterior
        PositionSnapshot current = currentSnapshots.get(entityId);
        if (current != null) {
            lastSnapshots.put(entityId, current);
        }
        
        // Crear nuevo snapshot
        Vec3d pos = entity.getPos();
        float yaw = entity.getYaw();
        float pitch = entity.getPitch();
        currentSnapshots.put(entityId, new PositionSnapshot(pos, yaw, pitch, now));
    }
    
    /**
     * Obtiene la posición interpolada de una entidad.
     * Si no hay interpolación disponible, retorna la posición actual.
     */
    public Vec3d getInterpolatedPosition(Entity entity) {
        if (!enabled || entity == null) {
            return entity != null ? entity.getPos() : Vec3d.ZERO;
        }
        
        int entityId = entity.getId();
        PositionSnapshot last = lastSnapshots.get(entityId);
        PositionSnapshot current = currentSnapshots.get(entityId);
        
        if (last == null || current == null) {
            return entity.getPos();
        }
        
        long now = System.currentTimeMillis();
        long deltaTime = now - current.timestamp;
        
        // Si el snapshot es muy antiguo, usar la posición actual
        if (deltaTime > interpolationWindowMs * 2) {
            return entity.getPos();
        }
        
        // Interpolar entre el snapshot anterior y el actual
        double t = Math.min(1.0, deltaTime / interpolationWindowMs);
        
        double x = lerp(last.position.x, current.position.x, t);
        double y = lerp(last.position.y, current.position.y, t);
        double z = lerp(last.position.z, current.position.z, t);
        
        return new Vec3d(x, y, z);
    }
    
    /**
     * Interpolación lineal simple.
     */
    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    /**
     * Ajusta la ventana de interpolación según el ping.
     * Ping alto = ventana más grande para suavizar más.
     */
    public void adjustInterpolationWindow(PingMetrics metrics) {
        double avgPing = metrics.getAveragePingMs();
        
        // Ajustar ventana según ping: 50ms base + hasta 100ms adicionales
        interpolationWindowMs = 50.0 + Math.min(100.0, avgPing * 0.5);
    }
    
    /**
     * Limpia los snapshots de una entidad cuando se elimina.
     */
    public void onEntityRemoved(int entityId) {
        lastSnapshots.remove(entityId);
        currentSnapshots.remove(entityId);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}

