package com.citywater.burst.model;

public enum RoadStatus {
    PENDING("待提交验收"),
    SUBMITTED("待确认"),
    ACCEPTED("验收通过"),
    REJECTED("验收不通过");

    private final String label;

    RoadStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
