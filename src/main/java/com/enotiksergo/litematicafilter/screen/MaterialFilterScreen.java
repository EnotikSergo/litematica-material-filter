package com.enotiksergo.litematicafilter.screen;

import com.enotiksergo.litematicafilter.LitematicaFilterMod;
import com.enotiksergo.litematicafilter.filter.MaterialFilterManager;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

public class MaterialFilterScreen extends Screen {

    private static final int TITLE_Y = 8;
    private static final int STATUS_Y = 19;
    private static final int SEARCH_Y = 32;
    private static final int SEARCH_H = 20;
    private static final int LIST_START_Y = 60;
    private static final int ENTRY_H = 22;
    private static final int BOTTOM_BAR_H = 28;
    private static final int BTN_H = 20;

    private static final int COL_PANEL_BG = 0xCC000000;
    private static final int COL_SEPARATOR = 0x80FFFFFF;
    private static final int COL_ENTRY_NORMAL = 0x20FFFFFF;
    private static final int COL_ENTRY_HOVER = 0x40FFFFFF;
    private static final int COL_ENTRY_ACTIVE = 0x4055AA55;
    private static final int COL_ACTIVE_MARK = 0xFF55FF55;
    private static final int COL_COUNT_OK = 0xFF55FF55;
    private static final int COL_COUNT_MISS = 0xFFFFAA00;
    private static final int COL_TEXT_MAIN = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFFAAAAAA;
    private static final int COL_TEXT_FILTER = 0xFF55FF55;
    private static final int COL_SCROLL_TRACK = 0x40FFFFFF;
    private static final int COL_SCROLL_THUMB = 0xAAFFFFFF;

    private final Set<String> selectedItemIds = new HashSet<>();

    private final Screen parent;
    private TextFieldWidget searchField;
    private int scrollOffset = 0;
    private int visibleEntries = 0;
    private int listWidth = 420;
    private int listX = 0;

    private List<MaterialListEntry> allEntries = new ArrayList<>();
    private List<MaterialListEntry> filteredEntries = new ArrayList<>();
    private boolean isDraggingScrollbar = false;

    public MaterialFilterScreen(Screen parent) {
        super(Text.translatable("litematicafilter.screen.title"));
        this.parent = parent;
        this.selectedItemIds.addAll(MaterialFilterManager.getInstance().getActiveFilterIds());
    }

    @Override
    protected void init() {
        super.init();

        listWidth = Math.min(460, this.width - 40);
        listX = (this.width - listWidth) / 2;

        loadMaterialsFromLitematica();

        int searchW = listWidth - 115;

        searchField = new TextFieldWidget(
                this.textRenderer,
                listX, SEARCH_Y,
                searchW, SEARCH_H,
                Text.translatable("litematicafilter.screen.search.narration")
        );
        searchField.setPlaceholder(Text.translatable("litematicafilter.screen.search.placeholder"));
        searchField.setMaxLength(64);
        searchField.setChangedListener(this::onSearchChanged);

        this.addDrawableChild(searchField);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("litematicafilter.screen.button.apply"),
                btn -> applyFilter()
        ).dimensions(listX + searchW + 5, SEARCH_Y, 110, BTN_H).build());

        int bottomY = this.height - BOTTOM_BAR_H;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("litematicafilter.screen.button.clear"),
                btn -> clearFilter()
        ).dimensions(listX, bottomY, 110, BTN_H).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("litematicafilter.screen.button.close"),
                btn -> closeScreen()
        ).dimensions(listX + listWidth - 70, bottomY, 70, BTN_H).build());

        int listEndY = this.height - BOTTOM_BAR_H - 6;
        int listAreaH = listEndY - LIST_START_Y;
        this.visibleEntries = Math.max(1, listAreaH / ENTRY_H);

        updateFilteredList(searchField.getText());
    }

    private void loadMaterialsFromLitematica() {
        allEntries.clear();

        MaterialListBase matList = DataManager.getMaterialList();

        if (matList == null) {
            LitematicaFilterMod.LOGGER.warn(
                    "[LitematicaFilter] DataManager.getMaterialList() returned null. ");
            return;
        }

        allEntries.addAll(matList.getMaterialsAll());

        LitematicaFilterMod.LOGGER.info(
                "[LitematicaFilter] Loaded {} entries from MaterialListBase.getMaterialsAll().",
                allEntries.size());
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
            String id = Registries.ITEM.getId(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            return id.contains(query) || name.contains(query);
        } catch (Exception e) {
            return false;
        }
    }

    private void applyFilter() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList == null) {
            LitematicaFilterMod.LOGGER.warn("[LitematicaFilter] No active material list");
            return;
        }

        String searchText = searchField.getText().trim();

        Set<String> matchingIds = new LinkedHashSet<>(selectedItemIds);

        if (!searchText.isEmpty()) {
            for (MaterialListEntry entry : filteredEntries) {
                try {
                    matchingIds.add(Registries.ITEM.getId(entry.getStack().getItem()).toString());
                } catch (Exception ignored) {
                }
            }
        }

        if (matchingIds.isEmpty()) {
            clearFilter();
            return;
        }

        matList.clearIgnored();

        int ignoredCount = 0;
        for (MaterialListEntry entry : new ArrayList<>(matList.getMaterialsAll())) {
            try {
                String id = Registries.ITEM.getId(entry.getStack().getItem()).toString();
                if (!matchingIds.contains(id)) {
                    matList.ignoreEntry(entry);
                    ignoredCount++;
                }
            } catch (Exception e) {
                LitematicaFilterMod.LOGGER.debug("[LitematicaFilter] ignoreEntry error: {}", e.getMessage());
            }
        }

        MaterialFilterManager.getInstance().setActiveFilter(searchText, matchingIds);

        selectedItemIds.clear();
        selectedItemIds.addAll(matchingIds);

        LitematicaFilterMod.LOGGER.info(
                "[LitematicaFilter] Active filter : {} visible, {} invisible",
                 matchingIds.size(), ignoredCount);
    }

    private void clearFilter() {
        MaterialListBase matList = DataManager.getMaterialList();
        if (matList != null) {
            matList.clearIgnored();
        }
        MaterialFilterManager.getInstance().clearFilter();

        selectedItemIds.clear();

        searchField.setText("");
        updateFilteredList("");
    }

    private void closeScreen() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xB2000000);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);

        ctx.fill(listX - 6, 2, listX + listWidth + 6, this.height - 2, COL_PANEL_BG);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("litematicafilter.screen.title").formatted(net.minecraft.util.Formatting.BOLD),
                this.width / 2, TITLE_Y, COL_TEXT_MAIN);

        MaterialFilterManager mgr = MaterialFilterManager.getInstance();
        if (mgr.isFilterActive()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("litematicafilter.screen.status.active",
                            mgr.getActiveFilterIds().size()),
                    this.width / 2, STATUS_Y, COL_TEXT_FILTER);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("litematicafilter.screen.status.inactive"),
                    this.width / 2, STATUS_Y, COL_TEXT_DIM);
        }

        ctx.drawTextWithShadow(textRenderer,
                Text.translatable("litematicafilter.screen.counter",
                        filteredEntries.size(), allEntries.size()),
                listX, LIST_START_Y - 6, COL_TEXT_DIM);

        ctx.fill(listX, LIST_START_Y + 4, listX + listWidth, LIST_START_Y + 5, COL_SEPARATOR);

        int listEndY = this.height - BOTTOM_BAR_H + 6;
        renderList(ctx, mx, my, listEndY, mgr);

        if (filteredEntries.size() > visibleEntries) {
            renderScrollbar(ctx, listX + listWidth + 3, LIST_START_Y, listEndY);
        }

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable(mgr.isFilterActive()
                        ? "litematicafilter.screen.hint.active"
                        : "litematicafilter.screen.hint.inactive"),
                this.width / 2, this.height - BOTTOM_BAR_H - 13, COL_TEXT_MAIN);

        super.render(ctx, mx, my, delta);

        if (allEntries.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("litematicafilter.screen.empty.nodata"),
                    this.width / 2, this.height / 2, 0xFF5555FF);
        } else if (filteredEntries.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("litematicafilter.screen.empty.noresults", searchField.getText()),
                    this.width / 2, this.height / 2, COL_TEXT_DIM);
        }
    }

    private void renderList(DrawContext ctx, int mx, int my, int endY, MaterialFilterManager mgr) {
        for (int i = 0; i < visibleEntries; i++) {
            int idx = i + scrollOffset;
            if (idx >= filteredEntries.size()) break;

            MaterialListEntry entry = filteredEntries.get(idx);
            int ey = LIST_START_Y + 7 + i * ENTRY_H;
            if (ey + ENTRY_H > endY) break;

            renderEntry(ctx, entry, listX, ey, mx, my, mgr);
        }
    }

    private void renderEntry(DrawContext ctx, MaterialListEntry entry, int x, int y, int mx, int my, MaterialFilterManager mgr) {
        ItemStack stack;
        String itemId;
        String displayName;
        try {
            stack = entry.getStack();
            itemId = Registries.ITEM.getId(stack.getItem()).toString();
            displayName = stack.getName().getString();
        } catch (Exception e) {
            return;
        }

        boolean inFilter = selectedItemIds.contains(itemId);
        boolean hovered = mx >= x && mx < x + listWidth && my >= y && my < y + ENTRY_H;

        int bg = inFilter ? COL_ENTRY_ACTIVE : (hovered ? COL_ENTRY_HOVER : COL_ENTRY_NORMAL);
        ctx.fill(x, y, x + listWidth, y + ENTRY_H - 1, bg);

        if (inFilter) ctx.fill(x, y, x + 3, y + ENTRY_H - 1, COL_ACTIVE_MARK);

        ctx.drawItem(stack, x + 3, y + 3);

        int maxNameW = listWidth - 80;
        String nameStr = textRenderer.getWidth(displayName) > maxNameW
                ? textRenderer.trimToWidth(displayName, maxNameW - 6) + "…"
                : displayName;
        ctx.drawTextWithShadow(textRenderer, Text.literal(nameStr), x + 23, y + 7, COL_TEXT_MAIN);

        try {
            long total = entry.getCountTotal();
            long avail = entry.getCountAvailable();
            if (total > 0) {
                String cnt = avail + "/" + total;
                int cColor = (avail >= total) ? COL_COUNT_OK : COL_COUNT_MISS;
                ctx.drawTextWithShadow(textRenderer, Text.literal(cnt),
                        x + listWidth - textRenderer.getWidth(cnt) - 2, y + 7, cColor);
            }
        } catch (Exception ignored) {
        }
    }

    private void renderScrollbar(DrawContext ctx, int x, int startY, int endY) {
        int trackH = endY - startY;
        int thumbH = Math.max(16, trackH * visibleEntries / filteredEntries.size());
        int maxScr = filteredEntries.size() - visibleEntries;
        int thumbY = startY + (maxScr > 0
                ? (int) ((float) scrollOffset / maxScr * (trackH - thumbH))
                : 0);

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
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (filteredEntries.size() > visibleEntries) {
            int listEndY = this.height - BOTTOM_BAR_H + 6;
            int scrollX = listX + listWidth + 3;
            int trackH = listEndY - LIST_START_Y;
            int thumbH = Math.max(16, trackH * visibleEntries / filteredEntries.size());
            int maxScr = filteredEntries.size() - visibleEntries;
            int thumbY = LIST_START_Y + (maxScr > 0 ? (int) ((float) scrollOffset / maxScr * (trackH - thumbH)) : 0);

            if (mouseX >= scrollX && mouseX <= scrollX + 4 && mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                isDraggingScrollbar = true;
                return true;
            }
        }
        int button = click.button();

        if (button == 0 && mouseY >= LIST_START_Y && mouseY < this.height - BOTTOM_BAR_H) {

            int listWidth = Math.min(460, this.width - 40);
            int listX = (this.width - listWidth) / 2;

            if (mouseX >= listX && mouseX <= listX + listWidth) {

                int clickedRow = (int) ((mouseY - LIST_START_Y - 10) / ENTRY_H);
                int entryIndex = scrollOffset + clickedRow;

                if (entryIndex >= 0 && entryIndex < filteredEntries.size()) {
                    MaterialListEntry entry = filteredEntries.get(entryIndex);

                    try {
                        String itemId = Registries.ITEM.getId(entry.getStack().getItem()).toString();

                        if (selectedItemIds.contains(itemId)) {
                            selectedItemIds.remove(itemId);
                        } else {
                            selectedItemIds.add(itemId);
                        }
                    } catch (Exception ignored) {
                    }

                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        boolean handled = super.mouseDragged(click, offsetX, offsetY);

        if (!isDraggingScrollbar) {
            return handled;
        }

        int listEndY = this.height - BOTTOM_BAR_H + 6;
        int trackH = listEndY - LIST_START_Y;
        int thumbH = Math.max(16, trackH * visibleEntries / filteredEntries.size());

        float ratio = (float) (click.y() - LIST_START_Y - (thumbH / 2.0)) / (trackH - thumbH);
        int maxScroll = Math.max(0, filteredEntries.size() - visibleEntries);
        scrollOffset = Math.max(0, Math.min(maxScroll, Math.round(ratio * maxScroll)));

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        isDraggingScrollbar = false;
        return super.mouseReleased(click);
    }
}
