package dev.zxnoragami.clientreflex.prediction;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**
 * Contexto que describe una acción realizada localmente por el jugador.
 * Se pasa a los módulos de predicción cuando el jugador realiza una acción.
 */
public class LocalActionContext {
    /**
     * Tipos de acciones que pueden ser predichas.
     */
    public enum ActionType {
        CRYSTAL_PLACE,      // Colocar end crystal
        CRYSTAL_BREAK,      // Romper/detonar end crystal
        BED_USE,            // Usar cama en dimensión explosiva
        ANCHOR_USE,         // Usar respawn anchor
        TNT_MINECART_PLACE, // Colocar TNT minecart
        TNT_MINECART_BREAK, // Romper/activar TNT minecart
        BLOCK_BREAK,        // Romper bloque (genérico)
        BLOCK_PLACE         // Colocar bloque (genérico)
    }
    
    private final ActionType actionType;
    private final BlockPos position;
    private final Entity targetEntity;
    private final long timestamp;
    
    public LocalActionContext(ActionType actionType, BlockPos position, Entity targetEntity) {
        this.actionType = actionType;
        this.position = position;
        this.targetEntity = targetEntity;
        this.timestamp = System.currentTimeMillis();
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public Entity getTargetEntity() {
        return targetEntity;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}

