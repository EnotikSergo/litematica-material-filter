package com.enotiksergo.litematicafilter.config;

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

    private Set<String> panelTargets = new HashSet<>();

    private boolean showEntities = true;

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

    public Set<String> getRenderTargets() { return renderTargets; }
    public void setRenderTargets(Set<String> targets) {
        this.renderTargets.clear();
        for (String t : targets) this.renderTargets.add(t.toLowerCase().trim());
        save();
    }
    public void clearRenderTargets() { this.renderTargets.clear(); save(); }

    public Set<String> getPanelTargets() { return panelTargets; }
    public void setPanelTargets(Set<String> targets) {
        this.panelTargets.clear();
        for (String t : targets) this.panelTargets.add(t.toLowerCase().trim());
        save();
    }
    public void clearPanelTargets() { this.panelTargets.clear(); save(); }

    public boolean isShowEntities() { return showEntities; }
    public void setShowEntities(boolean showEntities) { this.showEntities = showEntities; save(); }
    public void toggleShowEntities() { setShowEntities(!this.showEntities); }

    public boolean shouldShow(String blockId) {
        if (!enabled) return true;
        if (renderTargets.isEmpty()) return mode == FilterMode.BLACKLIST;
        boolean contains = renderTargets.contains(blockId.toLowerCase().trim());
        return mode == FilterMode.WHITELIST ? contains : !contains;
    }

    public boolean shouldShowInPanel(String blockId) {
        if (panelTargets.isEmpty()) return true;
        return panelTargets.contains(blockId.toLowerCase().trim());
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
                this.panelTargets = loaded.panelTargets != null ? loaded.panelTargets : new HashSet<>();
                this.showEntities = loaded.showEntities;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public void save() {
        File file = CONFIG_PATH.toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}