package com.citywater.burst.model;

public enum ComplaintType {
    YELLOW_WATER("黄水"),
    ODOR("异味");

    private final String label;

    ComplaintType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
