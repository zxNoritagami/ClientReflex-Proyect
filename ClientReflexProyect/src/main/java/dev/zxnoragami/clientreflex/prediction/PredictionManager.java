package dev.zxnoragami.clientreflex.prediction;

import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor central de predicciones.
 * 
 * Registra y coordina todos los módulos de predicción, distribuyendo eventos
 * y manteniendo un registro de entidades y bloques predichos como destruidos.
 * 
 * Este sistema permite que el cliente adelante visualmente la eliminación de
 * entidades para reducir el delay percibido en alto ping.
 */
public class PredictionManager {
    private static PredictionManager instance;
    private final List<PredictionModule> modules = new ArrayList<>();
    private final Map<Integer, PredictedEntity> predictedEntities = new ConcurrentHashMap<>();
    private final Map<BlockPos, PredictedBlock> predictedBlocks = new ConcurrentHashMap<>();
    private final PredictionTelemetry telemetry = new PredictionTelemetry();
    
    private PredictionManager() {
    }
    
    public static PredictionManager getInstance() {
        if (instance == null) {
            instance = new PredictionManager();
        }
        return instance;
    }
    
    /**
     * Registra un módulo de predicción.
     */
    public void registerModule(PredictionModule module) {
        if (!modules.contains(module)) {
            modules.add(module);
            ClientReflexMod.LOGGER.info("Módulo de predicción registrado: " + module.getName());
        }
    }
    
    /**
     * Notifica a todos los módulos sobre una acción local.
     */
    public void onLocalAction(LocalActionContext ctx) {
        for (PredictionModule module : modules) {
            if (module.isEnabled()) {
                try {
                    module.onLocalAction(ctx);
                } catch (Exception e) {
                    ClientReflexMod.LOGGER.error("Error en módulo " + module.getName() + ": ", e);
                }
            }
        }
    }
    
    /**
     * Notifica a todos los módulos sobre un paquete del servidor.
     */
    public void onServerPacket(ServerPacketContext ctx) {
        for (PredictionModule module : modules) {
            if (module.isEnabled()) {
                try {
                    module.onServerPacket(ctx);
                } catch (Exception e) {
                    ClientReflexMod.LOGGER.error("Error en módulo " + module.getName() + ": ", e);
                }
            }
        }
    }
    
    /**
     * Actualiza todos los módulos cada tick.
     */
    public void tick() {
        // Limpiar predicciones expiradas
        long now = System.currentTimeMillis();
        predictedEntities.entrySet().removeIf(entry -> {
            if (now - entry.getValue().timestamp > entry.getValue().timeoutMs) {
                telemetry.recordExpired(entry.getValue().type);
                return true;
            }
            return false;
        });
        
        predictedBlocks.entrySet().removeIf(entry -> {
            if (now - entry.getValue().timestamp > entry.getValue().timeoutMs) {
                return true;
            }
            return false;
        });
        
        // Tick de módulos
        for (PredictionModule module : modules) {
            if (module.isEnabled()) {
                try {
                    module.tick();
                } catch (Exception e) {
                    ClientReflexMod.LOGGER.error("Error en módulo " + module.getName() + ": ", e);
                }
            }
        }
    }
    
    /**
     * Marca una entidad como predicha como destruida.
     */
    public void predictEntityDestroyed(int entityId, String type, long timeoutMs) {
        predictedEntities.put(entityId, new PredictedEntity(entityId, type, System.currentTimeMillis(), timeoutMs));
        telemetry.recordPrediction(type);
    }
    
    /**
     * Verifica si una entidad está marcada como predicha como destruida.
     */
    public boolean isEntityPredictedDestroyed(int entityId) {
        return predictedEntities.containsKey(entityId);
    }
    
    /**
     * Confirma que una entidad fue realmente destruida (reconciliación exitosa).
     */
    public void confirmEntityDestroyed(int entityId) {
        PredictedEntity predicted = predictedEntities.remove(entityId);
        if (predicted != null) {
            telemetry.recordSuccess(predicted.type);
        }
    }
    
    /**
     * Revierte una predicción (rollback) porque el servidor indica que la entidad sigue viva.
     */
    public void rollbackEntity(int entityId) {
        PredictedEntity predicted = predictedEntities.remove(entityId);
        if (predicted != null) {
            telemetry.recordRollback(predicted.type);
        }
    }
    
    /**
     * Marca un bloque como predicho como destruido.
     */
    public void predictBlockDestroyed(BlockPos pos, String type, long timeoutMs) {
        predictedBlocks.put(pos, new PredictedBlock(pos, type, System.currentTimeMillis(), timeoutMs));
    }
    
    /**
     * Verifica si un bloque está marcado como predicho como destruido.
     */
    public boolean isBlockPredictedDestroyed(BlockPos pos) {
        return predictedBlocks.containsKey(pos);
    }
    
    /**
     * Confirma que un bloque fue realmente destruido.
     */
    public void confirmBlockDestroyed(BlockPos pos) {
        predictedBlocks.remove(pos);
    }
    
    /**
     * Revierte una predicción de bloque.
     */
    public void rollbackBlock(BlockPos pos) {
        predictedBlocks.remove(pos);
    }
    
    /**
     * Obtiene la telemetría de predicciones.
     */
    public PredictionTelemetry getTelemetry() {
        return telemetry;
    }
    
    /**
     * Limpia todas las predicciones activas.
     */
    public void clearAll() {
        predictedEntities.clear();
        predictedBlocks.clear();
    }
    
    // Clases internas para almacenar información de predicciones
    private static class PredictedEntity {
        @SuppressWarnings("unused")
        final int entityId; // Almacenado para debugging/futuras funcionalidades
        final String type;
        final long timestamp;
        final long timeoutMs;
        
        PredictedEntity(int entityId, String type, long timestamp, long timeoutMs) {
            this.entityId = entityId;
            this.type = type;
            this.timestamp = timestamp;
            this.timeoutMs = timeoutMs;
        }
    }
    
    private static class PredictedBlock {
        @SuppressWarnings("unused")
        final BlockPos position; // Almacenado para debugging/futuras funcionalidades
        @SuppressWarnings("unused")
        final String type; // Almacenado para debugging/futuras funcionalidades
        final long timestamp;
        final long timeoutMs;
        
        PredictedBlock(BlockPos position, String type, long timestamp, long timeoutMs) {
            this.position = position;
            this.type = type;
            this.timestamp = timestamp;
            this.timeoutMs = timeoutMs;
        }
    }
}

