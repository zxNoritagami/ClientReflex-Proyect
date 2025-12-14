package dev.zxnoragami.clientreflex;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Clase principal del mod ClientReflex.
 * Se inicializa cuando el mod se carga en el servidor (aunque este mod es solo cliente,
 * esta clase se ejecuta para inicializar la configuración).
 */
public class ClientReflexMod implements ModInitializer {
    public static final String MOD_ID = "clientreflex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // #region agent log
        try {
            String logPath = System.getProperty("user.dir") + "\\.cursor\\debug.log";
            String logEntry = String.format("{\"id\":\"log_init_%d\",\"timestamp\":%d,\"location\":\"ClientReflexMod.java:18\",\"message\":\"Mod initialization started\",\"data\":{\"modId\":\"%s\",\"javaVersion\":\"%s\",\"userDir\":\"%s\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n", System.currentTimeMillis(), System.currentTimeMillis(), MOD_ID, System.getProperty("java.version"), System.getProperty("user.dir"));
            Files.write(Paths.get(logPath), logEntry.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion
        
        LOGGER.info("ClientReflex inicializado");
        
        // Cargar la configuración al iniciar
        ClientReflexConfig.load();
    }
}
