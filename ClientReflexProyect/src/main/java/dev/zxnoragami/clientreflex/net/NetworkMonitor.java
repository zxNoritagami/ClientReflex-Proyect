package dev.zxnoragami.clientreflex.net;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Monitor de red que rastrea el ping, jitter y estadísticas de conexión.
 * Mantiene un historial de valores de ping para calcular promedios y variaciones.
 */
public class NetworkMonitor {
    private final Deque<Long> pingHistory = new ArrayDeque<>();
    private final Deque<Long> packetTimestamps = new ArrayDeque<>();
    
    private long lastPacketReceived = System.currentTimeMillis();
    private long currentPing = 0;
    private long lastKeepAliveSent = 0;
    private long lastKeepAliveReceived = 0;
    
    private long bytesSent = 0;
    private long bytesReceived = 0;

    /**
     * Actualiza el monitor de red.
     * Debe llamarse cada tick del cliente.
     */
    public void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.player == null) {
            return;
        }

        // Obtener el ping actual del jugador
        ClientPlayerEntity player = client.player;
        if (player.networkHandler != null) {
            int serverPing = player.networkHandler.getPlayerListEntry(player.getUuid()) != null 
                ? player.networkHandler.getPlayerListEntry(player.getUuid()).getLatency() 
                : 0;
            
            if (serverPing > 0) {
                updatePing(serverPing);
            }
        }

        // Limpiar historial antiguo
        cleanOldHistory();
    }

    /**
     * Actualiza el valor de ping y lo añade al historial.
     */
    private void updatePing(int ping) {
        this.currentPing = ping;
        
        // Añadir al historial
        pingHistory.addLast((long) ping);
        
        // Limitar el tamaño del historial
        int maxSize = ClientReflexConfig.getConfig().pingHistorySize;
        while (pingHistory.size() > maxSize) {
            pingHistory.removeFirst();
        }
    }

    /**
     * Registra que se recibió un paquete.
     * Se llama desde los mixins cuando se detecta tráfico de red.
     */
    public void onPacketReceived() {
        long now = System.currentTimeMillis();
        lastPacketReceived = now;
        packetTimestamps.addLast(now);
        
        // Limpiar timestamps antiguos (más de 60 segundos)
        while (!packetTimestamps.isEmpty() && 
               now - packetTimestamps.getFirst() > 60000) {
            packetTimestamps.removeFirst();
        }
    }

    /**
     * Registra que se envió un paquete.
     */
    public void onPacketSent() {
        // Se puede usar para estadísticas de bytes enviados
    }

    /**
     * Registra el envío de un keep-alive.
     */
    public void onKeepAliveSent() {
        lastKeepAliveSent = System.currentTimeMillis();
    }

    /**
     * Registra la recepción de un keep-alive.
     */
    public void onKeepAliveReceived() {
        lastKeepAliveReceived = System.currentTimeMillis();
        if (lastKeepAliveSent > 0) {
            long ping = lastKeepAliveReceived - lastKeepAliveSent;
            updatePing((int) ping);
        }
    }

    /**
     * Limpia el historial antiguo.
     */
    private void cleanOldHistory() {
        int windowSeconds = ClientReflexConfig.getConfig().networkStatsWindowSeconds;
        
        // Limpiar pings muy antiguos (más allá de la ventana de estadísticas)
        while (!pingHistory.isEmpty() && pingHistory.size() > windowSeconds * 20) {
            pingHistory.removeFirst();
        }
    }

    /**
     * Obtiene el ping actual.
     */
    public long getCurrentPing() {
        return currentPing;
    }

    /**
     * Obtiene el ping promedio de los últimos valores.
     */
    public double getAveragePing() {
        if (pingHistory.isEmpty()) {
            return 0;
        }
        
        long sum = 0;
        for (Long ping : pingHistory) {
            sum += ping;
        }
        return (double) sum / pingHistory.size();
    }

    /**
     * Obtiene el ping máximo del historial.
     */
    public long getMaxPing() {
        if (pingHistory.isEmpty()) {
            return 0;
        }
        
        long max = 0;
        for (Long ping : pingHistory) {
            if (ping > max) {
                max = ping;
            }
        }
        return max;
    }

    /**
     * Calcula el jitter (variación del ping).
     * Es la diferencia promedio entre pings consecutivos.
     */
    public double getJitter() {
        if (pingHistory.size() < 2) {
            return 0;
        }
        
        Iterator<Long> it = pingHistory.iterator();
        long prev = it.next();
        double sumDiff = 0;
        int count = 0;
        
        while (it.hasNext()) {
            long current = it.next();
            sumDiff += Math.abs(current - prev);
            prev = current;
            count++;
        }
        
        return count > 0 ? sumDiff / count : 0;
    }

    /**
     * Obtiene el tiempo desde el último paquete recibido en milisegundos.
     */
    public long getTimeSinceLastPacket() {
        return System.currentTimeMillis() - lastPacketReceived;
    }

    /**
     * Obtiene el número de paquetes recibidos en la última ventana de tiempo.
     */
    public int getPacketsReceivedInWindow() {
        long now = System.currentTimeMillis();
        int windowSeconds = ClientReflexConfig.getConfig().networkStatsWindowSeconds;
        long windowMs = windowSeconds * 1000L;
        
        int count = 0;
        for (Long timestamp : packetTimestamps) {
            if (now - timestamp <= windowMs) {
                count++;
            }
        }
        return count;
    }

    /**
     * Resetea las estadísticas de bytes.
     */
    public void resetByteStats() {
        bytesSent = 0;
        bytesReceived = 0;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }
}

