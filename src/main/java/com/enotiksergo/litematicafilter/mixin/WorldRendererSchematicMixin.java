package com.enotiksergo.litematicafilter.mixin;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"fi.dy.masa.litematica.render.schematic.WorldRendererSchematic"}, remap = false)
public class WorldRendererSchematicMixin {

    @Inject(method = {"renderEntities"}, at = {@At("HEAD")}, cancellable = true, require = 0)
    private void onRenderEntities(Camera camera, Frustum frustum, PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, ProfilerFiller profiler, CallbackInfo ci) {
        if (!FilterConfig.getInstance().isShowEntities()) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderBlockEntities"}, at = {@At("HEAD")}, cancellable = true, require = 0)
    private void onRenderBlockEntities(Camera camera, Frustum frustum, PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, ProfilerFiller profiler, CallbackInfo ci) {
        if (!FilterConfig.getInstance().isShowEntities()) {
            ci.cancel();
        }
    }
}