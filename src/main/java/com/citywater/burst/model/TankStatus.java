package com.citywater.burst.model;

public enum TankStatus {
    NORMAL("水位正常"),
    LOW("水位偏低"),
    EMPTY("已抽空"),
    REFILLED("已补水");

    private final String label;

    TankStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
