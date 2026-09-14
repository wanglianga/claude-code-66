package com.citywater.burst.model;

/**
 * 抢修现场阶段：到场 -> 关阀 -> 开挖 -> 换管 -> 冲洗 -> 消毒 -> 压力测试 -> 道路恢复 -> 完成。
 * 阶段只能向前推进，不允许回退。
 */
public enum RepairStage {
    DISPATCHED("已派单", 0),
    ARRIVED("到达现场", 1),
    VALVE_CLOSED("关阀止水", 2),
    EXCAVATION("开挖作业", 3),
    PIPE_REPLACED("换管完成", 4),
    FLUSHING("管道冲洗", 5),
    DISINFECTION("管道消毒", 6),
    PRESSURE_TEST("压力测试", 7),
    ROAD_RESTORED("道路恢复", 8),
    COMPLETED("抢修完成", 9);

    private final String label;
    private final int order;

    RepairStage(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() { return label; }

    public int getOrder() { return order; }
}
