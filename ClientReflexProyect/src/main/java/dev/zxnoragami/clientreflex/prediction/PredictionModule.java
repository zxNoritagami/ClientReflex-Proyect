package dev.zxnoragami.clientreflex.prediction;

/**
 * Interfaz para módulos de predicción de entidades y acciones.
 * 
 * Los módulos de predicción permiten adelantar visualmente acciones del cliente
 * para reducir el delay percibido en alto ping, especialmente útil en PvP.
 * 
 * IMPORTANTE: Este sistema NO modifica el protocolo ni envía paquetes ilegales.
 * Solo adelanta la eliminación/actualización visual de entidades en el cliente.
 */
public interface PredictionModule {
    
    /**
     * Se llama cuando el jugador realiza una acción localmente.
     * 
     * @param ctx Contexto con información sobre la acción realizada
     */
    void onLocalAction(LocalActionContext ctx);
    
    /**
     * Se llama cuando llega un paquete relevante del servidor.
     * Permite reconciliar las predicciones con el estado real del servidor.
     * 
     * @param ctx Contexto con información sobre el paquete recibido
     */
    void onServerPacket(ServerPacketContext ctx);
    
    /**
     * Se llama cada tick del cliente.
     * Permite limpiar predicciones expiradas y actualizar estados.
     */
    void tick();
    
    /**
     * Obtiene el nombre del módulo para logging y configuración.
     */
    String getName();
    
    /**
     * Verifica si el módulo está habilitado.
     */
    boolean isEnabled();
}

