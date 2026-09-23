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

    public MatRow(ItemStack stack, String itemId, String displayName,
                  long total, long available, boolean isRoot) {
        this.stack = stack;
        this.itemId = itemId;
        this.displayName = displayName;
        this.total = total;
        this.available = available;
        this.missing = Math.max(0, total - available);
        this.isRoot = isRoot;
    }
}