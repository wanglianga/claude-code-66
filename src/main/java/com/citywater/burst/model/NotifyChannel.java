package com.citywater.burst.model;

public enum NotifyChannel {
    SMS("短信"),
    PHONE("电话"),
    PROPERTY_GROUP("物业群"),
    SCHOOL_CONTACT("学校联系人"),
    KEY_USER("重点用户");

    private final String label;

    NotifyChannel(String label) { this.label = label; }

    public String getLabel() { return label; }
}
