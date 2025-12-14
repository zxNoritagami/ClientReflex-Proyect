package dev.zxnoragami.clientreflex.mixin;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.ping.InputSmoothingManager;
import dev.zxnoragami.clientreflex.prediction.LocalActionContext;
import dev.zxnoragami.clientreflex.prediction.PredictionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin para ClientPlayerEntity que intercepta acciones del jugador
 * y las notifica al sistema de predicción.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    
    /**
     * Intercepta cuando el jugador ataca una entidad.
     * Nota: En Minecraft 1.21, el método puede tener una firma diferente.
     * Temporalmente comentado hasta encontrar el método correcto.
     */
    // @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        // Suavizado de input: feedback visual inmediato
        if (ClientReflexConfig.getConfig().enableInputSmoothing) {
            InputSmoothingManager.getInstance().onPlayerAttack(target);
        }
        
        PredictionManager manager = PredictionManager.getInstance();
        
        // Detectar si es una crystal
        if (target instanceof EndCrystalEntity) {
            LocalActionContext ctx = new LocalActionContext(
                LocalActionContext.ActionType.CRYSTAL_BREAK,
                target.getBlockPos(),
                target
            );
            manager.onLocalAction(ctx);
        }
        // Detectar si es un TNT minecart
        else if (target instanceof TntMinecartEntity) {
            LocalActionContext ctx = new LocalActionContext(
                LocalActionContext.ActionType.TNT_MINECART_BREAK,
                target.getBlockPos(),
                target
            );
            manager.onLocalAction(ctx);
        }
    }
    
    /**
     * Intercepta cuando el jugador interactúa con una entidad.
     * Nota: Temporalmente comentado hasta encontrar el método correcto en 1.21.
     */
    // @Inject(method = "interact", at = @At("HEAD"))
    private void onInteract(Entity entity, CallbackInfo ci) {
        PredictionManager manager = PredictionManager.getInstance();
        
        if (entity instanceof EndCrystalEntity) {
            LocalActionContext ctx = new LocalActionContext(
                LocalActionContext.ActionType.CRYSTAL_BREAK,
                entity.getBlockPos(),
                entity
            );
            manager.onLocalAction(ctx);
        }
    }
}

