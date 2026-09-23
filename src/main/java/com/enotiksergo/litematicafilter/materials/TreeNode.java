package com.enotiksergo.litematicafilter.materials;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    public final ItemStack stack;
    public final String itemId;
    public final String displayName;
    public final long total;
    public final boolean isRoot;
    public final boolean hasRecipe; // Есть ли рецепт для раскрытия
    public final List<TreeNode> children;

    public TreeNode(ItemStack stack, String itemId, String displayName, long total, boolean isRoot, boolean hasRecipe) {
        this.stack = stack;
        this.itemId = itemId;
        this.displayName = displayName;
        this.total = total;
        this.isRoot = isRoot;
        this.hasRecipe = hasRecipe;
        this.children = new ArrayList<>();
    }
}