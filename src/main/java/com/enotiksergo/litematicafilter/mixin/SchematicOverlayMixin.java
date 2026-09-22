package com.enotiksergo.litematicafilter.mixin;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic;
import fi.dy.masa.litematica.render.schematic.IBlockOutputSchematic;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"fi.dy.masa.litematica.render.schematic.WorldRendererSchematic"}, remap = false)
public class SchematicOverlayMixin {

    @Inject(method = {"renderBlock"}, at = {@At("HEAD")}, cancellable = true, require = 0)
    private void onRenderBlock(BlockModelRendererSchematic blockRenderer, BlockAndTintGetter world, BlockState state, BlockPos pos, Vec3 posOffset, IBlockOutputSchematic output, CallbackInfoReturnable<Boolean> cir) {
        if (state != null) {
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (!FilterConfig.getInstance().shouldShow(blockId)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
}