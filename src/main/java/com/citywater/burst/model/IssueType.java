package com.citywater.burst.model;

public enum IssueType {
    YELLOW_WATER("黄水投诉"),
    SECONDARY_LEAK("二次漏水"),
    ROAD_SUBSIDENCE("道路沉降"),
    COMPENSATION("赔付申请");

    private final String label;

    IssueType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
