package com.citywater.burst.model;

public enum FacilityType {
    COMMUNITY("居民小区"),
    HOSPITAL("医院"),
    SCHOOL("学校"),
    RESTAURANT("餐饮街"),
    ROAD("道路"),
    COMMERCIAL("商业体");

    private final String label;

    FacilityType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
