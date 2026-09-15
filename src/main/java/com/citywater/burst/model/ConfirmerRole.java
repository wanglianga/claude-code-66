package com.citywater.burst.model;

/**
 * 道路验收确认方。
 */
public enum ConfirmerRole {
    SITE_LEADER("现场负责人"),
    CITY_MGMT("城管"),
    ROAD_AUTHORITY("道路单位");

    private final String label;

    ConfirmerRole(String label) { this.label = label; }

    public String getLabel() { return label; }
}
