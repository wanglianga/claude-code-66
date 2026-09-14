package com.citywater.burst.model;

public enum WaterPointStatus {
    OPEN("开放中"),
    CROWDED("排队拥挤"),
    CLOSED("已撤点");

    private final String label;

    WaterPointStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
