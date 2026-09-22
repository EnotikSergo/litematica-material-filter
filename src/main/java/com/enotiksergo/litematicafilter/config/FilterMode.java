package com.enotiksergo.litematicafilter.config;

public enum FilterMode {
    WHITELIST("Whitelist"),
    BLACKLIST("Blacklist");

    private final String displayName;
    FilterMode(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return this.displayName; }

    public FilterMode next() {
        FilterMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}