package com.enotiksergo.litematicafilter.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class MaterialFilterManager {

    private static final MaterialFilterManager INSTANCE = new MaterialFilterManager();
    private static final Logger LOGGER = LoggerFactory.getLogger("litematicafilter");

    private boolean filterActive = false;
    private String filterSearchText = "";

    private final Set<String> activeFilterIds = new LinkedHashSet<>();

    private MaterialFilterManager() {
    }

    public static MaterialFilterManager getInstance() {
        return INSTANCE;
    }

    public boolean isFilterActive() {
        return filterActive;
    }

    public String getFilterSearchText() {
        return filterSearchText;
    }

    public Set<String> getActiveFilterIds() {
        return Collections.unmodifiableSet(activeFilterIds);
    }

    public void setActiveFilter(String searchText, Set<String> shownItemIds) {
        this.filterSearchText = searchText;
        this.activeFilterIds.clear();
        this.activeFilterIds.addAll(shownItemIds);
        this.filterActive = !shownItemIds.isEmpty();
        LOGGER.info("[LitematicaFilter] Filter set: {} items visible", shownItemIds.size());
    }

    public void clearFilter() {
        this.filterSearchText = "";
        this.activeFilterIds.clear();
        this.filterActive = false;
        LOGGER.info("[LitematicaFilter] Filter cleared.");
    }
}
