package com.citywater.burst.model;

/**
 * 责任方：供水公司与物业责任分开记录。
 */
public enum Responsibility {
    UNDETERMINED("待判定"),
    WATER_COMPANY("供水公司"),
    PROPERTY("物业");

    private final String label;

    Responsibility(String label) { this.label = label; }

    public String getLabel() { return label; }
}
