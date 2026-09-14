package com.citywater.burst.model;

public enum EventSource {
    SENSOR("管网传感器"),
    RESIDENT("居民报修"),
    INSPECTOR("巡检员");

    private final String label;

    EventSource(String label) { this.label = label; }

    public String getLabel() { return label; }
}
