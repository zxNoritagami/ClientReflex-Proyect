package dev.zxnoragami.clientreflex.net;

import dev.zxnoragami.clientreflex.ClientReflexClient;
import dev.zxnoragami.clientreflex.ping.PingMetrics;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Handler robusto para keep-alive que asegura respuestas rápidas.
 * 
 * Este handler intercepta los paquetes de keep-alive en ambas direcciones (entrada y salida)
 * y registra las métricas de ping. No modifica el contenido de los paquetes, solo
 * refuerza que se manejen de forma eficiente.
 * 
 * Usa ChannelDuplexHandler para poder interceptar tanto mensajes entrantes (channelRead)
 * como salientes (write).
 */
public class RobustKeepAliveHandler extends ChannelDuplexHandler {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Detectar paquetes de keep-alive entrantes usando el nombre de la clase
        // Esto es más robusto ante cambios de nombres en diferentes versiones
        if (msg != null && msg.getClass().getSimpleName().contains("KeepAlive") && 
            msg.getClass().getPackage().getName().contains("s2c")) {
            // Registrar en el monitor de red
            if (ClientReflexClient.getNetworkMonitor() != null) {
                ClientReflexClient.getNetworkMonitor().onKeepAliveReceived();
            }
            
            // Registrar en las métricas de ping para cálculo de RTT
            PingMetrics pingMetrics = ClientReflexClient.getPingMetrics();
            if (pingMetrics != null) {
                pingMetrics.onKeepAliveReceived();
            }
        }
        
        // Pasar el paquete al siguiente handler para que se procese normalmente
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Detectar cuando se envía un keep-alive usando el nombre de la clase
        // Esto es más robusto ante cambios de nombres en diferentes versiones
        if (msg != null && msg.getClass().getSimpleName().contains("KeepAlive") && 
            msg.getClass().getPackage().getName().contains("c2s")) {
            if (ClientReflexClient.getNetworkMonitor() != null) {
                ClientReflexClient.getNetworkMonitor().onKeepAliveSent();
            }
            
            // Registrar en las métricas de ping
            PingMetrics pingMetrics = ClientReflexClient.getPingMetrics();
            if (pingMetrics != null) {
                pingMetrics.onKeepAliveSent();
            }
        }
        
        // Pasar el mensaje al siguiente handler en el pipeline
        super.write(ctx, msg, promise);
    }
}

