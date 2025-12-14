package dev.zxnoragami.clientreflex.net;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.logging.ConnectionLogger;
import dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

/**
 * Handler de timeout de lectura configurable.
 * Reemplaza el ReadTimeoutHandler por defecto de Minecraft con uno que permite
 * configurar el tiempo de espera desde la configuración del mod.
 * 
 * Este handler cierra la conexión cuando no se reciben paquetes durante el tiempo
 * configurado, pero permite valores más altos que el default de Minecraft para
 * ser más tolerante a lagazos temporales.
 */
public class ConfigurableReadTimeoutHandler extends ReadTimeoutHandler {
    private final int timeoutSeconds;

    /**
     * Crea un nuevo handler con el timeout configurado desde la configuración del mod.
     * IMPORTANTE: Lee la config ANTES de llamar a super() para pasar el valor correcto.
     */
    public ConfigurableReadTimeoutHandler() {
        // Leer el timeout de la config ANTES de llamar a super()
        this(ClientReflexConfig.getConfig().readTimeoutSeconds);
    }

    /**
     * Crea un nuevo handler con un timeout específico.
     * 
     * @param timeoutSeconds Tiempo de espera en segundos antes de desconectar
     */
    public ConfigurableReadTimeoutHandler(int timeoutSeconds) {
        // Pasar el valor correcto directamente al constructor padre
        super(timeoutSeconds, TimeUnit.SECONDS);
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
        // Registrar el timeout en el log
        long timeoutMs = timeoutSeconds * 1000L;
        String serverInfo = ctx.channel().remoteAddress() != null 
            ? ctx.channel().remoteAddress().toString() 
            : "Desconocido";
        ConnectionLogger.getInstance().logReadTimeout(timeoutMs, serverInfo);
        
        // Notificar al AntiDisconnectManager sobre el timeout
        AntiDisconnectManager.getInstance().handleRecoverableException(ReadTimeoutException.INSTANCE);
        
        // Cuando se supera el timeout, lanzar la excepción que cerrará la conexión
        // Esto es el comportamiento esperado: si no hay respuesta del servidor,
        // la conexión debe cerrarse para no quedarse colgada.
        ctx.fireExceptionCaught(ReadTimeoutException.INSTANCE);
    }

    /**
     * Obtiene el timeout configurado en segundos.
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}

