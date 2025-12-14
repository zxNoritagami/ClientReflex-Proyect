package dev.zxnoragami.clientreflex.net;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.ping.PriorityWriteHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Política de priorización de paquetes configurable.
 * Mapea categorías de paquetes a niveles de prioridad según la configuración.
 */
public class PriorityPolicy {
    private final Map<String, PriorityWriteHandler.Priority> categoryMap = new HashMap<>();
    
    public PriorityPolicy() {
        loadFromConfig();
    }
    
    /**
     * Carga la política desde la configuración.
     */
    public void loadFromConfig() {
        var config = ClientReflexConfig.getConfig().packetPriority;
        
        categoryMap.put("MOVEMENT", parsePriority(config.movementPriority));
        categoryMap.put("ATTACK", parsePriority(config.attackPriority));
        categoryMap.put("BLOCK_PLACE", parsePriority(config.blockPlacePriority));
        categoryMap.put("INTERACT", parsePriority(config.interactPriority));
        categoryMap.put("INVENTORY", parsePriority(config.inventoryPriority));
        categoryMap.put("CHAT", parsePriority(config.chatPriority));
        categoryMap.put("KEEPALIVE", parsePriority(config.keepAlivePriority));
    }
    
    /**
     * Parsea un string de prioridad.
     */
    private PriorityWriteHandler.Priority parsePriority(String priorityStr) {
        if (priorityStr == null) {
            return PriorityWriteHandler.Priority.MEDIUM;
        }
        
        switch (priorityStr.toUpperCase()) {
            case "HIGH":
                return PriorityWriteHandler.Priority.HIGH;
            case "LOW":
                return PriorityWriteHandler.Priority.LOW;
            case "MEDIUM":
            default:
                return PriorityWriteHandler.Priority.MEDIUM;
        }
    }
    
    /**
     * Obtiene la prioridad de un paquete.
     */
    public PriorityWriteHandler.Priority getPriority(Packet<?> packet) {
        if (packet == null) {
            return PriorityWriteHandler.Priority.MEDIUM;
        }
        
        // Movimiento
        if (packet instanceof PlayerMoveC2SPacket || 
            packet instanceof PlayerInputC2SPacket) {
            return categoryMap.getOrDefault("MOVEMENT", PriorityWriteHandler.Priority.HIGH);
        }
        
        // Ataque
        if (packet instanceof PlayerActionC2SPacket ||
            packet instanceof HandSwingC2SPacket) {
            return categoryMap.getOrDefault("ATTACK", PriorityWriteHandler.Priority.HIGH);
        }
        
        // Colocación de bloques
        if (packet instanceof PlayerInteractBlockC2SPacket) {
            return categoryMap.getOrDefault("BLOCK_PLACE", PriorityWriteHandler.Priority.HIGH);
        }
        
        // Interacción
        if (packet instanceof PlayerInteractEntityC2SPacket) {
            return categoryMap.getOrDefault("INTERACT", PriorityWriteHandler.Priority.MEDIUM);
        }
        
        // Inventario
        if (packet.getClass().getSimpleName().contains("ClickSlot") ||
            packet.getClass().getSimpleName().contains("Inventory")) {
            return categoryMap.getOrDefault("INVENTORY", PriorityWriteHandler.Priority.LOW);
        }
        
        // Chat
        if (packet instanceof ChatMessageC2SPacket ||
            packet instanceof CommandExecutionC2SPacket) {
            return categoryMap.getOrDefault("CHAT", PriorityWriteHandler.Priority.LOW);
        }
        
        // Keep-alive
        if (packet != null && packet.getClass().getSimpleName().contains("KeepAlive") && 
            packet.getClass().getPackage().getName().contains("c2s")) {
            return categoryMap.getOrDefault("KEEPALIVE", PriorityWriteHandler.Priority.MEDIUM);
        }
        
        // Por defecto, prioridad media
        return PriorityWriteHandler.Priority.MEDIUM;
    }
    
    /**
     * Actualiza la política desde la configuración.
     */
    public void update() {
        loadFromConfig();
    }
}

