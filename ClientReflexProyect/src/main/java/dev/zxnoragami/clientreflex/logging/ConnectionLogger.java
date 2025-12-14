package dev.zxnoragami.clientreflex.logging;

import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sistema de logging de errores de conexión.
 * Guarda todos los errores de conexión en un archivo de log para que el usuario
 * pueda revisar qué causó las desconexiones y encontrar soluciones.
 */
public class ConnectionLogger {
    private static ConnectionLogger instance;
    private final File logFile;
    private final ReentrantLock lock = new ReentrantLock();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ConnectionLogger() {
        File logsDir = new File(
            FabricLoader.getInstance().getGameDir().toFile(),
            "clientreflex.logs"
        );
        
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        
        this.logFile = new File(logsDir, "connection-errors.log");
    }

    public static ConnectionLogger getInstance() {
        if (instance == null) {
            instance = new ConnectionLogger();
        }
        return instance;
    }

    /**
     * Registra un error de conexión con información detallada.
     * 
     * @param errorType Tipo de error (TIMEOUT, DISCONNECT, EXCEPTION, etc.)
     * @param message Mensaje del error
     * @param exception Excepción asociada (puede ser null)
     * @param serverInfo Información del servidor (puede ser null)
     */
    public void logConnectionError(String errorType, String message, Throwable exception, String serverInfo) {
        lock.lock();
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            
            pw.println("=".repeat(80));
            pw.println("ERROR DE CONEXIÓN - " + timestamp);
            pw.println("Tipo: " + errorType);
            
            if (serverInfo != null && !serverInfo.isEmpty()) {
                pw.println("Servidor: " + serverInfo);
            }
            
            pw.println("Mensaje: " + message);
            
            if (exception != null) {
                pw.println("\nExcepción:");
                pw.println("  Clase: " + exception.getClass().getName());
                pw.println("  Mensaje: " + exception.getMessage());
                pw.println("\nStack Trace:");
                exception.printStackTrace(pw);
            }
            
            pw.println("=".repeat(80));
            pw.println();
            pw.flush();
            
            ClientReflexMod.LOGGER.info("Error de conexión registrado en: " + logFile.getAbsolutePath());
            
        } catch (IOException e) {
            ClientReflexMod.LOGGER.error("Error al escribir en el log de conexión: ", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registra un error de conexión simple (sin excepción).
     */
    public void logConnectionError(String errorType, String message, String serverInfo) {
        logConnectionError(errorType, message, null, serverInfo);
    }

    /**
     * Registra información sobre una desconexión.
     */
    public void logDisconnect(String reason, String serverInfo) {
        logConnectionError("DISCONNECT", reason, null, serverInfo);
    }

    /**
     * Registra un timeout de lectura.
     */
    public void logReadTimeout(long timeoutMs, String serverInfo) {
        String message = String.format("Timeout de lectura después de %d ms sin recibir paquetes", timeoutMs);
        logConnectionError("READ_TIMEOUT", message, null, serverInfo);
    }

    /**
     * Registra una excepción de red.
     */
    public void logNetworkException(String operation, Throwable exception, String serverInfo) {
        String message = "Error durante operación de red: " + operation;
        logConnectionError("NETWORK_EXCEPTION", message, exception, serverInfo);
    }

    /**
     * Obtiene la ruta del archivo de log.
     */
    public File getLogFile() {
        return logFile;
    }
}

