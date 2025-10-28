package ru.zenith.implement.features.modules.render;

import org.lwjgl.glfw.GLFW;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.implement.events.keyboard.KeyEvent;
import ru.zenith.implement.screens.listgui.ListGuiScreen;

public class ListGUI extends Module {
    
    public ListGUI() {
        super("ListGUI", "ListGUI", ModuleCategory.RENDER);
        setKey(GLFW.GLFW_KEY_P);
        setState(true); // Enable by default
    }
    
    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.isKeyDown(GLFW.GLFW_KEY_P)) {
            if (mc.currentScreen == null) {
                mc.setScreen(ListGuiScreen.INSTANCE);
            } else if (mc.currentScreen instanceof ListGuiScreen) {
                mc.setScreen(null);
            }
        }
    }
    
    @Override
    public void activate() {
        // Module is enabled and listening for key presses
    }
    
    @Override
    public void deactivate() {
        // Module is disabled
    }
}
