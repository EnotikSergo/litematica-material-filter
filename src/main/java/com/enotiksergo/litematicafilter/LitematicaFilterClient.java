package com.enotiksergo.litematicafilter;

import com.enotiksergo.litematicafilter.screen.MaterialFilterScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class LitematicaFilterClient implements ClientModInitializer {

    public static KeyBinding OPEN_FILTER_KEY;

    @Override
    public void onInitializeClient() {

        OPEN_FILTER_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.litematica_filter.open_filter",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_FILTER_KEY.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new MaterialFilterScreen(client.currentScreen));
                }
            }
        });
    }
}
