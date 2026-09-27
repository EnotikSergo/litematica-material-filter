package com.enotiksergo.litematicafilter.hud;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import com.enotiksergo.litematicafilter.config.HudConfig;
import com.enotiksergo.litematicafilter.materials.CraftTreeAdapter;
import com.enotiksergo.litematicafilter.materials.MatRow;
import com.enotiksergo.litematicafilter.materials.TreeNode;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

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

        int maxLines = HudConfig.getMaxLines();
        float scale = HudConfig.getScale();
        int linesToRender = Math.min(cachedRows.size(), maxLines);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int lineHeight = 12, padding = 2, iconSize = 16;

        int maxNameWidth = 0;
        int maxCountWidth = 0;
        for (int i = 0; i < linesToRender; i++) {
            MatRow row = cachedRows.get(i);
            String name = row.displayName;
            if (row.hasRecipe) {
                name = (row.isExpanded ? "[-] " : "[+] ") + name;
            }
            int nameW = mc.font.width(name);
            if (nameW > maxNameWidth) maxNameWidth = nameW;

            String countStr = CraftTreeAdapter.formatCountForHud(row.missing);
            int countW = mc.font.width(countStr);
            if (countW > maxCountWidth) maxCountWidth = countW;
        }

        Component titleComponent = Component.translatable("litematicafilter.screen.raw_hud.title")
                .withStyle(ChatFormatting.BOLD);
        int titleWidth = mc.font.width(titleComponent);
        int contentWidth = Math.max(maxNameWidth, titleWidth);

        int rawWidth = padding + iconSize + 4 + contentWidth + 8 + maxCountWidth + padding;
        int titleHeight = mc.font.lineHeight + 4;
        int rawHeight = titleHeight + linesToRender * lineHeight + padding * 2;

        int sWidth = (int) (rawWidth * scale);
        int sHeight = (int) (rawHeight * scale);

        int baseX = screenWidth - sWidth - 4;
        int baseY = screenHeight - sHeight - 4;

        ctx.fill(baseX, baseY, baseX + sWidth, baseY + sHeight, 0x80000000);
        ctx.fill(baseX, baseY, baseX + sWidth, baseY + 1, 0xFF555555);
        ctx.fill(baseX, baseY + sHeight - 1, baseX + sWidth, baseY + sHeight, 0xFF555555);
        ctx.fill(baseX, baseY, baseX + 1, baseY + sHeight, 0xFF555555);
        ctx.fill(baseX + sWidth - 1, baseY, baseX + sWidth, baseY + sHeight, 0xFF555555);

        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate((float) baseX, (float) baseY);
        pose.scale(scale, scale);

        ctx.text(mc.font, titleComponent, padding, padding, 0xFFFFFFFF);

        for (int i = 0; i < linesToRender; i++) {
            MatRow row = cachedRows.get(i);
            int currentY = padding + titleHeight + i * lineHeight;

            ctx.item(row.stack, padding, currentY - 2);

            String name = row.displayName;
            if (row.hasRecipe) {
                name = (row.isExpanded ? "§a[-] §r" : "§e[+] §r") + name;
            }
            int textX = padding + iconSize + 4;
            ctx.text(mc.font, Component.literal(name), textX, currentY, 0xFFFFFFFF);

            String countStr = CraftTreeAdapter.formatCountForHud(row.missing);
            int countColor = 0xFFFFAA00;
            int countX = rawWidth - padding - mc.font.width(countStr);
            ctx.text(mc.font, Component.literal(countStr), countX, currentY, countColor);
        }

        pose.popMatrix();
    }

    private static void rebuildRows() {
        FilterConfig config = FilterConfig.getInstance();
        MaterialListBase matList = getOrInitMaterialList();
        if (matList == null || matList.getMaterialsAll().isEmpty()) {
            cachedRows = List.of();
            return;
        }
        try {
            TreeNode tree = CraftTreeAdapter.buildTree(matList, config.getMaterialTargets());
            Set<String> priorityIds = new HashSet<>();
            priorityIds.addAll(config.getRenderTargets());
            priorityIds.addAll(config.getMaterialTargets());
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
                    if (matList.getMaterialsAll().isEmpty()) matList.reCreateMaterialList();
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
        if (itemId != null && !itemId.isEmpty()) hiddenItems.add(itemId.toLowerCase().trim());
    }

    public static boolean isHidden(String itemId) {
        return itemId != null && hiddenItems.contains(itemId.toLowerCase().trim());
    }

    public static void clearHiddenItems() {
        hiddenItems.clear();
        invalidateCache();
    }
}