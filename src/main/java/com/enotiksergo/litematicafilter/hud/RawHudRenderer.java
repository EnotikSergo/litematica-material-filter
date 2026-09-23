package com.enotiksergo.litematicafilter.hud;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import com.enotiksergo.litematicafilter.materials.CraftTreeAdapter;
import com.enotiksergo.litematicafilter.materials.MatRow;
import com.enotiksergo.litematicafilter.materials.TreeNode;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RawHudRenderer {
    private static List<MatRow> cachedRows = null;
    private static long lastRebuildTime = 0;

    private static final Set<String> hiddenItems = new HashSet<>();

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker tickDelta) {
        if (!FilterConfig.getInstance().isShowRawHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (cachedRows == null || System.currentTimeMillis() - lastRebuildTime > 1000) {
            rebuildRows();
            lastRebuildTime = System.currentTimeMillis();
        }

        if (cachedRows == null || cachedRows.isEmpty()) return;

        int x = 4, y = 4, lineHeight = 12, padding = 2, iconSize = 16;
        int nameMaxWidth = 120, countWidth = 100;
        int totalWidth = padding + iconSize + 4 + nameMaxWidth + 4 + countWidth + padding;
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
            if (row.hasRecipe) {
                name = (row.isExpanded ? "§a[-] §r" : "§e[+] §r") + name;
            } else if (row.isRoot) {
                name = "§6" + name;
            }

            if (mc.font.width(name) > nameMaxWidth) {
                name = mc.font.plainSubstrByWidth(name, nameMaxWidth - 6) + "...";
            }
            ctx.text(mc.font, Component.literal(name), x + padding + iconSize + 4, currentY, 0xFFFFFFFF);

            String countStr = CraftTreeAdapter.formatCountForHud(row.missing);
            int countColor = 0xFFFF5555;
            int countX = x + totalWidth - padding - mc.font.width(countStr);
            ctx.text(mc.font, Component.literal(countStr), countX, currentY, countColor);
        }
    }

    private static void rebuildRows() {
        FilterConfig config = FilterConfig.getInstance();
        MaterialListBase matList = getOrInitMaterialList();

        if (matList == null || matList.getMaterialsAll().isEmpty()) {
            cachedRows = List.of();
            return;
        }

        try {
            TreeNode tree = CraftTreeAdapter.buildTree(matList, config.getPanelTargets());
            Set<String> priorityIds = new HashSet<>();
            priorityIds.addAll(config.getRenderTargets());
            priorityIds.addAll(config.getPanelTargets());

            List<MatRow> allRows = CraftTreeAdapter.flattenAndSort(tree, config.getExpandedItems(), priorityIds);

            List<MatRow> visibleRows = new ArrayList<>();
            for (MatRow row : allRows) {
                String idLower = row.itemId.toLowerCase().trim();

                if (row.missing <= 0 && !row.isExpandedChild) {
                    hiddenItems.add(idLower);
                    continue;
                }

                if (hiddenItems.contains(idLower) && !row.isExpandedChild) {
                    continue;
                }

                visibleRows.add(row);
            }
            cachedRows = visibleRows;
        } catch (Exception e) {
            cachedRows = List.of();
        }
    }

    private static MaterialListBase getOrInitMaterialList() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList == null || matList.getMaterialsAll().isEmpty()) {
            try {
                SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
                if (placement != null) {
                    matList = placement.getMaterialList();
                    DataManager.setMaterialList(matList);
                    if (matList.getMaterialsAll().isEmpty()) {
                        matList.reCreateMaterialList();
                    }
                }
            } catch (Exception ignored) {}
        }
        return matList;
    }

    public static void invalidateCache() {
        cachedRows = null;
        lastRebuildTime = 0;
    }

    public static void markAsCollected(String itemId) {
        if (itemId != null && !itemId.isEmpty()) {
            hiddenItems.add(itemId.toLowerCase().trim());
        }
    }

    public static boolean isHidden(String itemId) {
        return itemId != null && hiddenItems.contains(itemId.toLowerCase().trim());
    }

    public static void clearHiddenItems() {
        hiddenItems.clear();
        invalidateCache();
    }
}