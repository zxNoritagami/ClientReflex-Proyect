package dev.zxnoragami.clientreflex.mixin;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.net.ConfigurableReadTimeoutHandler;
import dev.zxnoragami.clientreflex.net.RobustKeepAliveHandler;
import dev.zxnoragami.clientreflex.net.PriorityPolicy;
import dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectManager;
import dev.zxnoragami.clientreflex.ping.PriorityWriteHandler;
import dev.zxnoragami.clientreflex.ClientReflexClient;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin para ClientConnection que configura el pipeline de Netty.
 * 
 * Este mixin unifica toda la configuración de Netty y handlers en un solo lugar
 * para garantizar un orden de ejecución consistente:
 * 1. Primero se configuran las opciones de Netty (TCP_NODELAY, SO_KEEPALIVE)
 * 2. Luego se reemplaza el ReadTimeoutHandler
 * 3. Finalmente se añaden los handlers personalizados
 * 
 * Este mixin se ejecuta cuando se establece la conexión del cliente.
 */
@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    
    /**
     * Configura el pipeline de Netty cuando se activa el canal.
     * Este método unifica toda la lógica de configuración para evitar conflictos de orden.
     * 
     * Orden de ejecución garantizado:
     * 1. Configurar opciones de Netty (TCP_NODELAY, SO_KEEPALIVE)
     * 2. Reemplazar ReadTimeoutHandler con ConfigurableReadTimeoutHandler
     * 3. Añadir RobustKeepAliveHandler
     * 4. Añadir PriorityWriteHandler (si está habilitado)
     * 
     * @param ctx El contexto del canal de Netty
     * @param ci Callback info
     */
    @Inject(method = "channelActive", at = @At("TAIL"))
    private void onChannelActive(io.netty.channel.ChannelHandlerContext ctx, CallbackInfo ci) {
        Channel channel = ctx.channel();
        ChannelPipeline pipeline = channel.pipeline();
        var config = ClientReflexConfig.getConfig();
        
        // PASO 1: Configurar opciones de Netty primero
        // TCP_NODELAY desactiva el algoritmo de Nagle, reduciendo la latencia
        // al enviar paquetes pequeños inmediatamente en lugar de agruparlos
        if (config.tcpNoDelayOverride) {
            try {
                channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                ClientReflexMod.LOGGER.debug("TCP_NODELAY configurado en true");
            } catch (Exception e) {
                ClientReflexMod.LOGGER.warn("No se pudo configurar TCP_NODELAY: ", e);
            }
        }
        
        // SO_KEEPALIVE mantiene la conexión activa enviando paquetes periódicos
        if (config.soKeepAliveOverride) {
            try {
                channel.config().setOption(ChannelOption.SO_KEEPALIVE, true);
                ClientReflexMod.LOGGER.debug("SO_KEEPALIVE configurado en true");
            } catch (Exception e) {
                ClientReflexMod.LOGGER.warn("No se pudo configurar SO_KEEPALIVE: ", e);
            }
        }
        
        // PASO 2: Reemplazar el ReadTimeoutHandler por defecto con nuestro handler configurable
        // Leemos el timeout de la config ANTES de crear el handler
        int timeoutSeconds = config.readTimeoutSeconds;
        if (pipeline.get("timeout") != null) {
            pipeline.remove("timeout");
        }
        pipeline.addBefore("packet_handler", "clientreflex_timeout", 
            new ConfigurableReadTimeoutHandler(timeoutSeconds));
        
        // PASO 3: Añadir nuestro handler robusto de keep-alive
        pipeline.addBefore("packet_handler", "clientreflex_keepalive", 
            new RobustKeepAliveHandler());
        
        // PASO 4: Añadir el PriorityWriteHandler si está habilitado
        if (config.enablePriorityWriteHandler) {
            if (pipeline.get("clientreflex_priority") == null) {
                PriorityWriteHandler priorityHandler = new PriorityWriteHandler();
                PriorityPolicy policy = new PriorityPolicy();
                priorityHandler.updatePolicy(policy);
                pipeline.addBefore("packet_handler", "clientreflex_priority", priorityHandler);
                ClientReflexMod.LOGGER.debug("PriorityWriteHandler añadido al pipeline");
            }
        }
        
        // Guardar información del servidor para AntiDisconnect
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            ServerInfo serverInfo = client.getCurrentServerEntry();
            if (serverInfo != null) {
                AntiDisconnectManager.getInstance().saveServerInfo(serverInfo);
            }
        }
    }
    
    /**
     * Registra cuando se recibe un paquete para el monitor de red.
     */
    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void onChannelRead(io.netty.channel.ChannelHandlerContext context, 
                              net.minecraft.network.packet.Packet<?> packet, 
                              CallbackInfo ci) {
        if (ClientReflexClient.getNetworkMonitor() != null) {
            ClientReflexClient.getNetworkMonitor().onPacketReceived();
        }
    }
}

