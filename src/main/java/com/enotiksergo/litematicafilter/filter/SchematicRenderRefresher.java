package com.enotiksergo.litematicafilter.filter;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

public class SchematicRenderRefresher {
    private static final Logger LOGGER = LoggerFactory.getLogger("LitematicaFilter/Refresher");

    public static void refreshSchematicRendering() {
        Minecraft mc = Minecraft.getInstance();

        mc.execute(() -> {
            try {
                Class<?> litematicaRendererClass = Class.forName("fi.dy.masa.litematica.render.LitematicaRenderer");
                Method getInstance = litematicaRendererClass.getMethod("getInstance");
                Object litematicaRenderer = getInstance.invoke(null);
                if (litematicaRenderer != null) {
                    Method getWorldRenderer = litematicaRendererClass.getMethod("getWorldRenderer");
                    Object worldRenderer = getWorldRenderer.invoke(litematicaRenderer);
                    if (worldRenderer != null) {
                        try {
                            Method markNeedsUpdate = worldRenderer.getClass().getMethod("markNeedsUpdate");
                            markNeedsUpdate.invoke(worldRenderer);
                        } catch (Exception e) {}

                        try {
                            for (Field f : worldRenderer.getClass().getDeclaredFields()) {
                                if (Collection.class.isAssignableFrom(f.getType())) {
                                    f.setAccessible(true);
                                    Collection<?> list = (Collection) f.get(worldRenderer);
                                    if (list != null) {
                                        for (Object chunkRenderer : list) {
                                            if (chunkRenderer != null && chunkRenderer.getClass().getName().contains("ChunkRendererSchematic")) {
                                                for (Method m : chunkRenderer.getClass().getDeclaredMethods()) {
                                                    if (m.getName().equals("setNeedsUpdate")) {
                                                        m.setAccessible(true);
                                                        m.invoke(chunkRenderer, true);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e2) { LOGGER.debug("Failed marking chunk renderers dirty", e2); }
                    }
                }
            } catch (Exception e4) { LOGGER.warn("Litematica chunk refresh failed", e4); }
        });
    }
}