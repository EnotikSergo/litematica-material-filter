package com.enotiksergo.litematicafilter.materials;

import net.minecraft.world.item.ItemStack;

public class MatRow {
    public final ItemStack stack;
    public final String itemId;
    public final String displayName;
    public final long total;
    public final long available;
    public final long missing;
    public final boolean isRoot;
    public final boolean hasRecipe;
    public final boolean isExpanded;

    public MatRow(TreeNode node, long available, boolean isExpanded) {
        this.stack = node.stack;
        this.itemId = node.itemId;
        this.displayName = node.displayName;
        this.total = node.total;
        this.available = available;
        this.missing = Math.max(0, total - available);
        this.isRoot = node.isRoot;
        this.hasRecipe = node.hasRecipe;
        this.isExpanded = isExpanded;
    }
}