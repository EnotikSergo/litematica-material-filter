package com.enotiksergo.litematicafilter.hud;

import com.enotiksergo.litematicafilter.config.FilterConfig;
import com.enotiksergo.litematicafilter.config.HudConfig;
import com.enotiksergo.litematicafilter.materials.CraftTreeAdapter;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.*;

public class MaterialHudRenderer {
    private static List<MaterialRow> cachedRows = null;
    private static long lastRebuildTime = 0;
    private static final Set<String> hiddenItems = new HashSet<>();

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker tickDelta) {
        if (!FilterConfig.getInstance().isShowMaterialHud()) return;
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
        int nameMaxWidth = 120, countWidth = 100;

        int rawWidth = padding + iconSize + 4 + nameMaxWidth + 4 + countWidth + padding;
        int rawHeight = linesToRender * lineHeight + padding * 2;

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

        for (int i = 0; i < linesToRender; i++) {
            MaterialRow row = cachedRows.get(i);
            int currentY = padding + i * lineHeight;

            ctx.item(row.stack, padding, currentY - 2);

            String name = row.displayName;
            if (mc.font.width(name) > nameMaxWidth) {
                name = mc.font.plainSubstrByWidth(name, nameMaxWidth - 6) + "...";
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
            Set<String> materialTargets = config.getMaterialTargets();
            boolean filterByMaterial = materialTargets != null && !materialTargets.isEmpty();
            Set<String> priorityIds = new HashSet<>();
            priorityIds.addAll(config.getRenderTargets());
            priorityIds.addAll(config.getMaterialTargets());
            Map<String, Integer> invCounts = getInventoryCounts();

            List<MaterialRow> rows = new ArrayList<>();
            for (MaterialListEntry entry : matList.getMaterialsAll()) {
                ItemStack stack = entry.getStack();
                String blockId = CraftTreeAdapter.getItemId(stack).toLowerCase().trim();
                if (blockId.isEmpty()) continue;
                if (filterByMaterial && !materialTargets.contains(blockId)) continue;

                long total = 0;
                try { total = entry.getCountTotal(); } catch (Exception ignored) {}
                long available = invCounts.getOrDefault(blockId, 0);
                long missing = Math.max(0, total - available);

                if (missing <= 0) {
                    hiddenItems.add(blockId);
                    continue;
                }
                if (hiddenItems.contains(blockId)) continue;

                rows.add(new MaterialRow(stack, blockId, stack.getHoverName().getString(), total, available, missing));
            }

            rows.sort((a, b) -> {
                boolean aPri = priorityIds.contains(a.blockId);
                boolean bPri = priorityIds.contains(b.blockId);
                if (aPri != bPri) return aPri ? -1 : 1;
                return Long.compare(b.total, a.total);
            });
            cachedRows = rows;
        } catch (Exception e) {
            cachedRows = List.of();
        }
    }

    private static Map<String, Integer> getInventoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return counts;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                String id = CraftTreeAdapter.getItemId(stack).toLowerCase().trim();
                if (!id.isEmpty()) counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return counts;
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

    private static class MaterialRow {
        final ItemStack stack;
        final String blockId;
        final String displayName;
        final long total;
        final long available;
        final long missing;

        MaterialRow(ItemStack stack, String blockId, String displayName, long total, long available, long missing) {
            this.stack = stack;
            this.blockId = blockId;
            this.displayName = displayName;
            this.total = total;
            this.available = available;
            this.missing = missing;
        }
    }
}