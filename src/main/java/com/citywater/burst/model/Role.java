package com.citywater.burst.model;

public enum Role {
    ADMIN("系统管理员"),
    DISPATCHER("调度员"),
    CREW("抢修队"),
    CS("客服"),
    QUALITY("水质检测员");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }
}
