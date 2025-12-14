package dev.zxnoragami.clientreflex.ping;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;

/**
 * Gestor de suavizado de input y feedback visual instantáneo.
 * 
 * Este módulo asegura que las acciones del jugador (ataques, colocación de bloques,
 * uso de items) tengan feedback visual inmediato, incluso si la respuesta del servidor
 * tarda debido al ping alto.
 * 
 * IMPORTANTE: No modifica el protocolo ni envía paquetes extra. Solo adelanta
 * las animaciones y efectos visuales del lado del cliente.
 */
public class InputSmoothingManager {
    private static InputSmoothingManager instance;
    
    private InputSmoothingManager() {
    }
    
    public static InputSmoothingManager getInstance() {
        if (instance == null) {
            instance = new InputSmoothingManager();
        }
        return instance;
    }
    
    /**
     * Procesa un ataque del jugador y proporciona feedback visual inmediato.
     * 
     * @param target La entidad atacada (puede ser cualquier Entity, no solo PlayerEntity)
     */
    public void onPlayerAttack(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) {
            return;
        }
        
        // Adelantar la animación de swing del brazo
        // Minecraft ya hace esto en muchos casos, pero nos aseguramos de que
        // se ejecute inmediatamente sin esperar confirmación del servidor
        player.swingHand(Hand.MAIN_HAND);
        
        // Nota: Las partículas de daño y efectos de sonido se manejan normalmente
        // cuando llega la confirmación del servidor. Solo adelantamos la animación.
    }
    
    /**
     * Procesa la colocación de un bloque y proporciona feedback visual inmediato.
     */
    public void onBlockPlace() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) {
            return;
        }
        
        // Adelantar la animación de uso de item
        player.swingHand(Hand.MAIN_HAND);
        
        // Nota: El bloque se renderiza inmediatamente por el cliente vanilla,
        // pero puede desaparecer si el servidor rechaza la colocación.
        // Esto es comportamiento normal y no lo modificamos.
    }
    
    /**
     * Procesa el uso de un item y proporciona feedback visual inmediato.
     */
    public void onItemUse(Hand hand) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) {
            return;
        }
        
        // Adelantar la animación de uso
        player.swingHand(hand);
    }
    
    /**
     * Actualiza el suavizado cada tick.
     * Por ahora no hay lógica continua, pero se puede extender en el futuro.
     */
    public void tick() {
        // Lógica de suavizado continuo si es necesaria
    }
}

