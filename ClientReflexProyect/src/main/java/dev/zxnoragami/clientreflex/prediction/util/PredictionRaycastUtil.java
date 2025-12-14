package dev.zxnoragami.clientreflex.prediction.util;

import dev.zxnoragami.clientreflex.prediction.PredictionManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Utilidades para raycasting que respetan las predicciones.
 * 
 * Estas funciones verifican si una entidad o bloque está marcado como
 * predicho como destruido antes de incluirlo en los resultados de raycast.
 */
public class PredictionRaycastUtil {
    private static final PredictionManager manager = PredictionManager.getInstance();
    
    /**
     * Verifica si una entidad debe ser ignorada en raycast porque está
     * predicha como destruida.
     */
    public static boolean shouldIgnoreEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        return manager.isEntityPredictedDestroyed(entity.getId());
    }
    
    /**
     * Verifica si un bloque debe ser ignorado en raycast porque está
     * predicho como destruido.
     */
    public static boolean shouldIgnoreBlock(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        return manager.isBlockPredictedDestroyed(pos);
    }
    
    /**
     * Filtra un EntityHitResult si la entidad está predicha como destruida.
     * Retorna null si debe ser ignorada.
     */
    public static EntityHitResult filterPredictedEntities(EntityHitResult hit) {
        if (hit == null || hit.getEntity() == null) {
            return hit;
        }
        
        if (shouldIgnoreEntity(hit.getEntity())) {
            return null; // Ignorar esta entidad
        }
        
        return hit;
    }
}

