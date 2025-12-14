package dev.zxnoragami.clientreflex.prediction.modules;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.prediction.*;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.TntMinecartEntity;

/**
 * Módulo de predicción para TNT Minecarts.
 * 
 * Cuando el jugador rompe o activa un TNT minecart, se predice que será
 * destruido, permitiendo colocar otro minecart en el mismo rail inmediatamente.
 */
public class TntMinecartPredictionModule implements PredictionModule {
    private final PredictionManager manager;
    private final String type = "tnt_minecart";
    private long predictionTimeoutMs = 700;
    
    public TntMinecartPredictionModule(PredictionManager manager) {
        this.manager = manager;
    }
    
    @Override
    public void onLocalAction(LocalActionContext ctx) {
        if (!isEnabled()) {
            return;
        }
        
        if (ctx.getActionType() == LocalActionContext.ActionType.TNT_MINECART_BREAK) {
            Entity target = ctx.getTargetEntity();
            if (target instanceof TntMinecartEntity) {
                int entityId = target.getId();
                manager.predictEntityDestroyed(entityId, type, predictionTimeoutMs);
                ClientReflexMod.LOGGER.debug("TNT Minecart predicho como destruido: ID " + entityId);
            }
        }
    }
    
    @Override
    public void onServerPacket(ServerPacketContext ctx) {
        if (!isEnabled()) {
            return;
        }
        
        if (ctx.getPacketType() == ServerPacketContext.PacketType.ENTITY_DESTROY) {
            if (ctx.getEntity() instanceof TntMinecartEntity) {
                int entityId = ctx.getEntity().getId();
                if (manager.isEntityPredictedDestroyed(entityId)) {
                    manager.confirmEntityDestroyed(entityId);
                    ClientReflexMod.LOGGER.debug("TNT Minecart confirmado como destruido: ID " + entityId);
                }
            }
        }
    }
    
    @Override
    public void tick() {
        var config = ClientReflexConfig.getConfig();
        if (config.tntMinecartPredictionTimeoutMs > 0) {
            predictionTimeoutMs = config.tntMinecartPredictionTimeoutMs;
        }
    }
    
    @Override
    public String getName() {
        return "TntMinecartPrediction";
    }
    
    @Override
    public boolean isEnabled() {
        return ClientReflexConfig.getConfig().predictTntMinecarts;
    }
}

