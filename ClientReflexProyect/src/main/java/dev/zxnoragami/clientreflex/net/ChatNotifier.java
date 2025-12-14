package dev.zxnoragami.clientreflex.net;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

/**
 * Sistema centralizado de notificaciones en chat.
 * Maneja rate-limiting, prefijos personalizados y mensajes en español.
 */
public class ChatNotifier {
    private static ChatNotifier instance;
    
    private final Map<String, Long> lastMessageTime = new HashMap<>();
    private int instanceCounter = 0;
    
    private ChatNotifier() {
    }
    
    public static ChatNotifier getInstance() {
        if (instance == null) {
            instance = new ChatNotifier();
        }
        return instance;
    }
    
    /**
     * Genera un nuevo ID de instancia.
     */
    public String generateInstanceId() {
        instanceCounter++;
        return "AD-" + instanceCounter;
    }
    
    /**
     * Envía un mensaje al chat si las notificaciones están habilitadas.
     * 
     * @param messageKey Clave única para rate-limiting
     * @param message El mensaje a mostrar
     */
    public void notify(String messageKey, String message) {
        var config = ClientReflexConfig.getConfig().chatNotifications;
        
        if (!config.enabled) {
            return;
        }
        
        // Rate limiting
        long now = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(messageKey);
        if (lastTime != null && (now - lastTime) < config.rateLimitMs) {
            return; // Demasiado pronto, ignorar
        }
        lastMessageTime.put(messageKey, now);
        
        // Enviar al chat en el hilo del cliente
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.inGameHud == null) {
            return;
        }
        
        // Verificar que hay mundo y jugador (no enviar mensajes en menú principal)
        if (client.world == null || client.player == null) {
            return;
        }
        
        client.execute(() -> {
            // Verificar nuevamente en el hilo del cliente
            if (client.inGameHud != null && client.inGameHud.getChatHud() != null &&
                client.world != null && client.player != null) {
                Text text = Text.literal(message).formatted(Formatting.GRAY);
                client.inGameHud.getChatHud().addMessage(text);
            }
        });
    }
    
    /**
     * Envía un mensaje con ID de instancia.
     * 
     * @param instanceId ID de la instancia
     * @param messageKey Clave única para rate-limiting
     * @param message El mensaje a mostrar
     */
    public void notifyWithInstance(String instanceId, String messageKey, String message) {
        var config = ClientReflexConfig.getConfig().chatNotifications;
        
        if (!config.enabled) {
            return;
        }
        
        String fullMessage;
        if (config.showInstanceId && instanceId != null) {
            if ("BRACKETED".equals(config.prefixStyle)) {
                fullMessage = "[clientreflex][AntiDisconnect " + instanceId + "] " + message;
            } else {
                fullMessage = "clientreflex " + instanceId + ": " + message;
            }
        } else {
            if ("BRACKETED".equals(config.prefixStyle)) {
                fullMessage = "[clientreflex][AntiDisconnect] " + message;
            } else {
                fullMessage = "clientreflex: " + message;
            }
        }
        
        notify(messageKey, fullMessage);
    }
    
    /**
     * Limpia el historial de rate-limiting.
     */
    public void clearHistory() {
        lastMessageTime.clear();
    }
}

