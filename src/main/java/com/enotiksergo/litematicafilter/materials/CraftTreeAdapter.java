package com.enotiksergo.litematicafilter.materials;

import com.enotiksergo.litematicafilter.LitematicaFilterMod;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.json.MaterialListJsonBase;
import fi.dy.masa.litematica.materials.json.MaterialListJsonEntry;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class CraftTreeAdapter {

    public static TreeNode buildTree(MaterialListBase source, Set<String> panelTargets) {
        TreeNode virtualRoot = new TreeNode(ItemStack.EMPTY, "__virtual_root__", "Virtual Root", 0, false, false);
        boolean filterByPanel = panelTargets != null && !panelTargets.isEmpty();
        for (MaterialListEntry entry : source.getMaterialsAll()) {
            ItemStack stack = entry.getStack();
            String rootId = getItemId(stack);
            if (filterByPanel && !panelTargets.contains(rootId)) continue;
            long totalNeeded = entry.getCountTotal();
            if (totalNeeded <= 0) continue;
            try {
                Item item = stack.getItem();
                var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
                MaterialListJsonBase jsonBase = new MaterialListJsonBase(holder, (int) totalNeeded, null, true);
                TreeNode rootNode = buildNode(jsonBase, true, 0);
                if (rootNode != null) virtualRoot.children.add(rootNode);
            } catch (Exception e) {
                LitematicaFilterMod.LOGGER.debug("[LitematicaFilter] Tree build failed for {}, using as leaf", rootId);
                virtualRoot.children.add(new TreeNode(stack, rootId, stack.getHoverName().getString(), totalNeeded, true, false));
            }
        }
        return virtualRoot;
    }

    private static TreeNode buildNode(MaterialListJsonBase base, boolean isRoot, int depth) {
        if (depth > 20) return null;
        try {
            if (base.getInput() == null) return null;
            base.getInput().value();
            ItemStack stack = new ItemStack(base.getInput().value());
            String id = getItemId(stack);
            long count = base.getCount();
            MaterialListJsonEntry chosen = getChosenEntry(base);
            boolean hasRecipe = chosen != null && chosen.getRequirements() != null && !chosen.getRequirements().isEmpty();
            TreeNode node = new TreeNode(stack, id, stack.getHoverName().getString(), count, isRoot, hasRecipe);
            if (hasRecipe) {
                for (MaterialListJsonBase childBase : chosen.getRequirements()) {
                    TreeNode childNode = buildNode(childBase, false, depth + 1);
                    if (childNode != null) node.children.add(childNode);
                }
            }
            return node;
        } catch (Exception e) { return null; }
    }

    private static MaterialListJsonEntry getChosenEntry(MaterialListJsonBase b) {
        try {
            if (b.getMaterialsCrafting() != null) return b.getMaterialsCrafting();
            if (b.getMaterialsStonecutter() != null) return b.getMaterialsStonecutter();
            if (b.getMaterialsFurnace() != null) return b.getMaterialsFurnace();
        } catch (Exception ignored) {}
        return null;
    }

    public static List<MatRow> flattenAndSort(TreeNode root, Set<String> expanded, Set<String> priorityIds) {
        Map<String, TreeNode> aggregated = new LinkedHashMap<>();
        Map<String, Boolean> expandedChildFlags = new HashMap<>();

        for (TreeNode child : root.children) {
            collectAndAggregateLeaves(child, expanded, aggregated, expandedChildFlags, false);
        }

        Map<String, Integer> inventoryCounts = getInventoryCounts();
        Map<String, Integer> placedCounts = getPlacedBlockCounts();

        Map<String, Integer> placedIngredientCounts = new HashMap<>();
        for (TreeNode child : root.children) {
            String rootId = child.itemId.toLowerCase().trim();
            int placedCount = placedCounts.getOrDefault(rootId, 0);
            if (placedCount > 0) {
                boolean isExpanded = expanded.contains(rootId);
                if (isExpanded && child.hasRecipe && !child.children.isEmpty()) {
                    distributePlacedToLeaves(child, placedCount, placedIngredientCounts, expanded);
                } else {
                    placedIngredientCounts.merge(rootId, placedCount, Integer::sum);
                }
            }
        }

        for (var entry : placedCounts.entrySet()) {
            String id = entry.getKey().toLowerCase().trim();
            if (!placedIngredientCounts.containsKey(id)) {
                placedIngredientCounts.merge(id, entry.getValue(), Integer::sum);
            }
        }

        List<MatRow> rows = new ArrayList<>();
        for (TreeNode node : aggregated.values()) {
            String idLower = node.itemId.toLowerCase().trim();
            boolean isExpanded = expanded.contains(idLower);
            boolean isExpandedChild = expandedChildFlags.getOrDefault(idLower, false);

            long available = inventoryCounts.getOrDefault(idLower, 0)
                    + placedIngredientCounts.getOrDefault(idLower, 0);
            rows.add(new MatRow(node, available, isExpanded, isExpandedChild));
        }
        rows.sort((a, b) -> {
            boolean aPri = priorityIds.contains(a.itemId);
            boolean bPri = priorityIds.contains(b.itemId);
            if (aPri != bPri) return aPri ? -1 : 1;
            return Long.compare(b.total, a.total);
        });
        return rows;
    }

    private static void distributePlacedToLeaves(TreeNode node, int placedCount, Map<String, Integer> out, Set<String> expanded) {
        if (node.hasRecipe && !node.children.isEmpty()) {
            for (TreeNode child : node.children) {
                String childId = child.itemId.toLowerCase().trim();
                boolean isChildExpanded = expanded.contains(childId);

                int childPlaced = (int) ((long) placedCount * child.total / node.total);
                if (childPlaced > 0) {
                    if (isChildExpanded && child.hasRecipe && !child.children.isEmpty()) {
                        distributePlacedToLeaves(child, childPlaced, out, expanded);
                    } else {
                        out.merge(childId, childPlaced, Integer::sum);
                    }
                }
            }
        } else {
            out.merge(node.itemId.toLowerCase().trim(), placedCount, Integer::sum);
        }
    }

    private static void collectAndAggregateLeaves(TreeNode node, Set<String> expanded, Map<String, TreeNode> out, Map<String, Boolean> expandedChildFlags, boolean parentExpanded) {
        if (node == null) return;
        String idLower = node.itemId.toLowerCase().trim();
        boolean isExpanded = expanded.contains(idLower);
        if (node.hasRecipe && isExpanded && !node.children.isEmpty()) {
            for (TreeNode child : node.children) {
                collectAndAggregateLeaves(child, expanded, out, expandedChildFlags, true);
            }
        } else {
            boolean isExpandedChild = parentExpanded;
            if (out.containsKey(node.itemId)) {
                TreeNode existing = out.get(node.itemId);
                TreeNode merged = new TreeNode(
                        existing.stack, existing.itemId, existing.displayName,
                        existing.total + node.total,
                        existing.isRoot || node.isRoot,
                        existing.hasRecipe || node.hasRecipe
                );
                out.put(node.itemId, merged);
                expandedChildFlags.put(node.itemId, expandedChildFlags.getOrDefault(node.itemId, false) || isExpandedChild);
            } else {
                out.put(node.itemId, node);
                expandedChildFlags.put(node.itemId, isExpandedChild);
            }
        }
    }

    public static Map<String, Integer> getInventoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return counts;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                countStackAndContents(stack, counts);
            }
        }
        return counts;
    }

    private static void countStackAndContents(ItemStack stack, Map<String, Integer> counts) {
        if (stack.isEmpty()) return;

        String id = getItemId(stack).toLowerCase().trim();
        if (!id.isEmpty()) {
            counts.merge(id, stack.getCount(), Integer::sum);
        }

        try {
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container != null) {
                container.nonEmptyItemCopyStream().forEach(inner -> countStackAndContents(inner, counts));
            }
        } catch (Exception ignored) {}

        try {
            BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundle != null) {
                bundle.itemCopyStream().forEach(inner -> countStackAndContents(inner, counts));
            }
        } catch (Exception ignored) {}
    }

    public static Map<String, Integer> getPlacedBlockCounts() {
        Map<String, Integer> counts = new HashMap<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return counts;

        Level clientWorld = mc.level;
        Level schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) return counts;

        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        if (placement == null) return counts;

        try {
            var regionBoxes = placement.getSubRegionBoxes(fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED);
            for (var entry : regionBoxes.entrySet()) {
                var box = entry.getValue();
                BlockPos pos1 = box.getPos1();
                BlockPos pos2 = box.getPos2();
                if (pos1 == null || pos2 == null) continue;

                int minX = Math.min(pos1.getX(), pos2.getX());
                int minY = Math.min(pos1.getY(), pos2.getY());
                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                int maxX = Math.max(pos1.getX(), pos2.getX());
                int maxY = Math.max(pos1.getY(), pos2.getY());
                int maxZ = Math.max(pos1.getZ(), pos2.getZ());

                long volume = (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
                if (volume > 1000000) continue;

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            try {
                                BlockState schematicState = schematicWorld.getBlockState(pos);
                                if (!schematicState.isAir()) {
                                    BlockState clientState = clientWorld.getBlockState(pos);
                                    if (schematicState.equals(clientState)) {
                                        String id = getItemId(new ItemStack(schematicState.getBlock()));
                                        if (!id.isEmpty()) {
                                            counts.merge(id, 1, Integer::sum);
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            LitematicaFilterMod.LOGGER.debug("[LitematicaFilter] Failed to count placed blocks", e);
        }
        return counts;
    }

    public static String getItemId(ItemStack stack) {
        try {
            Block block = Block.byItem(stack.getItem());
            if (block != Blocks.AIR) return BuiltInRegistries.BLOCK.getKey(block).toString();
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        } catch (Exception e) { return ""; }
    }

    public static String formatCountForHud(long total) {
        if (total <= 0) return "0";
        long stacks = total / 64;
        long remainder = total % 64;
        if (stacks > 0) return total + " (" + stacks + " x 64 + " + remainder + ")";
        return String.valueOf(total);
    }
}