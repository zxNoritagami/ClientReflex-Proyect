package dev.zxnoragami.clientreflex.prediction;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;

/**
 * Contexto que describe un paquete recibido del servidor.
 * Se pasa a los módulos de predicción para que puedan reconciliar sus predicciones
 * con el estado real del servidor.
 */
public class ServerPacketContext {
    /**
     * Tipos de paquetes relevantes para la predicción.
     */
    public enum PacketType {
        ENTITY_SPAWN,       // Entidad spawn
        ENTITY_DESTROY,     // Entidad destruida
        BLOCK_UPDATE,       // Actualización de bloque
        EXPLOSION,          // Explosión
        ENTITY_STATUS,      // Cambio de estado de entidad
        OTHER               // Otro tipo de paquete
    }
    
    private final PacketType packetType;
    private final Packet<?> packet;
    private final Entity entity;      // Entidad relacionada (puede ser null)
    private final BlockPos position;   // Posición relacionada (puede ser null)
    
    public ServerPacketContext(PacketType packetType, Packet<?> packet, Entity entity, BlockPos position) {
        this.packetType = packetType;
        this.packet = packet;
        this.entity = entity;
        this.position = position;
    }
    
    public PacketType getPacketType() {
        return packetType;
    }
    
    public Packet<?> getPacket() {
        return packet;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public BlockPos getPosition() {
        return position;
    }
}

