package com.enotiksergo.litematicafilter.client;

import com.enotiksergo.litematicafilter.LitematicaFilterMod;
import com.enotiksergo.litematicafilter.config.FilterConfig;
import com.enotiksergo.litematicafilter.hud.RawHudRenderer;
import com.enotiksergo.litematicafilter.screen.MaterialFilterScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class LitematicaFilterClient implements ClientModInitializer {
    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        FilterConfig.getInstance();

        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.litematicafilter.open_gui",
                InputConstants.Type.KEYSYM,
                86,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(LitematicaFilterMod.MOD_ID, "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new MaterialFilterScreen(null));
                }
            }
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(LitematicaFilterMod.MOD_ID, "raw_hud"),
                RawHudRenderer::render
        );
    }
}