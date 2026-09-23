package com.enotiksergo.litematicafilter.materials;

import com.enotiksergo.litematicafilter.LitematicaFilterMod;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.json.MaterialListJsonBase;
import fi.dy.masa.litematica.materials.json.MaterialListJsonEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class CraftTreeAdapter {

    public static List<MatRow> buildFlatList(MaterialListBase source, Set<String> panelTargets) {
        Map<String, long[]> totals = new LinkedHashMap<>();
        Map<String, ItemStack> stacks = new HashMap<>();
        Map<String, Boolean> isRootMap = new HashMap<>();

        boolean filterByPanel = panelTargets != null && !panelTargets.isEmpty();

        for (MaterialListEntry entry : source.getMaterialsAll()) {
            ItemStack stack = entry.getStack();
            String rootId = getItemId(stack);

            if (filterByPanel && !panelTargets.contains(rootId)) {
                continue;
            }

            long totalNeeded = entry.getCountTotal();

            try {
                Item item = stack.getItem();
                var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
                MaterialListJsonBase jsonBase = new MaterialListJsonBase(holder, (int) totalNeeded, null, true);
                collectLeaves(jsonBase, totals, stacks, isRootMap, true);
            } catch (Exception e) {
                LitematicaFilterMod.LOGGER.debug("[LitematicaFilter] Decomposition failed for {}, using as-is", rootId);
                addToMap(totals, stacks, isRootMap, rootId, stack, totalNeeded, true);
            }
        }

        List<MatRow> rows = new ArrayList<>();
        for (Map.Entry<String, long[]> e : totals.entrySet()) {
            String id = e.getKey();
            long[] vals = e.getValue();
            ItemStack stack = stacks.getOrDefault(id, ItemStack.EMPTY);
            boolean isRoot = isRootMap.getOrDefault(id, false);
            rows.add(new MatRow(stack, id, stack.getHoverName().getString(),
                    vals[0], vals[1], isRoot));
        }

        rows.sort((a, b) -> {
            if (a.isRoot != b.isRoot) return a.isRoot ? -1 : 1;
            return Long.compare(b.missing, a.missing);
        });

        return rows;
    }

    private static void collectLeaves(MaterialListJsonBase node,
                                      Map<String, long[]> totals,
                                      Map<String, ItemStack> stacks,
                                      Map<String, Boolean> isRootMap,
                                      boolean isRoot) {

        MaterialListJsonEntry chosen = getChosenEntry(node);

        if (chosen != null) {
            Collection<MaterialListJsonBase> children = chosen.getRequirements();
            if (children != null) {
                for (MaterialListJsonBase child : children) {
                    collectLeaves(child, totals, stacks, isRootMap, false);
                }
            }
        } else {
            try {
                ItemStack stack = new ItemStack(node.getInput().value());
                String id = getItemId(stack);
                addToMap(totals, stacks, isRootMap, id, stack, node.getCount(), isRoot);
            } catch (Exception _) {
            }
        }
    }

    private static MaterialListJsonEntry getChosenEntry(MaterialListJsonBase b) {
        if (b.getMaterialsCrafting() != null) return b.getMaterialsCrafting();
        if (b.getMaterialsStonecutter() != null) return b.getMaterialsStonecutter();
        if (b.getMaterialsFurnace() != null) return b.getMaterialsFurnace();
        return null;
    }

    private static void addToMap(Map<String, long[]> totals, Map<String, ItemStack> stacks,
                                 Map<String, Boolean> isRootMap, String id,
                                 ItemStack stack, long count, boolean isRoot) {
        if (id.isEmpty()) return;
        totals.computeIfAbsent(id, _ -> new long[]{0, 0});
        totals.get(id)[0] += count;
        stacks.putIfAbsent(id, stack);
        if (isRoot) isRootMap.put(id, true);
    }

    public static String getItemId(ItemStack stack) {
        try {
            Block block = Block.byItem(stack.getItem());
            if (block != Blocks.AIR) {
                return BuiltInRegistries.BLOCK.getKey(block).toString();
            }
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        } catch (Exception e) {
            return "";
        }
    }
}