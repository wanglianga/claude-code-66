package com.citywater.burst.model;

public enum HospitalSupportStatus {
    PENDING("待调度"),
    DELIVERING("供水保障中"),
    SUPPLIED("供水已到位"),
    CONFIRMED("医院已确认");

    private final String label;

    HospitalSupportStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
