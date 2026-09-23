package com.enotiksergo.litematicafilter.screen;

import com.enotiksergo.litematicafilter.LitematicaFilterMod;
import com.enotiksergo.litematicafilter.config.FilterConfig;
import com.enotiksergo.litematicafilter.filter.MaterialFilterManager;
import com.enotiksergo.litematicafilter.filter.SchematicRenderRefresher;
import com.enotiksergo.litematicafilter.hud.RawHudRenderer;
import com.enotiksergo.litematicafilter.materials.CraftTreeAdapter;
import com.enotiksergo.litematicafilter.materials.MatRow;
import com.enotiksergo.litematicafilter.materials.TreeNode;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class MaterialFilterScreen extends Screen {
    private static final int TITLE_Y = 8;
    private static final int STATUS_Y = 20;
    private static final int HINT_Y = 31;
    private static final int CONTROLS_Y = 44;
    private static final int SEARCH_Y = 68;
    private static final int SEARCH_H = 20;
    private static final int LIST_START_Y = 96;
    private static final int ENTRY_H = 22;
    private static final int BOTTOM_BAR_H = 28;
    private static final int BTN_H = 20;

    private static final int COL_PANEL_BG = 0xCC000000;
    private static final int COL_SEPARATOR = 0x80FFFFFF;
    private static final int COL_ENTRY_NORMAL = 0x20FFFFFF;
    private static final int COL_ENTRY_HOVER = 0x40FFFFFF;
    private static final int COL_ENTRY_RENDER = 0x405555AA;
    private static final int COL_ENTRY_PANEL = 0x4055AA55;
    private static final int COL_ENTRY_BOTH = 0x40AAAA55;
    private static final int COL_MARK_RENDER = 0xFF5555FF;
    private static final int COL_MARK_PANEL = 0xFF55FF55;
    private static final int COL_MARK_BOTH = 0xFFFFFF55;
    private static final int COL_COUNT_OK = 0xFF55FF55;
    private static final int COL_COUNT_MISS = 0xFFFFAA00;
    private static final int COL_TEXT_MAIN = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFFAAAAAA;
    private static final int COL_TEXT_FILTER = 0xFFFFFFFF;
    private static final int COL_TEXT_INFO = 0xFFDD88FF;
    private static final int COL_SCROLL_TRACK = 0x40FFFFFF;
    private static final int COL_SCROLL_THUMB = 0xAAFFFFFF;
    private static final int COL_ENTRY_ROOT = 0x30B08040;
    private static final int COL_MARK_ROOT = 0xFFE0B040;

    private final Set<String> selectedRenderIds = new HashSet<>();
    private final Set<String> selectedPanelIds = new HashSet<>();
    private final Screen parent;
    private final FilterConfig config;
    private List<MatRow> infoHudRows = new ArrayList<>();

    private EditBox searchField;
    private Button btnToggleEnabled, btnToggleMode, btnToggleEntities, btnToggleRawHud;
    private int scrollOffset = 0, visibleEntries = 0, listWidth = 420, listX = 0;
    private final List<MaterialListEntry> allEntries = new ArrayList<>();
    private List<MaterialListEntry> filteredEntries = new ArrayList<>();
    private boolean isDraggingScrollbar = false;

    public MaterialFilterScreen(Screen parent) {
        super(Component.translatable("litematicafilter.screen.title"));
        this.parent = parent;
        this.config = FilterConfig.getInstance();
        this.selectedRenderIds.addAll(config.getRenderTargets());
        this.selectedPanelIds.addAll(config.getPanelTargets());
    }

    @Override
    protected void init() {
        super.init();
        listWidth = Math.min(460, this.width - 40);
        listX = (this.width - listWidth) / 2;
        loadMaterialsFromLitematica();

        int ctrlBtnWidth = (listWidth - 15) / 4;
        int ctrlY = CONTROLS_Y;

        btnToggleEnabled = Button.builder(getToggleText(), _ -> { config.toggleEnabled(); updateControlButtons(); SchematicRenderRefresher.refreshSchematicRendering(); }).bounds(listX, ctrlY, ctrlBtnWidth, BTN_H).build();
        this.addRenderableWidget(btnToggleEnabled);
        btnToggleMode = Button.builder(getModeText(), _ -> { config.setMode(config.getMode().next()); updateControlButtons(); SchematicRenderRefresher.refreshSchematicRendering(); }).bounds(listX + ctrlBtnWidth + 5, ctrlY, ctrlBtnWidth, BTN_H).build();
        this.addRenderableWidget(btnToggleMode);
        btnToggleEntities = Button.builder(getEntityText(), _ -> { config.toggleShowEntities(); updateControlButtons(); SchematicRenderRefresher.refreshSchematicRendering(); }).bounds(listX + (ctrlBtnWidth + 5) * 2, ctrlY, ctrlBtnWidth, BTN_H).build();
        this.addRenderableWidget(btnToggleEntities);
        btnToggleRawHud = Button.builder(getRawHudText(), _ -> { config.toggleShowRawHud(); updateControlButtons(); if (config.isShowRawHud()) rebuildTreeAndRows(); scrollOffset = 0; }).bounds(listX + (ctrlBtnWidth + 5) * 3, ctrlY, ctrlBtnWidth, BTN_H).build();
        this.addRenderableWidget(btnToggleRawHud);

        int searchW = listWidth - 227;
        searchField = new EditBox(this.font, listX, SEARCH_Y, searchW, SEARCH_H, Component.translatable("litematicafilter.screen.search.narration"));
        searchField.setHint(Component.translatable("litematicafilter.screen.search.placeholder"));
        searchField.setMaxLength(64);
        searchField.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchField);

        int btn1X = listX + searchW + 5;
        int btn2X = btn1X + 105;
        this.addRenderableWidget(Button.builder(Component.translatable("litematicafilter.screen.button.apply_hud"), _ -> applyToHud()).bounds(btn1X, SEARCH_Y, 100, BTN_H).build());
        this.addRenderableWidget(Button.builder(Component.translatable("litematicafilter.screen.button.apply_render"), _ -> applyToRender()).bounds(btn2X, SEARCH_Y, 117, BTN_H).build());

        int bottomY = this.height - BOTTOM_BAR_H;
        this.addRenderableWidget(Button.builder(Component.translatable("litematicafilter.screen.button.clear"), _ -> clearFilter()).bounds(listX, bottomY, 110, BTN_H).build());
        this.addRenderableWidget(Button.builder(Component.translatable("litematicafilter.screen.button.close"), _ -> closeScreen()).bounds(listX + listWidth - 70, bottomY, 70, BTN_H).build());

        int listEndY = this.height - BOTTOM_BAR_H - 6;
        this.visibleEntries = Math.max(1, (listEndY - LIST_START_Y) / ENTRY_H);
        updateFilteredList(searchField.getValue());
        if (config.isShowRawHud()) rebuildTreeAndRows();
    }

    // ИСПРАВЛЕНИЕ: Асинхронный подсчет Litematica
    private void loadMaterialsFromLitematica() {
        allEntries.clear();
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList == null) {
            try {
                SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
                if (placement != null) {
                    matList = placement.getMaterialList();
                    DataManager.setMaterialList(matList);
                }
            } catch (Exception e) { LitematicaFilterMod.LOGGER.warn("Failed to auto-generate material list", e); }
        }
        if (matList == null) return;

        if (matList.getMaterialsAll().isEmpty()) {
            matList.reCreateMaterialList();
            MaterialListBase finalMatList = matList;
            matList.setCompletionListener(() -> {
                allEntries.clear();
                allEntries.addAll(finalMatList.getMaterialsAll());
                updateFilteredList(searchField != null ? searchField.getValue() : "");
                if (config.isShowRawHud()) rebuildTreeAndRows();
            });
        } else {
            allEntries.addAll(matList.getMaterialsAll());
        }
    }

    private void rebuildTreeAndRows() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList == null || matList.getMaterialsAll().isEmpty()) { infoHudRows.clear(); return; }
        try {
            TreeNode tree = CraftTreeAdapter.buildTree(matList, config.getPanelTargets());
            Set<String> priorityIds = new HashSet<>();
            priorityIds.addAll(config.getRenderTargets());
            priorityIds.addAll(config.getPanelTargets());
            infoHudRows = CraftTreeAdapter.flattenAndSort(tree, config.getExpandedItems(), priorityIds);
        } catch (Exception e) { infoHudRows.clear(); }
    }

    private String getBlockId(ItemStack stack) { return CraftTreeAdapter.getItemId(stack); }
    private void onSearchChanged(String text) { updateFilteredList(text); scrollOffset = 0; }
    private void updateFilteredList(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        filteredEntries = q.isEmpty() ? new ArrayList<>(allEntries) : allEntries.stream().filter(e -> matchesQuery(e, q)).collect(Collectors.toList());
    }
    private boolean matchesQuery(MaterialListEntry entry, String query) {
        try {
            ItemStack stack = entry.getStack();
            return getBlockId(stack).toLowerCase(Locale.ROOT).contains(query) || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
        } catch (Exception e) { return false; }
    }

    private void applyToHud() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList == null) return;
        Set<String> finalPanelIds = new LinkedHashSet<>(selectedPanelIds);
        if (!searchField.getValue().trim().isEmpty()) {
            for (MaterialListEntry entry : filteredEntries) {
                String blockId = getBlockId(entry.getStack());
                if (!blockId.isEmpty()) { finalPanelIds.add(blockId); selectedPanelIds.add(blockId); }
            }
        }
        config.setPanelTargets(finalPanelIds);
        matList.clearIgnored();
        for (MaterialListEntry entry : new ArrayList<>(matList.getMaterialsAll())) {
            String blockId = getBlockId(entry.getStack());
            if (!blockId.isEmpty() && !config.shouldShowInPanel(blockId)) matList.ignoreEntry(entry);
        }
        if (config.isShowRawHud()) { rebuildTreeAndRows(); RawHudRenderer.invalidateCache(); }
    }

    private void applyToRender() {
        Set<String> finalRenderIds = new LinkedHashSet<>(selectedRenderIds);
        if (!searchField.getValue().trim().isEmpty()) {
            for (MaterialListEntry entry : filteredEntries) {
                String blockId = getBlockId(entry.getStack());
                if (!blockId.isEmpty()) { finalRenderIds.add(blockId); selectedRenderIds.add(blockId); }
            }
        }
        config.setRenderTargets(finalRenderIds);
        MaterialFilterManager.getInstance().setActiveFilter(searchField.getValue().trim(), finalRenderIds);
        SchematicRenderRefresher.refreshSchematicRendering();
        RawHudRenderer.invalidateCache();
    }

    private void clearFilter() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList != null) matList.clearIgnored();
        config.clearRenderTargets(); config.clearPanelTargets(); config.clearExpanded();
        MaterialFilterManager.getInstance().clearFilter();
        selectedRenderIds.clear(); selectedPanelIds.clear();
        searchField.setValue(""); updateFilteredList("");
        if (config.isShowRawHud()) rebuildTreeAndRows();
        SchematicRenderRefresher.refreshSchematicRendering();
        RawHudRenderer.invalidateCache();
    }

    private void closeScreen() { this.minecraft.setScreenAndShow(parent); }

    private Component getToggleText() { return Component.translatable(config.isEnabled() ? "litematicafilter.screen.button.filter.on" : "litematicafilter.screen.button.filter.off"); }
    private Component getModeText() { return Component.translatable("litematicafilter.screen.button.mode", Component.translatable("litematicafilter.mode." + config.getMode().name().toLowerCase())); }
    private Component getEntityText() { return Component.translatable(config.isShowEntities() ? "litematicafilter.screen.button.entities.on" : "litematicafilter.screen.button.entities.off"); }
    private Component getRawHudText() { return Component.translatable(config.isShowRawHud() ? "litematicafilter.screen.button.rawhud.on" : "litematicafilter.screen.button.rawhud.off"); }
    private void updateControlButtons() {
        btnToggleEnabled.setMessage(getToggleText()); btnToggleMode.setMessage(getModeText());
        btnToggleEntities.setMessage(getEntityText()); btnToggleRawHud.setMessage(getRawHudText());
    }

    public void renderBackground(GuiGraphicsExtractor ctx) { ctx.fill(0, 0, this.width, this.height, 0xB2000000); }

    public void extractRenderState(@NotNull GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        ctx.fill(listX - 6, 2, listX + listWidth + 6, this.height - 2, COL_PANEL_BG);
        ctx.centeredText(font, Component.translatable("litematicafilter.screen.title").withStyle(net.minecraft.ChatFormatting.BOLD), this.width / 2, TITLE_Y, COL_TEXT_MAIN);
        ctx.centeredText(font, Component.translatable("litematicafilter.screen.status.counts", selectedPanelIds.size(), selectedRenderIds.size()), this.width / 2, STATUS_Y, COL_TEXT_FILTER);

        ctx.centeredText(font, Component.translatable(config.isShowRawHud() ? "litematicafilter.screen.hint.rawhud" : "litematicafilter.screen.hint.controls"), this.width / 2, HINT_Y, config.isShowRawHud() ? COL_TEXT_INFO : COL_TEXT_DIM);

        ctx.text(font, Component.translatable(config.isShowRawHud() ? "litematicafilter.screen.counter.rawhud" : "litematicafilter.screen.counter",
                config.isShowRawHud() ? infoHudRows.size() : filteredEntries.size(),
                config.isShowRawHud() ? 0 : allEntries.size()), listX, LIST_START_Y - 6, config.isShowRawHud() ? COL_TEXT_INFO : COL_TEXT_DIM);

        ctx.fill(listX, LIST_START_Y + 4, listX + listWidth, LIST_START_Y + 5, COL_SEPARATOR);
        int listEndY = this.height - BOTTOM_BAR_H + 6;

        if (config.isShowRawHud()) renderRawHudList(ctx, mx, my, listEndY);
        else renderList(ctx, mx, my, listEndY);

        int totalItems = config.isShowRawHud() ? infoHudRows.size() : filteredEntries.size();
        if (totalItems > visibleEntries) renderScrollbar(ctx, listX + listWidth + 3, listEndY, totalItems);

        super.extractRenderState(ctx, mx, my, delta);
    }

    private void renderList(GuiGraphicsExtractor ctx, int mx, int my, int endY) {
        for (int i = 0; i < visibleEntries; i++) {
            int idx = i + scrollOffset;
            if (idx >= filteredEntries.size()) break;
            int ey = LIST_START_Y + 7 + i * ENTRY_H;
            if (ey + ENTRY_H > endY) break;
            renderEntry(ctx, filteredEntries.get(idx), listX, ey, mx, my);
        }
    }

    private void renderEntry(GuiGraphicsExtractor ctx, MaterialListEntry entry, int x, int y, int mx, int my) {
        ItemStack stack; String blockId, displayName;
        try { stack = entry.getStack(); blockId = getBlockId(stack); displayName = stack.getHoverName().getString(); } catch (Exception e) { return; }
        if (blockId.isEmpty()) return;

        boolean inRender = selectedRenderIds.contains(blockId), inPanel = selectedPanelIds.contains(blockId);
        boolean hovered = mx >= x && mx < x + listWidth && my >= y && my < y + ENTRY_H;
        int bg = (inRender && inPanel) ? COL_ENTRY_BOTH : (inRender ? COL_ENTRY_RENDER : (inPanel ? COL_ENTRY_PANEL : (hovered ? COL_ENTRY_HOVER : COL_ENTRY_NORMAL)));
        int mark = (inRender && inPanel) ? COL_MARK_BOTH : (inRender ? COL_MARK_RENDER : (inPanel ? COL_MARK_PANEL : 0));

        ctx.fill(x, y, x + listWidth, y + ENTRY_H - 1, bg);
        if (mark != 0) ctx.fill(x, y, x + 3, y + ENTRY_H - 1, mark);
        ctx.item(stack, x + 3, y + 3);

        String nameStr = font.width(displayName) > listWidth - 80 ? font.plainSubstrByWidth(displayName, listWidth - 86) + "…" : displayName;
        ctx.text(font, Component.literal(nameStr), x + 23, y + 7, COL_TEXT_MAIN);
        try {
            long total = entry.getCountTotal(), avail = entry.getCountAvailable();
            if (total > 0) {
                String cnt = avail + " / " + total;
                ctx.text(font, Component.literal(cnt), x + listWidth - font.width(cnt) - 2, y + 7, (avail >= total) ? COL_COUNT_OK : COL_COUNT_MISS);
            }
        } catch (Exception ignored) {}
    }

    private void renderRawHudList(GuiGraphicsExtractor ctx, int mx, int my, int endY) {
        for (int i = 0; i < visibleEntries; i++) {
            int idx = i + scrollOffset;
            if (idx >= infoHudRows.size()) break;
            int ey = LIST_START_Y + 7 + i * ENTRY_H;
            if (ey + ENTRY_H > endY) break;
            renderRawHudEntry(ctx, infoHudRows.get(idx), listX, ey, mx, my);
        }
    }

    private void renderRawHudEntry(GuiGraphicsExtractor ctx, MatRow row, int x, int y, int mx, int my) {
        boolean hovered = mx >= x && mx < x + listWidth && my >= y && my < y + ENTRY_H;
        int bg = row.isRoot ? COL_ENTRY_ROOT : (hovered ? COL_ENTRY_HOVER : COL_ENTRY_NORMAL);
        int mark = row.isRoot ? COL_MARK_ROOT : 0;
        ctx.fill(x, y, x + listWidth, y + ENTRY_H - 1, bg);
        if (mark != 0) ctx.fill(x, y, x + 3, y + ENTRY_H - 1, mark);
        ctx.item(row.stack, x + 3, y + 3);

        String label = row.displayName;
        if (row.hasRecipe) label = (row.isExpanded ? "§a[-] §r" : "§e[+] §r") + label;
        else if (row.isRoot) label = "§6" + label;

        String nameStr = font.width(label) > listWidth - 100 ? font.plainSubstrByWidth(label, listWidth - 106) + "…" : label;
        ctx.text(font, Component.literal(nameStr), x + 23, y + 7, COL_TEXT_MAIN);

        // ИСПРАВЛЕНИЕ: Формат GUI: available / total
        String cnt = row.available + " / " + row.total;
        ctx.text(font, Component.literal(cnt), x + listWidth - font.width(cnt) - 2, y + 7, (row.available >= row.total) ? COL_COUNT_OK : COL_COUNT_MISS);
    }

    private void renderScrollbar(GuiGraphicsExtractor ctx, int x, int endY, int totalItems) {
        int trackH = endY - LIST_START_Y;
        int thumbH = Math.max(16, trackH * visibleEntries / totalItems);
        int maxScr = totalItems - visibleEntries;
        int thumbY = LIST_START_Y + (maxScr > 0 ? (int) ((float) scrollOffset / maxScr * (trackH - thumbH)) : 0);
        ctx.fill(x, LIST_START_Y, x + 4, endY, COL_SCROLL_TRACK);
        ctx.fill(x, thumbY, x + 4, thumbY + thumbH, COL_SCROLL_THUMB);
    }

    @Override public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int totalItems = config.isShowRawHud() ? infoHudRows.size() : filteredEntries.size();
        scrollOffset = Math.max(0, Math.min(Math.max(0, totalItems - visibleEntries), scrollOffset - (int) vScroll));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x(), mouseY = click.y();
        int totalItems = config.isShowRawHud() ? infoHudRows.size() : filteredEntries.size();

        if (totalItems > visibleEntries) {
            int listEndY = this.height - BOTTOM_BAR_H + 6;
            int scrollX = listX + listWidth + 3, trackH = listEndY - LIST_START_Y;
            int thumbH = Math.max(16, trackH * visibleEntries / totalItems);
            int maxScr = totalItems - visibleEntries;
            int thumbY = LIST_START_Y + (maxScr > 0 ? (int) ((float) scrollOffset / maxScr * (trackH - thumbH)) : 0);
            if (mouseX >= scrollX && mouseX <= scrollX + 4 && mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                isDraggingScrollbar = true; return true;
            }
        }

        if (config.isShowRawHud()) {
            if (click.button() == 0 && mouseY >= LIST_START_Y && mouseY < this.height - BOTTOM_BAR_H && mouseX >= listX && mouseX <= listX + listWidth) {
                int clickedRow = (int) ((mouseY - LIST_START_Y - 10) / ENTRY_H);
                int entryIndex = scrollOffset + clickedRow;
                if (entryIndex >= 0 && entryIndex < infoHudRows.size()) {
                    MatRow row = infoHudRows.get(entryIndex);
                    if (row.hasRecipe) {
                        config.toggleExpanded(row.itemId);
                        rebuildTreeAndRows();
                        RawHudRenderer.invalidateCache();
                        return true;
                    }
                }
            }
            return super.mouseClicked(click, doubled);
        }

        int button = click.button();
        if ((button == 0 || button == 1) && mouseY >= LIST_START_Y && mouseY < this.height - BOTTOM_BAR_H && mouseX >= listX && mouseX <= listX + listWidth) {
            int clickedRow = (int) ((mouseY - LIST_START_Y - 10) / ENTRY_H);
            int entryIndex = scrollOffset + clickedRow;
            if (entryIndex >= 0 && entryIndex < filteredEntries.size()) {
                String blockId = getBlockId(filteredEntries.get(entryIndex).getStack());
                if (!blockId.isEmpty()) {
                    Set<String> targetSet = (button == 0) ? selectedPanelIds : selectedRenderIds;
                    if (targetSet.contains(blockId)) targetSet.remove(blockId); else targetSet.add(blockId);
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseDragged(@NotNull MouseButtonEvent click, double offsetX, double offsetY) {
        boolean handled = super.mouseDragged(click, offsetX, offsetY);
        if (!isDraggingScrollbar) return handled;
        int totalItems = config.isShowRawHud() ? infoHudRows.size() : filteredEntries.size();
        int trackH = (this.height - BOTTOM_BAR_H + 6) - LIST_START_Y;
        int thumbH = Math.max(16, trackH * visibleEntries / totalItems);
        float ratio = (float) (click.y() - LIST_START_Y - (thumbH / 2.0)) / (trackH - thumbH);
        scrollOffset = Math.max(0, Math.min(Math.max(0, totalItems - visibleEntries), Math.round(ratio * Math.max(0, totalItems - visibleEntries))));
        return true;
    }

    @Override public boolean mouseReleased(@NotNull MouseButtonEvent click) { isDraggingScrollbar = false; return super.mouseReleased(click); }
}