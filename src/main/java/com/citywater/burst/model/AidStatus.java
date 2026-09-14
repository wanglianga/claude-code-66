package com.citywater.burst.model;

public enum AidStatus {
    PENDING("待送水"),
    DELIVERING("送水中"),
    DONE("已送达");

    private final String label;

    AidStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
