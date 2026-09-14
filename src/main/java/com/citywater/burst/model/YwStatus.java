package com.citywater.burst.model;

public enum YwStatus {
    OPEN("待处理"),
    FOLLOW_UP("待回访"),
    DONE("已办结");

    private final String label;

    YwStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
