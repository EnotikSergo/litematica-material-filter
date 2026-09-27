package com.enotiksergo.litematicafilter.mixin;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = RayTraceUtils.class, remap = false)
public class RayTraceUtilsMixin {

    @Inject(method = "traceToSchematicWorld", at = @At("RETURN"), cancellable = true)
    private static void onTraceToSchematicWorld(Entity entity, double range, boolean respectRenderRange, boolean targetFluids, CallbackInfoReturnable<BlockHitResult> cir) {
        BlockHitResult hit = cir.getReturnValue();
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        Level schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) return;

        Vec3 eyesPos = entity.getEyePosition(1f);
        Vec3 lookDir = entity.getViewVector(1f).normalize();
        Vec3 endPos = eyesPos.add(lookDir.scale(range));
        ClipContext.Fluid fluidMode = targetFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;

        int maxSkips = 64;
        while (hit != null && hit.getType() == HitResult.Type.BLOCK && maxSkips-- > 0) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = schematicWorld.getBlockState(pos);
            if (state.isAir()) break;

            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

            if (FilterConfig.getInstance().shouldShow(blockId)) {
                Vec3 newStart = hit.getLocation();
                for (int i = 0; i < 20; i++) {
                    newStart = newStart.add(lookDir.scale(0.1));
                    BlockPos newStartPos = new BlockPos((int) Math.floor(newStart.x), (int) Math.floor(newStart.y), (int) Math.floor(newStart.z));
                    if (!newStartPos.equals(pos)) {
                        break;
                    }
                }

                hit = schematicWorld.clip(new ClipContext(newStart, endPos, ClipContext.Block.OUTLINE, fluidMode, entity));
            } else {
                break;
            }
        }

        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos finalPos = hit.getBlockPos();
            BlockState finalState = schematicWorld.getBlockState(finalPos);
            if (!finalState.isAir()) {
                String finalId = BuiltInRegistries.BLOCK.getKey(finalState.getBlock()).toString();
                if (FilterConfig.getInstance().shouldShow(finalId)) {
                    hit = BlockHitResult.miss(hit.getLocation(), hit.getDirection(), finalPos);
                }
            }
        }

        cir.setReturnValue(hit);
    }

    @Inject(method = "rayTraceBlocksToList", at = @At("RETURN"), cancellable = true)
    private static void onRayTraceBlocksToList(Level world, Vec3 start, Vec3 end, ClipContext.Fluid fluidMode, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, boolean respectLayerRange, int maxSteps, CallbackInfoReturnable<List<BlockHitResult>> cir) {
        filterHitList(cir, world);
    }

    @Inject(method = "rayTraceSchematicWorldBlocksToList", at = @At("RETURN"), cancellable = true)
    private static void onRayTraceSchematicWorldBlocksToList(Level world, Vec3 start, Vec3 end, int maxSteps, CallbackInfoReturnable<List<BlockHitResult>> cir) {
        filterHitList(cir, world);
    }

    @Inject(method = "getSchematicWorldTraceIfClosest", at = @At("RETURN"), cancellable = true)
    private static void onGetSchematicWorldTraceIfClosest(Level worldClient, Entity entity, double range, CallbackInfoReturnable<BlockPos> cir) {
        if (isFilteredOut(cir.getReturnValue())) cir.setReturnValue(null);
    }

    @Inject(method = "getSchematicWorldTraceIfClosestNoFluids", at = @At("RETURN"), cancellable = true)
    private static void onGetSchematicWorldTraceIfClosestNoFluids(Level worldClient, Entity entity, double range, CallbackInfoReturnable<BlockPos> cir) {
        if (isFilteredOut(cir.getReturnValue())) cir.setReturnValue(null);
    }

    @Inject(method = "getSchematicWorldTraceWrapperIfClosest", at = @At("RETURN"), cancellable = true)
    private static void onGetSchematicWorldTraceWrapperIfClosest(Level worldClient, Entity entity, double range, CallbackInfoReturnable<RayTraceUtils.RayTraceWrapper> cir) {
        RayTraceUtils.RayTraceWrapper w = cir.getReturnValue();
        if (w != null && w.getHitType() == RayTraceUtils.RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            if (isFilteredOut(w.getBlockHitResult().getBlockPos())) cir.setReturnValue(null);
        }
    }

    @Inject(method = "getSchematicWorldTraceWrapperIfClosestNoFluids", at = @At("RETURN"), cancellable = true)
    private static void onGetSchematicWorldTraceWrapperIfClosestNoFluids(Level worldClient, Entity entity, double range, CallbackInfoReturnable<RayTraceUtils.RayTraceWrapper> cir) {
        RayTraceUtils.RayTraceWrapper w = cir.getReturnValue();
        if (w != null && w.getHitType() == RayTraceUtils.RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            if (isFilteredOut(w.getBlockHitResult().getBlockPos())) cir.setReturnValue(null);
        }
    }

    @Inject(method = "getPickBlockLastTrace", at = @At("RETURN"), cancellable = true)
    private static void onGetPickBlockLastTrace(Level worldClient, Entity entity, double maxRange, boolean adjacentOnly, CallbackInfoReturnable<BlockPos> cir) {
        if (isFilteredOut(cir.getReturnValue())) cir.setReturnValue(null);
    }

    @Inject(method = "getFurthestSchematicWorldBlockBeforeVanilla", at = @At("RETURN"), cancellable = true)
    private static void onGetFurthestSchematicWorldBlockBeforeVanilla(Level worldClient, Entity entity, double maxRange, boolean requireVanillaBlockBehind, CallbackInfoReturnable<BlockPos> cir) {
        if (isFilteredOut(cir.getReturnValue())) cir.setReturnValue(null);
    }

    @Inject(method = "getFurthestSchematicWorldTraceBeforeVanilla", at = @At("RETURN"), cancellable = true)
    private static void onGetFurthestSchematicWorldTraceBeforeVanilla(Level worldClient, Entity entity, double maxRange, CallbackInfoReturnable<RayTraceUtils.RayTraceWrapper> cir) {
        RayTraceUtils.RayTraceWrapper w = cir.getReturnValue();
        if (w != null && w.getHitType() == RayTraceUtils.RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            if (isFilteredOut(w.getBlockHitResult().getBlockPos())) cir.setReturnValue(null);
        }
    }

    private static boolean isFilteredOut(BlockPos pos) {
        if (pos == null) return false;
        Level schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) return false;
        BlockState state = schematicWorld.getBlockState(pos);
        if (state.isAir()) return false;
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return FilterConfig.getInstance().shouldShow(blockId);
    }

    private static void filterHitList(CallbackInfoReturnable<List<BlockHitResult>> cir, Level world) {
        List<BlockHitResult> hits = cir.getReturnValue();
        if (hits == null || hits.isEmpty()) return;
        if (world != SchematicWorldHandler.getSchematicWorld()) return;

        List<BlockHitResult> filteredHits = new ArrayList<>();
        for (BlockHitResult hit : hits) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                if (!isFilteredOut(hit.getBlockPos())) {
                    filteredHits.add(hit);
                }
            } else {
                filteredHits.add(hit);
            }
        }
        cir.setReturnValue(filteredHits);
    }
}