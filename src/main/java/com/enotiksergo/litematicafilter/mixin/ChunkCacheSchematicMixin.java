package com.enotiksergo.litematicafilter.mixin;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = { "fi.dy.masa.litematica.render.schematic.ChunkCacheSchematic" }, remap = false)
public class ChunkCacheSchematicMixin {

    @Inject(method = { "getBlockState" }, at = { @At("RETURN") }, cancellable = true, require = 0)
    private void onGetBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && !state.isAir()) {
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (FilterConfig.getInstance().shouldShow(blockId)) {
                cir.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Inject(method = { "getFluidState" }, at = { @At("RETURN") }, cancellable = true, require = 0)
    private void onGetFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        if (!FilterConfig.getInstance().isShowEntities()) {
            FluidState fluid = cir.getReturnValue();
            if (fluid != null && !fluid.isEmpty()) {
                cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
            }
        }
    }
}