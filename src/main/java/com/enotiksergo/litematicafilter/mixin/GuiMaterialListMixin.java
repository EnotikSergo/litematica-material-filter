package com.enotiksergo.litematicafilter.mixin;

import com.enotiksergo.litematicafilter.screen.MaterialFilterScreen;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiMaterialList.class, remap = false)
public abstract class GuiMaterialListMixin extends GuiBase {

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        int btnWidth = 100;
        int btnHeight = 20;

        int x = this.width - 210;
        int y = this.height - 36;

        String buttonLabel = Text.translatable("litematicafilter.screen.button.open").getString();
        ButtonGeneric filterButton = new ButtonGeneric(x, y, btnWidth, btnHeight, buttonLabel);

        IButtonActionListener actionListener = (btn, mouseButton) -> {
            MinecraftClient.getInstance().setScreen(new MaterialFilterScreen(this));
        };

        this.addButton(filterButton, actionListener);
    }
}