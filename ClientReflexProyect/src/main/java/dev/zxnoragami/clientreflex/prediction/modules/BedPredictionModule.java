package dev.zxnoragami.clientreflex.prediction.modules;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.prediction.*;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

/**
 * Módulo de predicción para camas en dimensiones explosivas.
 * 
 * Cuando el jugador intenta usar una cama en el Nether o End, se predice
 * que el bloque será destruido por la explosión, permitiendo colocar otro
 * bloque inmediatamente sin esperar la confirmación del servidor.
 */
public class BedPredictionModule implements PredictionModule {
    private final PredictionManager manager;
    private final String type = "bed";
    private long predictionTimeoutMs = 600;
    
    public BedPredictionModule(PredictionManager manager) {
        this.manager = manager;
    }
    
    @Override
    public void onLocalAction(LocalActionContext ctx) {
        if (!isEnabled()) {
            return;
        }
        
        if (ctx.getActionType() == LocalActionContext.ActionType.BED_USE) {
            BlockPos pos = ctx.getPosition();
            if (pos != null) {
                // Marcar el bloque como predicho como destruido
                manager.predictBlockDestroyed(pos, type, predictionTimeoutMs);
                ClientReflexMod.LOGGER.debug("Cama predicha como destruida en: " + pos);
            }
        }
    }
    
    @Override
    public void onServerPacket(ServerPacketContext ctx) {
        if (!isEnabled()) {
            return;
        }
        
        // Reconciliar con actualizaciones de bloque del servidor
        if (ctx.getPacketType() == ServerPacketContext.PacketType.BLOCK_UPDATE) {
            if (ctx.getPacket() instanceof BlockUpdateS2CPacket) {
                BlockUpdateS2CPacket blockPacket = (BlockUpdateS2CPacket) ctx.getPacket();
                BlockPos pos = blockPacket.getPos();
                
                if (manager.isBlockPredictedDestroyed(pos)) {
                    // Verificar si el bloque fue realmente destruido
                    Block block = blockPacket.getState().getBlock();
                    if (!(block instanceof BedBlock)) {
                        // El bloque fue destruido
                        manager.confirmBlockDestroyed(pos);
                    } else {
                        // El bloque sigue siendo una cama (rollback)
                        manager.rollbackBlock(pos);
                        ClientReflexMod.LOGGER.debug("Rollback de cama en: " + pos);
                    }
                }
            }
        } else if (ctx.getPacketType() == ServerPacketContext.PacketType.EXPLOSION) {
            // Si hay una explosión cerca de una cama predicha, confirmarla
            BlockPos explosionPos = ctx.getPosition();
            if (explosionPos != null) {
                // Buscar bloques predichos cerca de la explosión
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        for (int z = -2; z <= 2; z++) {
                            BlockPos checkPos = explosionPos.add(x, y, z);
                            if (manager.isBlockPredictedDestroyed(checkPos)) {
                                manager.confirmBlockDestroyed(checkPos);
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void tick() {
        var config = ClientReflexConfig.getConfig();
        if (config.bedPredictionTimeoutMs > 0) {
            predictionTimeoutMs = config.bedPredictionTimeoutMs;
        }
    }
    
    @Override
    public String getName() {
        return "BedPrediction";
    }
    
    @Override
    public boolean isEnabled() {
        return ClientReflexConfig.getConfig().predictBeds;
    }
}

