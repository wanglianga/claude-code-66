package com.citywater.burst.model;

public enum Severity {
    MINOR("轻微"),
    MODERATE("一般"),
    MAJOR("严重"),
    CRITICAL("特别重大");

    private final String label;

    Severity(String label) { this.label = label; }

    public String getLabel() { return label; }
}
