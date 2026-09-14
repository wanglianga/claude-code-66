package com.citywater.burst.model;

public enum YwMethod {
    SECOND_FLUSH("二次冲洗"),
    DOOR_SAMPLE("上门取样"),
    EXPLAIN_DISCHARGE("解释短时排放");

    private final String label;

    YwMethod(String label) { this.label = label; }

    public String getLabel() { return label; }
}
