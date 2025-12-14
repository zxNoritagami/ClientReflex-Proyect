package dev.zxnoragami.clientreflex.ui;

import dev.zxnoragami.clientreflex.ClientReflexMod;
import dev.zxnoragami.clientreflex.config.ClientReflexConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Pantalla de configuración de ClientReflex usando Cloth Config.
 */
public class ClientReflexConfigScreen {
    
    public static Screen create(Screen parent) {
        try {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("clientreflex.config.title"))
            .setSavingRunnable(() -> {
                ClientReflexConfig.save();
                // Hot-reload de configuración
                ClientReflexConfig.reload();
                // Actualizar políticas y handlers
                ClientReflexConfigScreen.updateRuntimeConfig();
            });
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        var config = ClientReflexConfig.getConfig();
        
        // Categoría: AntiDisconnect
        ConfigCategory antiDisconnect = builder.getOrCreateCategory(Text.translatable("clientreflex.config.antidisconnect"));
        antiDisconnect.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("clientreflex.config.antidisconnect.enabled"),
                config.antiDisconnect.enabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("clientreflex.config.antidisconnect.enabled.tooltip"))
            .setSaveConsumer(val -> config.antiDisconnect.enabled = val)
            .build());
        
        antiDisconnect.addEntry(entryBuilder.startIntField(
                Text.translatable("clientreflex.config.antidisconnect.maxAttempts"),
                config.antiDisconnect.maxAttempts)
            .setDefaultValue(8)
            .setMin(1)
            .setMax(50)
            .setTooltip(Text.translatable("clientreflex.config.antidisconnect.maxAttempts.tooltip"))
            .setSaveConsumer(val -> config.antiDisconnect.maxAttempts = val)
            .build());
        
        antiDisconnect.addEntry(entryBuilder.startIntField(
                Text.translatable("clientreflex.config.antidisconnect.baseDelayMs"),
                config.antiDisconnect.baseDelayMs)
            .setDefaultValue(1000)
            .setMin(100)
            .setMax(10000)
            .setTooltip(Text.translatable("clientreflex.config.antidisconnect.baseDelayMs.tooltip"))
            .setSaveConsumer(val -> config.antiDisconnect.baseDelayMs = val)
            .build());
        
        antiDisconnect.addEntry(entryBuilder.startIntField(
                Text.translatable("clientreflex.config.antidisconnect.maxDelayMs"),
                config.antiDisconnect.maxDelayMs)
            .setDefaultValue(30000)
            .setMin(1000)
            .setMax(120000)
            .setTooltip(Text.translatable("clientreflex.config.antidisconnect.maxDelayMs.tooltip"))
            .setSaveConsumer(val -> config.antiDisconnect.maxDelayMs = val)
            .build());
        
        antiDisconnect.addEntry(entryBuilder.startDoubleField(
                Text.translatable("clientreflex.config.antidisconnect.multiplier"),
                config.antiDisconnect.multiplier)
            .setDefaultValue(1.5)
            .setMin(1.0)
            .setMax(5.0)
            .setTooltip(Text.translatable("clientreflex.config.antidisconnect.multiplier.tooltip"))
            .setSaveConsumer(val -> config.antiDisconnect.multiplier = val)
            .build());
        
        antiDisconnect.addEntry(entryBuilder.startIntSlider(
                Text.translatable("clientreflex.config.antidisconnect.jitterPct"),
                config.antiDisconnect.jitterPct, 0, 50)
            .setDefaultValue(10)
            .setTooltip(Text.translatable("clientreflex.config.antidisconnect.jitterPct.tooltip"))
            .setSaveConsumer(val -> config.antiDisconnect.jitterPct = val)
            .build());
        
        antiDisconnect.addEntry(entryBuilder.startIntField(
                Text.translatable("clientreflex.config.antidisconnect.stableResetSeconds"),
                config.antiDisconnect.stableResetSeconds)
            .setDefaultValue(30)
            .setMin(5)
            .setMax(300)
            .setTooltip(Text.translatable("clientreflex.config.antidisconnect.stableResetSeconds.tooltip"))
            .setSaveConsumer(val -> config.antiDisconnect.stableResetSeconds = val)
            .build());
        
        // Categoría: Notificaciones en Chat
        ConfigCategory chatNotifications = builder.getOrCreateCategory(Text.translatable("clientreflex.config.chatnotifications"));
        chatNotifications.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("clientreflex.config.chatnotifications.enabled"),
                config.chatNotifications.enabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("clientreflex.config.chatnotifications.enabled.tooltip"))
            .setSaveConsumer(val -> config.chatNotifications.enabled = val)
            .build());
        
        chatNotifications.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("clientreflex.config.chatnotifications.showInstanceId"),
                config.chatNotifications.showInstanceId)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("clientreflex.config.chatnotifications.showInstanceId.tooltip"))
            .setSaveConsumer(val -> config.chatNotifications.showInstanceId = val)
            .build());
        
        Verbosity verbosityValue;
        try {
            verbosityValue = Verbosity.valueOf(config.chatNotifications.verbosity);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("Verbosity inválido '{}', usando NORMAL", config.chatNotifications.verbosity);
            verbosityValue = Verbosity.NORMAL;
        }
        
        chatNotifications.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.chatnotifications.verbosity"),
                Verbosity.class,
                verbosityValue)
            .setDefaultValue(Verbosity.NORMAL)
            .setTooltip(Text.translatable("clientreflex.config.chatnotifications.verbosity.tooltip"))
            .setSaveConsumer(val -> config.chatNotifications.verbosity = val.name())
            .build());
        
        chatNotifications.addEntry(entryBuilder.startIntField(
                Text.translatable("clientreflex.config.chatnotifications.rateLimitMs"),
                config.chatNotifications.rateLimitMs)
            .setDefaultValue(2000)
            .setMin(0)
            .setMax(10000)
            .setTooltip(Text.translatable("clientreflex.config.chatnotifications.rateLimitMs.tooltip"))
            .setSaveConsumer(val -> config.chatNotifications.rateLimitMs = val)
            .build());
        
        PrefixStyle prefixStyleValue;
        try {
            prefixStyleValue = PrefixStyle.valueOf(config.chatNotifications.prefixStyle);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("PrefixStyle inválido '{}', usando BRACKETED", config.chatNotifications.prefixStyle);
            prefixStyleValue = PrefixStyle.BRACKETED;
        }
        
        chatNotifications.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.chatnotifications.prefixStyle"),
                PrefixStyle.class,
                prefixStyleValue)
            .setDefaultValue(PrefixStyle.BRACKETED)
            .setTooltip(Text.translatable("clientreflex.config.chatnotifications.prefixStyle.tooltip"))
            .setSaveConsumer(val -> config.chatNotifications.prefixStyle = val.name())
            .build());
        
        // Categoría: Priorización de Paquetes
        ConfigCategory packetPriority = builder.getOrCreateCategory(Text.translatable("clientreflex.config.packetpriority"));
        
        Priority movementPriorityValue;
        try {
            movementPriorityValue = Priority.valueOf(config.packetPriority.movementPriority);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("Prioridad de movimiento inválida '{}', usando HIGH", config.packetPriority.movementPriority);
            movementPriorityValue = Priority.HIGH;
        }
        
        packetPriority.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.packetpriority.movement"),
                Priority.class,
                movementPriorityValue)
            .setDefaultValue(Priority.HIGH)
            .setTooltip(Text.translatable("clientreflex.config.packetpriority.movement.tooltip"))
            .setSaveConsumer(val -> config.packetPriority.movementPriority = val.name())
            .build());
        
        Priority attackPriorityValue;
        try {
            attackPriorityValue = Priority.valueOf(config.packetPriority.attackPriority);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("Prioridad de ataque inválida '{}', usando HIGH", config.packetPriority.attackPriority);
            attackPriorityValue = Priority.HIGH;
        }
        
        packetPriority.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.packetpriority.attack"),
                Priority.class,
                attackPriorityValue)
            .setDefaultValue(Priority.HIGH)
            .setTooltip(Text.translatable("clientreflex.config.packetpriority.attack.tooltip"))
            .setSaveConsumer(val -> config.packetPriority.attackPriority = val.name())
            .build());
        
        Priority blockPlacePriorityValue;
        try {
            blockPlacePriorityValue = Priority.valueOf(config.packetPriority.blockPlacePriority);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("Prioridad de colocación inválida '{}', usando HIGH", config.packetPriority.blockPlacePriority);
            blockPlacePriorityValue = Priority.HIGH;
        }
        
        packetPriority.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.packetpriority.blockPlace"),
                Priority.class,
                blockPlacePriorityValue)
            .setDefaultValue(Priority.HIGH)
            .setTooltip(Text.translatable("clientreflex.config.packetpriority.blockPlace.tooltip"))
            .setSaveConsumer(val -> config.packetPriority.blockPlacePriority = val.name())
            .build());
        
        Priority interactPriorityValue;
        try {
            interactPriorityValue = Priority.valueOf(config.packetPriority.interactPriority);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("Prioridad de interacción inválida '{}', usando MEDIUM", config.packetPriority.interactPriority);
            interactPriorityValue = Priority.MEDIUM;
        }
        
        packetPriority.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.packetpriority.interact"),
                Priority.class,
                interactPriorityValue)
            .setDefaultValue(Priority.MEDIUM)
            .setTooltip(Text.translatable("clientreflex.config.packetpriority.interact.tooltip"))
            .setSaveConsumer(val -> config.packetPriority.interactPriority = val.name())
            .build());
        
        Priority inventoryPriorityValue;
        try {
            inventoryPriorityValue = Priority.valueOf(config.packetPriority.inventoryPriority);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("Prioridad de inventario inválida '{}', usando LOW", config.packetPriority.inventoryPriority);
            inventoryPriorityValue = Priority.LOW;
        }
        
        packetPriority.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.packetpriority.inventory"),
                Priority.class,
                inventoryPriorityValue)
            .setDefaultValue(Priority.LOW)
            .setTooltip(Text.translatable("clientreflex.config.packetpriority.inventory.tooltip"))
            .setSaveConsumer(val -> config.packetPriority.inventoryPriority = val.name())
            .build());
        
        Priority chatPriorityValue;
        try {
            chatPriorityValue = Priority.valueOf(config.packetPriority.chatPriority);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("Prioridad de chat inválida '{}', usando LOW", config.packetPriority.chatPriority);
            chatPriorityValue = Priority.LOW;
        }
        
        packetPriority.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.packetpriority.chat"),
                Priority.class,
                chatPriorityValue)
            .setDefaultValue(Priority.LOW)
            .setTooltip(Text.translatable("clientreflex.config.packetpriority.chat.tooltip"))
            .setSaveConsumer(val -> config.packetPriority.chatPriority = val.name())
            .build());
        
        Priority keepAlivePriorityValue;
        try {
            keepAlivePriorityValue = Priority.valueOf(config.packetPriority.keepAlivePriority);
        } catch (IllegalArgumentException e) {
            ClientReflexMod.LOGGER.warn("Prioridad de keep-alive inválida '{}', usando MEDIUM", config.packetPriority.keepAlivePriority);
            keepAlivePriorityValue = Priority.MEDIUM;
        }
        
        packetPriority.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("clientreflex.config.packetpriority.keepAlive"),
                Priority.class,
                keepAlivePriorityValue)
            .setDefaultValue(Priority.MEDIUM)
            .setTooltip(Text.translatable("clientreflex.config.packetpriority.keepAlive.tooltip"))
            .setSaveConsumer(val -> config.packetPriority.keepAlivePriority = val.name())
            .build());
        
        packetPriority.addEntry(entryBuilder.startTextDescription(
                Text.translatable("clientreflex.config.packetpriority.description"))
            .build());
        
        return builder.build();
        } catch (Exception e) {
            ClientReflexMod.LOGGER.error("Error al crear la pantalla de configuración: ", e);
            // Retornar la pantalla padre si hay un error
            return parent;
        }
    }
    
    /**
     * Actualiza la configuración en tiempo de ejecución.
     */
    private static void updateRuntimeConfig() {
        // Actualizar PriorityPolicy
        dev.zxnoragami.clientreflex.net.PriorityPolicy policy = new dev.zxnoragami.clientreflex.net.PriorityPolicy();
        
        // Actualizar PriorityWriteHandler si está activo
        // Nota: Esto requiere acceso al pipeline de Netty, que se manejará en la próxima conexión
        // Por ahora, la política se actualizará cuando se cree un nuevo handler
        
        // Limpiar historial de ChatNotifier
        dev.zxnoragami.clientreflex.net.ChatNotifier.getInstance().clearHistory();
        
        // Actualizar estado de AntiDisconnect según configuración
        dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectManager manager = 
            dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectManager.getInstance();
        var config = ClientReflexConfig.getConfig();
        if (!config.antiDisconnect.enabled) {
            manager.stopMonitoring();
        } else if (manager.getState() == dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectState.DISABLED ||
                   manager.getState() == dev.zxnoragami.clientreflex.net.antidisconnect.AntiDisconnectState.IDLE) {
            manager.startMonitoring();
        }
    }
    
    // Enums para Cloth Config
    public enum Verbosity {
        MINIMAL, NORMAL, VERBOSE
    }
    
    public enum PrefixStyle {
        SIMPLE, BRACKETED
    }
    
    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}

