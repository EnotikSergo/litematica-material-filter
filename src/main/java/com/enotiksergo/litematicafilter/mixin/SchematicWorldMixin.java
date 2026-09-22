package com.enotiksergo.litematicafilter.mixin;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = { "fi.dy.masa.litematica.world.WorldSchematic" }, remap = false)
public class SchematicWorldMixin {

    @Inject(method = { "getBlockState" }, at = { @At("RETURN") }, cancellable = true, require = 0)
    private void onGetBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState original = cir.getReturnValue();
        if (original != null && !original.isAir()) {
            String blockId = BuiltInRegistries.BLOCK.getKey(original.getBlock()).toString();
            if (!FilterConfig.getInstance().shouldShow(blockId)) {
                cir.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }
}