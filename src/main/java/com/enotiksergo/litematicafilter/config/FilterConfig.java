package com.enotiksergo.litematicafilter.config;

import com.enotiksergo.litematicafilter.hud.MaterialHudRenderer;
import com.enotiksergo.litematicafilter.hud.RawHudRenderer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class FilterConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("LitematicaFilter/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("litematicafilter.json");
    private static FilterConfig INSTANCE;

    private boolean enabled = true;
    private FilterMode mode = FilterMode.WHITELIST;
    private Set<String> renderTargets = new HashSet<>();
    private Set<String> materialTargets = new HashSet<>();
    private boolean showEntities = true;
    private boolean showRawHud = false;
    private boolean showMaterialHud = false;
    private Set<String> expandedItems = new HashSet<>();

    public static FilterConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FilterConfig();
            INSTANCE.load();
        }
        return INSTANCE;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; save(); }
    public void toggleEnabled() { setEnabled(!this.enabled); }

    public FilterMode getMode() { return mode; }
    public void setMode(FilterMode mode) { this.mode = mode; save(); }

    public boolean isShowEntities() { return showEntities; }
    public void toggleShowEntities() { setShowEntities(!this.showEntities); }
    public void setShowEntities(boolean v) { this.showEntities = v; save(); }

    public boolean isShowRawHud() { return showRawHud; }
    public void toggleShowRawHud() { setShowRawHud(!this.showRawHud); }
    public void setShowRawHud(boolean v) { this.showRawHud = v; save(); }

    public boolean isShowMaterialHud() { return showMaterialHud; }
    public void setShowMaterialHud(boolean v) { this.showMaterialHud = v; save(); MaterialHudRenderer.invalidateCache(); }
    public void toggleShowMaterialHud() { setShowMaterialHud(!this.showMaterialHud); }

    public Set<String> getRenderTargets() { return renderTargets; }
    public void setRenderTargets(Set<String> targets) {
        this.renderTargets.clear();
        for (String t : targets) this.renderTargets.add(t.toLowerCase().trim());
        save(); RawHudRenderer.invalidateCache(); MaterialHudRenderer.invalidateCache();
    }
    public void clearRenderTargets() { this.renderTargets.clear(); save(); RawHudRenderer.invalidateCache(); MaterialHudRenderer.invalidateCache(); }

    public Set<String> getMaterialTargets() { return materialTargets; }
    public void setMaterialTargets(Set<String> targets) {
        this.materialTargets.clear();
        for (String t : targets) this.materialTargets.add(t.toLowerCase().trim());
        save(); RawHudRenderer.invalidateCache(); MaterialHudRenderer.invalidateCache();
    }
    public void clearMaterialTargets() { this.materialTargets.clear(); save(); RawHudRenderer.invalidateCache(); MaterialHudRenderer.invalidateCache(); }

    public Set<String> getExpandedItems() { return expandedItems; }
    public boolean isExpanded(String itemId) { return expandedItems.contains(itemId.toLowerCase().trim()); }
    public void toggleExpanded(String itemId) {
        String id = itemId.toLowerCase().trim();
        if (expandedItems.contains(id)) expandedItems.remove(id);
        else expandedItems.add(id);
        save(); RawHudRenderer.invalidateCache();
    }
    public void clearExpanded() { this.expandedItems.clear(); save(); RawHudRenderer.invalidateCache(); }

    public void expandAll(Set<String> ids) {
        boolean changed = false;
        for (String id : ids) {
            if (this.expandedItems.add(id.toLowerCase().trim())) changed = true;
        }
        if (changed) { save(); RawHudRenderer.invalidateCache(); }
    }

    public boolean shouldShow(String blockId) {
        if (!enabled) return false;
        if (renderTargets.isEmpty()) return mode != FilterMode.BLACKLIST;
        boolean contains = renderTargets.contains(blockId.toLowerCase().trim());
        return (mode == FilterMode.WHITELIST) != contains;
    }

    public boolean shouldShowInMaterial(String blockId) {
        if (materialTargets.isEmpty()) return true;
        return materialTargets.contains(blockId.toLowerCase().trim());
    }

    public void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) { save(); return; }
        try (FileReader reader = new FileReader(file)) {
            FilterConfig loaded = GSON.fromJson(reader, FilterConfig.class);
            if (loaded != null) {
                this.enabled = loaded.enabled;
                this.mode = loaded.mode != null ? loaded.mode : FilterMode.WHITELIST;
                this.renderTargets = loaded.renderTargets != null ? loaded.renderTargets : new HashSet<>();
                this.materialTargets = loaded.materialTargets != null ? loaded.materialTargets : new HashSet<>();
                this.showEntities = loaded.showEntities;
                this.showRawHud = loaded.showRawHud;
                this.showMaterialHud = loaded.showMaterialHud;
                this.expandedItems = loaded.expandedItems != null ? loaded.expandedItems : new HashSet<>();
            }
        } catch (IOException e) { LOGGER.error("Failed to load config", e); }
    }

    public void save() {
        File file = CONFIG_PATH.toFile();
        try (FileWriter writer = new FileWriter(file)) { GSON.toJson(this, writer); }
        catch (IOException e) { LOGGER.error("Failed to save config", e); }
    }
}