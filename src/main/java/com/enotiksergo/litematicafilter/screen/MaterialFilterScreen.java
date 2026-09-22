package com.enotiksergo.litematicafilter.screen;

import com.enotiksergo.litematicafilter.LitematicaFilterMod;
import com.enotiksergo.litematicafilter.config.FilterConfig;
import com.enotiksergo.litematicafilter.filter.MaterialFilterManager;
import com.enotiksergo.litematicafilter.filter.SchematicRenderRefresher;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

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
    private static final int COL_SCROLL_TRACK = 0x40FFFFFF;
    private static final int COL_SCROLL_THUMB = 0xAAFFFFFF;

    private final Set<String> selectedRenderIds = new HashSet<>();
    private final Set<String> selectedPanelIds = new HashSet<>();

    private final Screen parent;
    private final FilterConfig config;

    private EditBox searchField;
    private Button btnToggleEnabled;
    private Button btnToggleMode;
    private Button btnToggleEntities;
    private Button btnApplyHud;
    private Button btnApplyRender;

    private int scrollOffset = 0;
    private int visibleEntries = 0;
    private int listWidth = 420;
    private int listX = 0;
    private List<MaterialListEntry> allEntries = new ArrayList<>();
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

        int ctrlBtnWidth = (listWidth - 10) / 3;
        int ctrlY = CONTROLS_Y;

        btnToggleEnabled = Button.builder(getToggleText(), btn -> {
            config.toggleEnabled();
            updateControlButtons();
            SchematicRenderRefresher.refreshSchematicRendering();
        }).bounds(listX, ctrlY, ctrlBtnWidth, BTN_H).build();
        this.addRenderableWidget(btnToggleEnabled);

        btnToggleMode = Button.builder(getModeText(), btn -> {
            config.setMode(config.getMode().next());
            updateControlButtons();
            SchematicRenderRefresher.refreshSchematicRendering();
        }).bounds(listX + ctrlBtnWidth + 5, ctrlY, ctrlBtnWidth, BTN_H).build();
        this.addRenderableWidget(btnToggleMode);

        btnToggleEntities = Button.builder(getEntityText(), btn -> {
            config.toggleShowEntities();
            updateControlButtons();
            SchematicRenderRefresher.refreshSchematicRendering();
        }).bounds(listX + (ctrlBtnWidth + 5) * 2, ctrlY, ctrlBtnWidth, BTN_H).build();
        this.addRenderableWidget(btnToggleEntities);

        int searchW = listWidth - 227;
        searchField = new EditBox(this.font, listX, SEARCH_Y, searchW, SEARCH_H,
                Component.translatable("litematicafilter.screen.search.narration"));
        searchField.setHint(Component.translatable("litematicafilter.screen.search.placeholder"));
        searchField.setMaxLength(60);
        searchField.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchField);

        int btn1X = listX + searchW + 5;
        int btn2X = btn1X + 105;

        btnApplyHud = Button.builder(
                Component.translatable("litematicafilter.screen.button.apply_hud"),
                btn -> applyToHud()
        ).bounds(btn1X, SEARCH_Y, 100, BTN_H).build();
        this.addRenderableWidget(btnApplyHud);

        btnApplyRender = Button.builder(
                Component.translatable("litematicafilter.screen.button.apply_render"),
                btn -> applyToRender()
        ).bounds(btn2X, SEARCH_Y, 117, BTN_H).build();
        this.addRenderableWidget(btnApplyRender);

        int bottomY = this.height - BOTTOM_BAR_H;
        this.addRenderableWidget(Button.builder(
                Component.translatable("litematicafilter.screen.button.clear"),
                btn -> clearFilter()
        ).bounds(listX, bottomY, 110, BTN_H).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("litematicafilter.screen.button.close"),
                btn -> closeScreen()
        ).bounds(listX + listWidth - 70, bottomY, 70, BTN_H).build());

        int listEndY = this.height - BOTTOM_BAR_H - 6;
        int listAreaH = listEndY - LIST_START_Y;
        this.visibleEntries = Math.max(1, listAreaH / ENTRY_H);
        updateFilteredList(searchField.getValue());
    }

    private void loadMaterialsFromLitematica() {
        allEntries.clear();
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList == null) {
            LitematicaFilterMod.LOGGER.warn("[LitematicaFilter] DataManager.getMaterialList() returned null.");
            return;
        }
        allEntries.addAll(matList.getMaterialsAll());
    }

    private String getBlockId(ItemStack stack) {
        try {
            Block block = Block.byItem(stack.getItem());
            if (block != null && block != Blocks.AIR) {
                return BuiltInRegistries.BLOCK.getKey(block).toString();
            }
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void onSearchChanged(String text) {
        updateFilteredList(text);
        scrollOffset = 0;
    }

    private void updateFilteredList(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            filteredEntries = new ArrayList<>(allEntries);
        } else {
            filteredEntries = allEntries.stream()
                    .filter(e -> matchesQuery(e, q))
                    .collect(Collectors.toList());
        }
    }

    private boolean matchesQuery(MaterialListEntry entry, String query) {
        try {
            ItemStack stack = entry.getStack();
            String id = getBlockId(stack).toLowerCase(Locale.ROOT);
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            return id.contains(query) || name.contains(query);
        } catch (Exception e) {
            return false;
        }
    }

    private void applyToHud() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList == null) return;

        String searchText = searchField.getValue().trim();
        Set<String> finalPanelIds = new LinkedHashSet<>(selectedPanelIds);

        if (!searchText.isEmpty()) {
            for (MaterialListEntry entry : filteredEntries) {
                String blockId = getBlockId(entry.getStack());
                if (!blockId.isEmpty()) {
                    finalPanelIds.add(blockId);
                    selectedPanelIds.add(blockId);
                }
            }
        }

        config.setPanelTargets(finalPanelIds);

        matList.clearIgnored();
        for (MaterialListEntry entry : new ArrayList<>(matList.getMaterialsAll())) {
            String blockId = getBlockId(entry.getStack());
            if (!blockId.isEmpty() && !config.shouldShowInPanel(blockId)) {
                matList.ignoreEntry(entry);
            }
        }
    }

    private void applyToRender() {
        String searchText = searchField.getValue().trim();
        Set<String> finalRenderIds = new LinkedHashSet<>(selectedRenderIds);

        if (!searchText.isEmpty()) {
            for (MaterialListEntry entry : filteredEntries) {
                String blockId = getBlockId(entry.getStack());
                if (!blockId.isEmpty()) {
                    finalRenderIds.add(blockId);
                    selectedRenderIds.add(blockId);
                }
            }
        }

        config.setRenderTargets(finalRenderIds);
        MaterialFilterManager.getInstance().setActiveFilter(searchText, finalRenderIds);
        SchematicRenderRefresher.refreshSchematicRendering();
    }

    private void clearFilter() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList != null) matList.clearIgnored();

        config.clearRenderTargets();
        config.clearPanelTargets();
        MaterialFilterManager.getInstance().clearFilter();
        selectedRenderIds.clear();
        selectedPanelIds.clear();
        searchField.setValue("");
        updateFilteredList("");

        SchematicRenderRefresher.refreshSchematicRendering();
    }

    private void closeScreen() {
        if (this.minecraft != null) this.minecraft.setScreenAndShow(parent);
    }

    private Component getToggleText() {
        return Component.translatable(config.isEnabled() ? "litematicafilter.screen.button.filter.on" : "litematicafilter.screen.button.filter.off");
    }

    private Component getModeText() {
        return Component.translatable("litematicafilter.screen.button.mode",
                Component.translatable("litematicafilter.mode." + config.getMode().name().toLowerCase()));
    }

    private Component getEntityText() {
        return Component.translatable(config.isShowEntities() ? "litematicafilter.screen.button.entities.on" : "litematicafilter.screen.button.entities.off");
    }

    private void updateControlButtons() {
        btnToggleEnabled.setMessage(getToggleText());
        btnToggleMode.setMessage(getModeText());
        btnToggleEntities.setMessage(getEntityText());
    }

    public void renderBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xB2000000);
    }

    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        ctx.fill(listX - 6, 2, listX + listWidth + 6, this.height - 2, COL_PANEL_BG);
        ctx.centeredText(font,
                Component.translatable("litematicafilter.screen.title")
                        .withStyle(net.minecraft.ChatFormatting.BOLD),
                this.width / 2, TITLE_Y, COL_TEXT_MAIN);

        ctx.centeredText(font,
                Component.translatable("litematicafilter.screen.status.counts",
                        selectedPanelIds.size(), selectedRenderIds.size()),
                this.width / 2, STATUS_Y, COL_TEXT_FILTER);

        ctx.centeredText(font,
                Component.translatable("litematicafilter.screen.hint.controls"),
                this.width / 2, HINT_Y, COL_TEXT_DIM);

        ctx.text(font,
                Component.translatable("litematicafilter.screen.counter",
                        filteredEntries.size(), allEntries.size()),
                listX, LIST_START_Y - 6, COL_TEXT_DIM);
        ctx.fill(listX, LIST_START_Y + 4, listX + listWidth, LIST_START_Y + 5, COL_SEPARATOR);

        int listEndY = this.height - BOTTOM_BAR_H + 6;
        renderList(ctx, mx, my, listEndY);

        if (filteredEntries.size() > visibleEntries)
            renderScrollbar(ctx, listX + listWidth + 3, LIST_START_Y, listEndY);

        super.extractRenderState(ctx, mx, my, delta);

        if (allEntries.isEmpty()) {
            ctx.centeredText(font,
                    Component.translatable("litematicafilter.screen.empty.nodata"),
                    this.width / 2, this.height / 2, 0xFF5555FF);
        } else if (filteredEntries.isEmpty()) {
            ctx.centeredText(font,
                    Component.translatable("litematicafilter.screen.empty.noresults",
                            searchField.getValue()),
                    this.width / 2, this.height / 2, COL_TEXT_DIM);
        }
    }

    private void renderList(GuiGraphicsExtractor ctx, int mx, int my, int endY) {
        for (int i = 0; i < visibleEntries; i++) {
            int idx = i + scrollOffset;
            if (idx >= filteredEntries.size()) break;
            MaterialListEntry entry = filteredEntries.get(idx);
            int ey = LIST_START_Y + 7 + i * ENTRY_H;
            if (ey + ENTRY_H > endY) break;
            renderEntry(ctx, entry, listX, ey, mx, my);
        }
    }

    private void renderEntry(GuiGraphicsExtractor ctx, MaterialListEntry entry,
                             int x, int y, int mx, int my) {
        ItemStack stack;
        String blockId;
        String displayName;
        try {
            stack = entry.getStack();
            blockId = getBlockId(stack);
            displayName = stack.getHoverName().getString();
        } catch (Exception e) { return; }

        if (blockId.isEmpty()) return;

        boolean inRender = selectedRenderIds.contains(blockId);
        boolean inPanel = selectedPanelIds.contains(blockId);
        boolean hovered = mx >= x && mx < x + listWidth && my >= y && my < y + ENTRY_H;

        int bg, mark;
        if (inRender && inPanel) {
            bg = COL_ENTRY_BOTH;
            mark = COL_MARK_BOTH;
        } else if (inRender) {
            bg = COL_ENTRY_RENDER;
            mark = COL_MARK_RENDER;
        } else if (inPanel) {
            bg = COL_ENTRY_PANEL;
            mark = COL_MARK_PANEL;
        } else {
            bg = hovered ? COL_ENTRY_HOVER : COL_ENTRY_NORMAL;
            mark = 0;
        }

        ctx.fill(x, y, x + listWidth, y + ENTRY_H - 1, bg);
        if (mark != 0) ctx.fill(x, y, x + 3, y + ENTRY_H - 1, mark);

        ctx.item(stack, x + 3, y + 3);

        int maxNameW = listWidth - 80;
        String nameStr = font.width(displayName) > maxNameW
                ? font.plainSubstrByWidth(displayName, maxNameW - 6) + "…"
                : displayName;
        ctx.text(font, Component.literal(nameStr), x + 23, y + 7, COL_TEXT_MAIN);

        try {
            long total = entry.getCountTotal();
            long avail = entry.getCountAvailable();
            if (total > 0) {
                String cnt = avail + "/" + total;
                int cColor = (avail >= total) ? COL_COUNT_OK : COL_COUNT_MISS;
                ctx.text(font, Component.literal(cnt),
                        x + listWidth - font.width(cnt) - 2, y + 7, cColor);
            }
        } catch (Exception ignored) {}
    }

    private void renderScrollbar(GuiGraphicsExtractor ctx, int x, int startY, int endY) {
        int trackH = endY - startY;
        int thumbH = Math.max(16, trackH * visibleEntries / filteredEntries.size());
        int maxScr = filteredEntries.size() - visibleEntries;
        int thumbY = startY + (maxScr > 0
                ? (int) ((float) scrollOffset / maxScr * (trackH - thumbH)) : 0);
        ctx.fill(x, startY, x + 4, endY, COL_SCROLL_TRACK);
        ctx.fill(x, thumbY, x + 4, thumbY + thumbH, COL_SCROLL_THUMB);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int maxScroll = Math.max(0, filteredEntries.size() - visibleEntries);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) vScroll));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (filteredEntries.size() > visibleEntries) {
            int listEndY = this.height - BOTTOM_BAR_H + 6;
            int scrollX = listX + listWidth + 3;
            int trackH = listEndY - LIST_START_Y;
            int thumbH = Math.max(16, trackH * visibleEntries / filteredEntries.size());
            int maxScr = filteredEntries.size() - visibleEntries;
            int thumbY = LIST_START_Y + (maxScr > 0
                    ? (int) ((float) scrollOffset / maxScr * (trackH - thumbH)) : 0);
            if (mouseX >= scrollX && mouseX <= scrollX + 4
                    && mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                isDraggingScrollbar = true;
                return true;
            }
        }

        int button = click.button();

        if ((button == 0 || button == 1)
                && mouseY >= LIST_START_Y
                && mouseY < this.height - BOTTOM_BAR_H
                && mouseX >= listX
                && mouseX <= listX + listWidth) {

            int clickedRow = (int) ((mouseY - LIST_START_Y - 10) / ENTRY_H);
            int entryIndex = scrollOffset + clickedRow;
            if (entryIndex >= 0 && entryIndex < filteredEntries.size()) {
                MaterialListEntry entry = filteredEntries.get(entryIndex);
                String blockId = getBlockId(entry.getStack());
                if (!blockId.isEmpty()) {
                    Set<String> targetSet = (button == 0) ? selectedPanelIds : selectedRenderIds;
                    if (targetSet.contains(blockId)) {
                        targetSet.remove(blockId);
                    } else {
                        targetSet.add(blockId);
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        boolean handled = super.mouseDragged(click, offsetX, offsetY);
        if (!isDraggingScrollbar) return handled;
        int listEndY = this.height - BOTTOM_BAR_H + 6;
        int trackH = listEndY - LIST_START_Y;
        int thumbH = Math.max(16, trackH * visibleEntries / filteredEntries.size());
        float ratio = (float) (click.y() - LIST_START_Y - (thumbH / 2.0)) / (trackH - thumbH);
        int maxScroll = Math.max(0, filteredEntries.size() - visibleEntries);
        scrollOffset = Math.max(0, Math.min(maxScroll, Math.round(ratio * maxScroll)));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        isDraggingScrollbar = false;
        return super.mouseReleased(click);
    }
}