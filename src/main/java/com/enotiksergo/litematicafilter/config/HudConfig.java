package com.enotiksergo.litematicafilter.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class HudConfig {
    public static int getMaxLines() {
        try {
            Class<?> clazz = Class.forName("fi.dy.masa.litematica.config.Configs$InfoOverlays");
            Field field = clazz.getField("MATERIAL_LIST_HUD_MAX_LINES");
            field.setAccessible(true);
            Object config = field.get(null);
            Method method = config.getClass().getMethod("getIntegerValue");

            return ((Number) method.invoke(config)).intValue();
        } catch (Exception e) {
            return 10;
        }
    }

    public static float getScale() {
        try {
            Class<?> clazz = Class.forName("fi.dy.masa.litematica.config.Configs$InfoOverlays");
            Field field = clazz.getField("MATERIAL_LIST_HUD_SCALE");
            field.setAccessible(true);
            Object config = field.get(null);

            Method method = config.getClass().getMethod("getDoubleValue");

            return ((Number) method.invoke(config)).floatValue();
        } catch (Exception e) {
            return 1.0f;
        }
    }
}
