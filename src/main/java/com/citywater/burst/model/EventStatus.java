package com.citywater.burst.model;

public enum EventStatus {
    REPORTED("已报警"),
    ASSESSED("已评估"),
    DISPATCHED("已派单"),
    REPAIRING("抢修中"),
    RESTORE_CHECK("复供确认中"),
    RESTORED("已复供"),
    CLOSED("已关闭");

    private final String label;

    EventStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
