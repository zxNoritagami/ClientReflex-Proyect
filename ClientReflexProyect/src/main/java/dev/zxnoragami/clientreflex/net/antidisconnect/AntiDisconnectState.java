package dev.zxnoragami.clientreflex.net.antidisconnect;

/**
 * Estados del sistema AntiDisconnect.
 */
public enum AntiDisconnectState {
    /**
     * Sistema inactivo, no hay monitoreo activo.
     */
    IDLE,
    
    /**
     * Monitoreando la red y paquetes para detectar problemas.
     */
    MONITORING,
    
    /**
     * Intentando recuperar la conexión sin desconectar.
     */
    RECOVERING,
    
    /**
     * Reconexión en curso.
     */
    RECONNECTING,
    
    /**
     * Esperando por backoff antes del siguiente intento.
     */
    COOLDOWN,
    
    /**
     * Sistema deshabilitado por configuración.
     */
    DISABLED,
    
    /**
     * Cancelado manualmente por el usuario.
     */
    STOPPED_BY_USER
}

