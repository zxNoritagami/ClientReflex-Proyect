package dev.zxnoragami.clientreflex.prediction.modules;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.prediction.*;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.block.Block;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

/**
 * Módulo de predicción para Respawn Anchors.
 * 
 * Similar a las camas, cuando el jugador intenta usar un respawn anchor
 * sin carga en el Overworld, se predice que explotará y se destruirá.
 */
public class AnchorPredictionModule implements PredictionModule {
    private final PredictionManager manager;
    private final String type = "anchor";
    private long predictionTimeoutMs = 600;
    
    public AnchorPredictionModule(PredictionManager manager) {
        this.manager = manager;
    }
    
    @Override
    public void onLocalAction(LocalActionContext ctx) {
        if (!isEnabled()) {
            return;
        }
        
        if (ctx.getActionType() == LocalActionContext.ActionType.ANCHOR_USE) {
            BlockPos pos = ctx.getPosition();
            if (pos != null) {
                manager.predictBlockDestroyed(pos, type, predictionTimeoutMs);
                ClientReflexMod.LOGGER.debug("Anchor predicho como destruido en: " + pos);
            }
        }
    }
    
    @Override
    public void onServerPacket(ServerPacketContext ctx) {
        if (!isEnabled()) {
            return;
        }
        
        if (ctx.getPacketType() == ServerPacketContext.PacketType.BLOCK_UPDATE) {
            if (ctx.getPacket() instanceof BlockUpdateS2CPacket) {
                BlockUpdateS2CPacket blockPacket = (BlockUpdateS2CPacket) ctx.getPacket();
                BlockPos pos = blockPacket.getPos();
                
                if (manager.isBlockPredictedDestroyed(pos)) {
                    Block block = blockPacket.getState().getBlock();
                    if (!(block instanceof RespawnAnchorBlock)) {
                        manager.confirmBlockDestroyed(pos);
                    } else {
                        manager.rollbackBlock(pos);
                        ClientReflexMod.LOGGER.debug("Rollback de anchor en: " + pos);
                    }
                }
            }
        } else if (ctx.getPacketType() == ServerPacketContext.PacketType.EXPLOSION) {
            BlockPos explosionPos = ctx.getPosition();
            if (explosionPos != null) {
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
        if (config.anchorPredictionTimeoutMs > 0) {
            predictionTimeoutMs = config.anchorPredictionTimeoutMs;
        }
    }
    
    @Override
    public String getName() {
        return "AnchorPrediction";
    }
    
    @Override
    public boolean isEnabled() {
        return ClientReflexConfig.getConfig().predictAnchors;
    }
}

