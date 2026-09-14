package com.citywater.burst.model;

public enum WatchStatus {
    WATCHING("观察中"),
    CLEARED("已解除");

    private final String label;

    WatchStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
