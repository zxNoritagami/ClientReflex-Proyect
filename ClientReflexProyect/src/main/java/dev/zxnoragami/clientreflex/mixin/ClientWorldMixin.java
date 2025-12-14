package dev.zxnoragami.clientreflex.mixin;

import dev.zxnoragami.clientreflex.prediction.PredictionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin para ClientWorld que intercepta paquetes del servidor relacionados
 * con entidades y bloques para reconciliar las predicciones.
 */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    
    /**
     * Intercepta cuando se añade una entidad al mundo.
     * Esto puede indicar que una predicción fue incorrecta (rollback).
     * Nota: En Minecraft 1.21, addEntity ya no tiene el parámetro int id.
     */
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        PredictionManager manager = PredictionManager.getInstance();
        
        // Obtener el ID de la entidad directamente
        int id = entity.getId();
        
        // Si la entidad estaba predicha como destruida, hacer rollback
        if (manager.isEntityPredictedDestroyed(id)) {
            manager.rollbackEntity(id);
        }
    }
    
    /**
     * Intercepta cuando se elimina una entidad del mundo.
     * Esto confirma que una predicción fue correcta.
     */
    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        PredictionManager manager = PredictionManager.getInstance();
        
        if (manager.isEntityPredictedDestroyed(entityId)) {
            manager.confirmEntityDestroyed(entityId);
        }
    }
}

