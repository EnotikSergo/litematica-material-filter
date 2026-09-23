package com.enotiksergo.litematicafilter.hud;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import com.enotiksergo.litematicafilter.materials.CraftTreeAdapter;
import com.enotiksergo.litematicafilter.materials.MatRow;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RawHudRenderer {
    private static List<MatRow> cachedRows = null;
    private static long lastRebuildTime = 0;

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker tickDelta) {
        if (!FilterConfig.getInstance().isShowRawHud()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (cachedRows == null || System.currentTimeMillis() - lastRebuildTime > 1000) {
            rebuildRows();
            lastRebuildTime = System.currentTimeMillis();
        }

        if (cachedRows == null || cachedRows.isEmpty()) return;

        int x = 4;
        int y = 4;
        int lineHeight = 12;
        int padding = 2;
        int iconSize = 16;
        int nameMaxWidth = 100;
        int totalWidth = padding + iconSize + 4 + nameMaxWidth + 4 + 60 + padding;
        int totalHeight = cachedRows.size() * lineHeight + padding * 2;

        ctx.fill(x, y, x + totalWidth, y + totalHeight, 0x80000000);
        ctx.fill(x, y, x + totalWidth, y + 1, 0xFF555555);
        ctx.fill(x, y + totalHeight - 1, x + totalWidth, y + totalHeight, 0xFF555555);
        ctx.fill(x, y, x + 1, y + totalHeight, 0xFF555555);
        ctx.fill(x + totalWidth - 1, y, x + totalWidth, y + totalHeight, 0xFF555555);

        for (int i = 0; i < cachedRows.size(); i++) {
            MatRow row = cachedRows.get(i);
            int currentY = y + padding + i * lineHeight;

            ctx.item(row.stack, x + padding, currentY - 2);

            String name = row.displayName;
            if (mc.font.width(name) > nameMaxWidth) {
                name = mc.font.plainSubstrByWidth(name, nameMaxWidth - 6) + "...";
            }
            int nameColor = row.isRoot ? 0xFFE0B040 : 0xFFFFFFFF;

            ctx.text(mc.font, Component.literal(name), x + padding + iconSize + 4, currentY, nameColor);

            String countStr;
            int countColor;
            if (row.missing > 0) {
                countStr = row.missing + " / " + row.total;
                countColor = 0xFFFF5555;
            } else {
                countStr = "0 / " + row.total;
                countColor = 0xFF55FF55;
            }
            int countX = x + totalWidth - padding - mc.font.width(countStr);
            ctx.text(mc.font, Component.literal(countStr), countX, currentY, countColor);
        }
    }

    private static void rebuildRows() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList == null) {
            cachedRows = List.of();
            return;
        }
        try {
            cachedRows = CraftTreeAdapter.buildFlatList(matList, FilterConfig.getInstance().getPanelTargets());
        } catch (Exception e) {
            cachedRows = List.of();
        }
    }

    public static void invalidateCache() {
        cachedRows = null;
        lastRebuildTime = 0;
    }
}