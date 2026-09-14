package com.citywater.burst.model;

public enum LossStatus {
    REPORTED("已申报"),
    REVIEWING("审核中"),
    SETTLED("已赔付");

    private final String label;

    LossStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
