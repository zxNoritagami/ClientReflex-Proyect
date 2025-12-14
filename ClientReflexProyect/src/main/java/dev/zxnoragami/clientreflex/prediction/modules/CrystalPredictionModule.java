package dev.zxnoragami.clientreflex.prediction.modules;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.prediction.*;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

/**
 * Módulo de predicción para End Crystals.
 * 
 * Este módulo replica la funcionalidad de Marlow's Crystal Optimizer:
 * cuando el jugador detona una crystal (por ataque o explosión en cadena),
 * la marca inmediatamente como destruida en el cliente para reducir el delay
 * percibido en alto ping.
 * 
 * IMPORTANTE: No modifica paquetes enviados al servidor. Solo adelanta la
 * eliminación visual de la entidad en el cliente.
 */
public class CrystalPredictionModule implements PredictionModule {
    private final PredictionManager manager;
    private final String type = "crystal";
    private long predictionTimeoutMs = 800; // 800ms por defecto
    
    public CrystalPredictionModule(PredictionManager manager) {
        this.manager = manager;
    }
    
    @Override
    public void onLocalAction(LocalActionContext ctx) {
        if (!isEnabled()) {
            return;
        }
        
        // Solo procesar acciones relacionadas con crystals
        if (ctx.getActionType() == LocalActionContext.ActionType.CRYSTAL_BREAK) {
            Entity target = ctx.getTargetEntity();
            if (target instanceof EndCrystalEntity) {
                int entityId = target.getId();
                
                // Marcar la crystal como predicha como destruida
                manager.predictEntityDestroyed(entityId, type, predictionTimeoutMs);
                
                // Remover la entidad del mundo cliente inmediatamente
                // Esto se hace a través de un mixin que consulta al PredictionManager
                ClientReflexMod.LOGGER.debug("Crystal predicha como destruida: ID " + entityId);
            }
        }
    }
    
    @Override
    public void onServerPacket(ServerPacketContext ctx) {
        if (!isEnabled()) {
            return;
        }
        
        // Reconciliar con paquetes del servidor
        if (ctx.getPacketType() == ServerPacketContext.PacketType.ENTITY_DESTROY) {
            // El servidor confirma que la entidad fue destruida
            if (ctx.getEntity() instanceof EndCrystalEntity) {
                int entityId = ctx.getEntity().getId();
                if (manager.isEntityPredictedDestroyed(entityId)) {
                    manager.confirmEntityDestroyed(entityId);
                    ClientReflexMod.LOGGER.debug("Crystal confirmada como destruida: ID " + entityId);
                }
            }
        } else if (ctx.getPacketType() == ServerPacketContext.PacketType.ENTITY_STATUS) {
            // Verificar si es un cambio de estado de crystal
            if (ctx.getPacket() instanceof EntityStatusS2CPacket) {
                // Si la crystal sigue viva según el servidor, hacer rollback
                if (ctx.getEntity() instanceof EndCrystalEntity) {
                    int entityId = ctx.getEntity().getId();
                    if (manager.isEntityPredictedDestroyed(entityId)) {
                        // El servidor indica que la crystal sigue viva
                        manager.rollbackEntity(entityId);
                        ClientReflexMod.LOGGER.debug("Rollback de crystal: ID " + entityId);
                    }
                }
            }
        }
    }
    
    @Override
    public void tick() {
        // Limpiar predicciones expiradas se hace en PredictionManager
        // Aquí solo actualizamos el timeout si cambió en la configuración
        var config = ClientReflexConfig.getConfig();
        if (config.crystalPredictionTimeoutMs > 0) {
            predictionTimeoutMs = config.crystalPredictionTimeoutMs;
        }
    }
    
    @Override
    public String getName() {
        return "CrystalPrediction";
    }
    
    @Override
    public boolean isEnabled() {
        return ClientReflexConfig.getConfig().predictCrystals;
    }
}

