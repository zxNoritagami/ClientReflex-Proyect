package dev.zxnoragami.clientreflex.ui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.zxnoragami.clientreflex.ClientReflexMod;
import net.minecraft.client.gui.screen.Screen;

/**
 * Integración con ModMenu para mostrar la pantalla de configuración.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            try {
                ClientReflexMod.LOGGER.info("ModMenu: Creando pantalla de configuración...");
                Screen configScreen = ClientReflexConfigScreen.create(parent);
                ClientReflexMod.LOGGER.info("ModMenu: Pantalla de configuración creada exitosamente");
                return configScreen;
            } catch (NoClassDefFoundError e) {
                ClientReflexMod.LOGGER.error("ModMenu: Cloth Config no está disponible. Asegúrate de tener Cloth Config instalado como mod.", e);
                return null;
            } catch (Exception e) {
                ClientReflexMod.LOGGER.error("ModMenu: Error al crear la pantalla de configuración: ", e);
                return null;
            }
        };
    }
}

