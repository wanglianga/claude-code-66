package com.citywater.burst.model;

public enum SubsidenceStatus {
    OPEN("待复查"),
    RECHECKED("已复查");

    private final String label;

    SubsidenceStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
