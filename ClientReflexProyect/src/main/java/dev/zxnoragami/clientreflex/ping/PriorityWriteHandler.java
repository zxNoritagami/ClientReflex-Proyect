package dev.zxnoragami.clientreflex.ping;

import dev.zxnoragami.clientreflex.ClientReflexMod;
import dev.zxnoragami.clientreflex.net.PriorityPolicy;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handler de Netty que prioriza la escritura de paquetes según su importancia.
 * 
 * Este handler clasifica los paquetes en categorías de prioridad y asegura que
 * los paquetes críticos (movimiento, ataque, interacciones) se envíen primero
 * en situaciones de saturación de red.
 * 
 * IMPORTANTE: No modifica el orden lógico del protocolo, solo prioriza las
 * escrituras físicas de los paquetes. El servidor recibirá los mismos paquetes
 * que recibiría normalmente, solo que en un orden más eficiente.
 */
public class PriorityWriteHandler extends ChannelOutboundHandlerAdapter {
    
    /**
     * Niveles de prioridad para los paquetes.
     */
    public enum Priority {
        HIGH,    // Movimiento, ataque, uso de item, interactuar
        MEDIUM,  // Chat, comandos
        LOW      // Cambios de opciones, pings secundarios, etc.
    }
    
    private final Queue<QueuedPacket> highPriorityQueue = new ArrayDeque<>();
    private final Queue<QueuedPacket> mediumPriorityQueue = new ArrayDeque<>();
    private final Queue<QueuedPacket> lowPriorityQueue = new ArrayDeque<>();
    
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    private final AtomicReference<PriorityPolicy> policy = new AtomicReference<>(new PriorityPolicy());
    private static final int MAX_QUEUE_SIZE = 50; // Límite para evitar acumulación excesiva
    
    /**
     * Clasifica un paquete según su tipo y retorna su prioridad.
     * Usa PriorityPolicy para obtener la prioridad configurable.
     */
    private Priority classifyPacket(Packet<?> packet) {
        PriorityPolicy currentPolicy = policy.get();
        if (currentPolicy != null) {
            return currentPolicy.getPriority(packet);
        }
        
        // Fallback a clasificación hardcodeada si no hay política
        return fallbackClassifyPacket(packet);
    }
    
    /**
     * Clasificación fallback (comportamiento original).
     */
    private Priority fallbackClassifyPacket(Packet<?> packet) {
        // Paquetes de alta prioridad: movimiento, ataque, interacciones
        if (packet instanceof PlayerMoveC2SPacket ||
            packet instanceof PlayerActionC2SPacket ||
            packet instanceof PlayerInteractEntityC2SPacket ||
            packet instanceof PlayerInteractBlockC2SPacket ||
            packet instanceof HandSwingC2SPacket ||
            packet instanceof PlayerInputC2SPacket) {
            return Priority.HIGH;
        }
        
        // Paquetes de prioridad media: chat, comandos
        if (packet instanceof ChatMessageC2SPacket ||
            packet instanceof CommandExecutionC2SPacket) {
            return Priority.MEDIUM;
        }
        
        // Paquetes de baja prioridad: opciones, keep-alive secundarios, etc.
        if (packet instanceof UpdatePlayerAbilitiesC2SPacket ||
            packet instanceof ClientStatusC2SPacket) {
            return Priority.LOW;
        }
        
        // Keep-alive es alta prioridad para mantener la conexión
        if (packet != null && packet.getClass().getSimpleName().contains("KeepAlive") && 
            packet.getClass().getPackage().getName().contains("c2s")) {
            return Priority.HIGH;
        }
        
        // Por defecto, prioridad media
        return Priority.MEDIUM;
    }
    
    /**
     * Actualiza la política de priorización.
     * Permite hot-reload de la configuración.
     */
    public void updatePolicy(PriorityPolicy newPolicy) {
        policy.set(newPolicy);
        ClientReflexMod.LOGGER.debug("PriorityPolicy actualizada en PriorityWriteHandler");
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof Packet)) {
            // No es un paquete de Minecraft, pasar directamente
            super.write(ctx, msg, promise);
            return;
        }
        
        Packet<?> packet = (Packet<?>) msg;
        Priority priority = classifyPacket(packet);
        
        // Añadir a la cola correspondiente
        QueuedPacket queued = new QueuedPacket(packet, promise, priority);
        
        switch (priority) {
            case HIGH:
                if (highPriorityQueue.size() < MAX_QUEUE_SIZE) {
                    highPriorityQueue.offer(queued);
                } else {
                    // Cola de alta prioridad llena: enviar directamente para NO perder el paquete
                    ClientReflexMod.LOGGER.warn("Cola de alta prioridad llena, enviando paquete directamente");
                    ctx.writeAndFlush(packet, promise);
                    return; // Ya se envió, no añadir a la cola
                }
                break;
            case MEDIUM:
                if (mediumPriorityQueue.size() < MAX_QUEUE_SIZE) {
                    mediumPriorityQueue.offer(queued);
                } else {
                    // Cola de prioridad media llena: enviar directamente para NO perder el paquete
                    ClientReflexMod.LOGGER.debug("Cola de prioridad media llena, enviando paquete directamente");
                    ctx.writeAndFlush(packet, promise);
                    return; // Ya se envió, no añadir a la cola
                }
                break;
            case LOW:
                if (lowPriorityQueue.size() < MAX_QUEUE_SIZE) {
                    lowPriorityQueue.offer(queued);
                } else {
                    // Cola de baja prioridad llena: enviar directamente para NO perder el paquete
                    ClientReflexMod.LOGGER.debug("Cola de baja prioridad llena, enviando paquete directamente");
                    ctx.writeAndFlush(packet, promise);
                    return; // Ya se envió, no añadir a la cola
                }
                break;
        }
        
        // Intentar flush inmediato si no hay otro flush en progreso
        if (!flushing.get()) {
            flushQueues(ctx);
        }
    }
    
    /**
     * Procesa las colas de prioridad y envía los paquetes en orden.
     */
    private void flushQueues(ChannelHandlerContext ctx) {
        if (!flushing.compareAndSet(false, true)) {
            return; // Ya hay un flush en progreso
        }
        
        try {
            // Enviar primero los de alta prioridad
            flushQueue(ctx, highPriorityQueue);
            
            // Luego los de prioridad media
            flushQueue(ctx, mediumPriorityQueue);
            
            // Finalmente los de baja prioridad
            flushQueue(ctx, lowPriorityQueue);
            
            // Hacer flush del canal
            ctx.flush();
        } finally {
            flushing.set(false);
        }
    }
    
    /**
     * Envía todos los paquetes de una cola.
     */
    private void flushQueue(ChannelHandlerContext ctx, Queue<QueuedPacket> queue) {
        while (!queue.isEmpty()) {
            QueuedPacket queued = queue.poll();
            if (queued != null) {
                ctx.write(queued.packet, queued.promise);
            }
        }
    }
    
    /**
     * Se llama cuando el canal se vuelve writable.
     * Aprovechamos para procesar las colas.
     * Nota: channelWritableChanged fue removido en versiones recientes de Netty,
     * pero podemos usar flush() directamente cuando el canal es writable.
     */
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable()) {
            flushQueues(ctx);
        }
        super.flush(ctx);
    }
    
    /**
     * Clase interna para almacenar paquetes en cola.
     */
    private static class QueuedPacket {
        final Packet<?> packet;
        final ChannelPromise promise;
        @SuppressWarnings("unused")
        final Priority priority; // Almacenado para debugging/futuras funcionalidades
        
        QueuedPacket(Packet<?> packet, ChannelPromise promise, Priority priority) {
            this.packet = packet;
            this.promise = promise;
            this.priority = priority;
        }
    }
}

