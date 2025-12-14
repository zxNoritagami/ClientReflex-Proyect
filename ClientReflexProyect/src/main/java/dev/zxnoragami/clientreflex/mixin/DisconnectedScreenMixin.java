package dev.zxnoragami.clientreflex.mixin;

import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import dev.zxnoragami.clientreflex.logging.ConnectionLogger;
import dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectManager;
import dev.zxnoragami.clientreflex.ui.AutoReconnectManager;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin para DisconnectedScreen que añade funcionalidad de auto-reconexión.
 * 
 * Este mixin modifica la pantalla de desconexión para añadir:
 * - Un botón "Reintentar ahora" que reconecta inmediatamente
 * - Un checkbox "Auto-reconectar" que inicia la reconexión automática
 * - Información sobre el estado de la reconexión
 */
@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow
    @Final
    private Screen parent;
    
    // Nota: El campo 'reason' no existe en DisconnectedScreen en Minecraft 1.21
    // Se elimina el @Shadow y se usa null cuando sea necesario
    
    private ButtonWidget reconnectButton;
    private ButtonWidget autoReconnectButton;
    private boolean autoReconnectEnabled = false;

    // Constructor requerido por Mixin, pero no se usa directamente
    protected DisconnectedScreenMixin(Text title) {
        super(title);
        throw new AssertionError("Mixin constructor should not be called");
    }

    /**
     * Inyecta los botones de reconexión cuando se inicializa la pantalla.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Registrar la desconexión en el log
        String serverInfo = this.client.getCurrentServerEntry() != null
            ? this.client.getCurrentServerEntry().address
            : "Servidor desconocido";
        // Nota: No podemos acceder al campo 'reason' en 1.21, usamos un mensaje genérico
        ConnectionLogger.getInstance().logDisconnect(
            "Desconexión del servidor",
            serverInfo
        );
        
        AutoReconnectManager reconnectManager = AutoReconnectManager.getInstance();
        AntiDisconnectManager antiDisconnectManager = AntiDisconnectManager.getInstance();
        
        int buttonY = this.height / 2 + 50;
        int buttonWidth = 150;
        int buttonHeight = 20;
        
        // Botón "Reintentar ahora"
        this.reconnectButton = ButtonWidget.builder(
            Text.translatable("clientreflex.reconnect.now"),
            button -> {
                reconnectManager.cancel(); // Cancelar auto-reconexión legacy si está activa
                antiDisconnectManager.cancel(); // Cancelar AntiDisconnect si está activo
                attemptReconnect();
            }
        ).dimensions(this.width / 2 - buttonWidth - 5, buttonY, buttonWidth, buttonHeight).build();
        
        this.addDrawableChild(this.reconnectButton);
        
        // Botón "Auto-reconectar" (toggle)
        String autoReconnectText = autoReconnectEnabled 
            ? "clientreflex.autoreconnect.enabled"
            : "clientreflex.autoreconnect.disabled";
            
        this.autoReconnectButton = ButtonWidget.builder(
            Text.translatable(autoReconnectText),
            button -> {
                autoReconnectEnabled = !autoReconnectEnabled;
                if (autoReconnectEnabled) {
                    // Iniciar auto-reconexión usando AntiDisconnect si está habilitado
                    if (ClientReflexConfig.getConfig().antiDisconnect.enabled) {
                        antiDisconnectManager.handleRecoverableError("Desconexión del servidor");
                    } else {
                        reconnectManager.startAutoReconnect(null);
                    }
                } else {
                    // Cancelar auto-reconexión
                    reconnectManager.cancel();
                    antiDisconnectManager.cancel();
                }
                updateButtonText();
            }
        ).dimensions(this.width / 2 + 5, buttonY, buttonWidth, buttonHeight).build();
        
        this.addDrawableChild(this.autoReconnectButton);
        
        // Si la auto-reconexión está habilitada en la configuración, iniciarla automáticamente
        // Usar el nuevo AntiDisconnectManager si está habilitado, sino usar el legacy
        if (ClientReflexConfig.getConfig().antiDisconnect.enabled) {
            // Usar el nuevo sistema AntiDisconnect
            String reason = "Desconexión del servidor";
            antiDisconnectManager.handleRecoverableError(reason);
            autoReconnectEnabled = true;
        } else if (ClientReflexConfig.getConfig().autoReconnectEnabled) {
            // Usar el sistema legacy
            autoReconnectEnabled = true;
            reconnectManager.startAutoReconnect(null);
        }
        updateButtonText();
    }
    
    /**
     * Actualiza el texto del botón de auto-reconexión.
     */
    private void updateButtonText() {
        AutoReconnectManager reconnectManager = AutoReconnectManager.getInstance();
        AntiDisconnectManager antiDisconnectManager = AntiDisconnectManager.getInstance();
        String text;
        
        // Priorizar mostrar estado del nuevo sistema AntiDisconnect
        if (antiDisconnectManager.getState() != dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectState.IDLE &&
            antiDisconnectManager.getState() != dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectState.DISABLED) {
            int attempt = antiDisconnectManager.getCurrentAttempt();
            int maxAttempts = ClientReflexConfig.getConfig().antiDisconnect.maxAttempts;
            text = String.format("AntiDisconnect (%d/%d)", attempt, maxAttempts);
        } else if (reconnectManager.isReconnecting()) {
            int remaining = reconnectManager.getRemainingAttempts();
            text = String.format("Auto-reconectar (%d intentos)", remaining);
        } else if (autoReconnectEnabled) {
            text = "Auto-reconectar: ON";
        } else {
            text = "Auto-reconectar: OFF";
        }
        
        this.autoReconnectButton.setMessage(Text.literal(text));
    }
    
    /**
     * Intenta reconectar al servidor.
     */
    private void attemptReconnect() {
        // La reconexión real se manejará desde el manager
        // Por ahora, simplemente volvemos a la pantalla anterior
        // En una implementación completa, esto conectaría directamente al servidor guardado
        this.client.setScreen(this.parent);
    }
    
    /**
     * Actualiza la pantalla cada tick para mostrar el estado de la reconexión.
     * Nota: El método 'tick' no existe en DisconnectedScreen en Minecraft 1.21.
     * Temporalmente comentado hasta encontrar una alternativa.
     */
    // @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (autoReconnectButton != null && autoReconnectEnabled) {
            AutoReconnectManager reconnectManager = AutoReconnectManager.getInstance();
            if (reconnectManager.isReconnecting()) {
                updateButtonText();
            }
        }
    }
}

