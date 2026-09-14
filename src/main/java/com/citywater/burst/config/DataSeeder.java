package com.citywater.burst.config;

import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.*;
import com.citywater.burst.service.BurstService;
import com.citywater.burst.service.HospitalSupportService;
import com.citywater.burst.service.IssueService;
import com.citywater.burst.service.RepairService;
import com.citywater.burst.service.SupportService;
import com.citywater.burst.service.YellowWaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 演示数据：账号、管网基础资料，以及三个处于不同阶段的爆管事件，
 * 便于直接演示“报警 -> 评估 -> 派单 -> 抢修 -> 复供 -> 复供后问题”全流程。
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final AppUserRepo userRepo;
    private final ValveZoneRepo zoneRepo;
    private final PipeSegmentRepo pipeRepo;
    private final ValveRepo valveRepo;
    private final FacilityRepo facilityRepo;
    private final BurstEventRepo eventRepo;
    private final PasswordEncoder passwordEncoder;
    private final BurstService burstService;
    private final RepairService repairService;
    private final IssueService issueService;
    private final SupportService supportService;
    private final HospitalSupportService hospitalSupportService;
    private final YellowWaterService yellowWaterService;

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedDemoData || userRepo.count() > 0) {
            return;
        }
        seedUsers();
        seedNetwork();
        seedEvents();
        SecurityContextHolder.clearContext();
    }

    private void seedUsers() {
        user("admin", "admin123", "系统管理员", Role.ADMIN);
        user("dispatcher", "disp123", "调度员-张调度", Role.DISPATCHER);
        user("crew", "crew123", "抢修一队-李队长", Role.CREW);
        user("cs", "cs123", "客服-王客服", Role.CS);
        user("quality", "qua123", "水质检测-刘工", Role.QUALITY);
    }

    private void user(String username, String password, String displayName, Role role) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setDisplayName(displayName);
        u.setRole(role);
        u.setPhone("0571-88000000");
        userRepo.save(u);
    }

    private ValveZone z1, z2, z3;
    private PipeSegment p600, p400, p300;

    private void seedNetwork() {
        z1 = zone("Z-01", "城东供水分区", "中山路、解放路一带，含医院学校与高层小区");
        z2 = zone("Z-02", "城西供水分区", "滨河路沿线，含餐饮美食街");
        z3 = zone("Z-03", "老城区供水分区", "老街片区，管线老化严重");

        p600 = pipe("P-DN600-01", z1, 600, "球墨铸铁", 1998, "中山路");
        pipe("P-DN200-01", z1, 200, "PE", 2015, "解放路");
        p400 = pipe("P-DN400-01", z2, 400, "PE", 2005, "滨河路");
        p300 = pipe("P-DN300-01", z3, 300, "铸铁", 1985, "老街");

        valve("V-101", "中山路与解放路交叉口", z1);
        valve("V-102", "中山路东段", z1);
        valve("V-201", "滨河路北口", z2);
        valve("V-202", "滨河路南口", z2);
        valve("V-301", "老街西口", z3);
        valve("V-302", "老街东口", z3);

        facility("阳光高层小区", FacilityType.COMMUNITY, z1, 3200, true, "物业-王经理", "13800000001", "中山路88号", "32层高层，二次供水水箱2座");
        facility("人民医院", FacilityType.HOSPITAL, z1, 800, false, "后勤-李主任", "13800000002", "中山路120号", "含血液透析中心，重点保障单位");
        facility("实验小学", FacilityType.SCHOOL, z1, 1200, false, "校办-陈老师", "13800000003", "解放路15号", "学校午餐食堂用水");
        facility("中山路", FacilityType.ROAD, z1, 0, false, null, null, null, "城市主干道");
        facility("滨河花园", FacilityType.COMMUNITY, z2, 1800, true, "物业-赵经理", "13800000004", "滨河路200号", "18层小高层");
        facility("滨河美食街", FacilityType.RESTAURANT, z2, 0, false, "商会-周会长", "13800000005", "滨河路150号", "餐饮商户45家");
        facility("滨河路", FacilityType.ROAD, z2, 0, false, null, null, null, "次干道");
        facility("老街社区", FacilityType.COMMUNITY, z3, 950, false, "社区-吴主任", "13800000006", "老街3号", "老旧小区");
        facility("和平路", FacilityType.ROAD, z3, 0, false, null, null, null, "支路");
    }

    private void seedEvents() {
        // 以调度员身份执行业务操作，保证操作人字段真实
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dispatcher", null,
                        List.of(new SimpleGrantedAuthority("ROLE_DISPATCHER"))));

        // ---- 事件一：中山路 DN600 爆管，抢修中（已超 6 小时，触发长时停水保障） ----
        BurstEvent e1 = burstService.create(new EventCreateReq(
                EventSource.SENSOR, "中山路与解放路交叉口",
                "管网压力传感器报警，路面大量涌水，疑似 DN600 主干管爆裂",
                p600.getId(), z1.getId(), "调度中心", "0571-96055"));
        burstService.assess(e1.getId());
        RepairOrder o1 = repairService.dispatch(e1.getId(), new DispatchReq(
                "抢修一队", "李队长", "13900000001",
                null, "挖许字2026-0312", null,
                "中山路双向各封闭一条车道，交警二大队协管",
                "DN600球墨管6米×2、哈夫节×2、消毒剂20kg（库存确认充足）",
                8, "阳光高层小区东门、人民医院急诊楼前",
                null, null, null, null, null));
        repairService.addProgress(o1.getId(), new ProgressReq(RepairStage.ARRIVED, "抢修一队到场，现场涌水点确认"));
        repairService.addProgress(o1.getId(), new ProgressReq(RepairStage.VALVE_CLOSED, "V-101、V-102 已关闭，止水完成"));
        repairService.addProgress(o1.getId(), new ProgressReq(RepairStage.EXCAVATION, "开挖作业中，已探明破裂点位于路口东北侧"));
        // 让事件已持续 8 小时，演示长时停水预警
        e1.setCreatedAt(LocalDateTime.now().minusHours(8));
        eventRepo.save(e1);

        supportService.addWaterPoint(new WaterPointReq(e1.getId(), "阳光高层东门送水点", "阳光高层小区东门", "供水车2辆驻点"));
        supportService.addElderly(new ElderlyReq(e1.getId(), "张奶奶", "阳光高层3栋2单元1801", "13811110001", "志愿者-小王", "行动不便，需送水上楼"));
        supportService.addLoss(new MerchantLossReq(e1.getId(), "中山路洗车行", "洗车", new BigDecimal("3000"), "停水无法营业一天"));
        supportService.addTank(new TankReq(e1.getId(), "阳光高层小区", "3栋水箱", 15, TankStatus.LOW, "停水期间水箱即将抽空"));

        // 医院应急供水保障（E1 影响人民医院，派单时自动建单）：已调度供水车，
        // 供水车无法进院改设水点+志愿者送水，目前已供水到位待医院确认
        HospitalSupport hs1 = hospitalSupportService.list(e1.getId()).stream().findFirst().orElseThrow();
        hospitalSupportService.dispatchSupply(hs1.getId(), new HospitalDispatchReq(
                "供水车2辆（浙A·D1234、浙A·D5678）", "5m³临时水箱×2（住院楼前）"));
        hospitalSupportService.recordAccessIssue(hs1.getId(), new AccessIssueReq(
                "医院东门对面人行道临时水点", "志愿者3人轮班送水至住院楼、透析中心"));
        hospitalSupportService.markSupplied(hs1.getId(), new HospitalSupplyReq(12.5, null));

        // ---- 事件二：滨河路居民报修，待评估 ----
        burstService.create(new EventCreateReq(
                EventSource.RESIDENT, "滨河路150号美食街口",
                "居民反映路面渗水，井盖冒水，附近美食街商户用水变小",
                p400.getId(), z2.getId(), "市民刘先生", "13822220002"));

        // ---- 事件三：老街 DN300 爆管，已复供，演示复供后问题闭环 ----
        BurstEvent e3 = burstService.create(new EventCreateReq(
                EventSource.INSPECTOR, "老街中段",
                "巡检发现老街中段路面塌陷渗水，判断 DN300 铸铁管破裂",
                p300.getId(), z3.getId(), "巡检员-老周", "13833330003"));
        burstService.assess(e3.getId());
        RepairOrder o3 = repairService.dispatch(e3.getId(), new DispatchReq(
                "抢修二队", "赵队长", "13900000002",
                null, "挖许字2026-0305", null, null,
                "DN300铸铁管6米×1、管件一批",
                5, "老街社区活动中心",
                null, null, null, null, null));
        repairService.addProgress(o3.getId(), new ProgressReq(RepairStage.ARRIVED, "到场确认漏点"));
        repairService.addProgress(o3.getId(), new ProgressReq(RepairStage.VALVE_CLOSED, "V-301、V-302 已关闭"));
        repairService.addProgress(o3.getId(), new ProgressReq(RepairStage.EXCAVATION, "开挖完成"));
        repairService.addProgress(o3.getId(), new ProgressReq(RepairStage.PIPE_REPLACED, "更换 DN300 管段 6 米"));
        repairService.addProgress(o3.getId(), new ProgressReq(RepairStage.FLUSHING, "管道冲洗完成"));
        repairService.addProgress(o3.getId(), new ProgressReq(RepairStage.DISINFECTION, "管道消毒完成"));
        repairService.addProgress(o3.getId(), new ProgressReq(RepairStage.PRESSURE_TEST, "压力测试 0.6MPa 稳压 30 分钟合格"));
        repairService.addProgress(o3.getId(), new ProgressReq(RepairStage.ROAD_RESTORED, "路面回填并恢复沥青"));
        repairService.upsertCheck(o3.getId(), new RestoreCheckReq(true, true, true, true, 0.8, 0.3, "浊度0.8NTU、余氯0.3mg/L，符合复供标准"));
        repairService.confirmRestore(o3.getId());
        issueService.create(new IssueCreateReq(o3.getId(), IssueType.YELLOW_WATER,
                "老街社区3栋居民反映复供后水龙头出黄水", "居民-陈阿姨", "13844440004"));

        // 复供黄水投诉处理（E3 老街社区）：1 件已办结，2 件高层集中投诉触发物业提示与重点观察
        YellowWaterCase yw1 = yellowWaterService.create(new YellowWaterCreateReq(
                o3.getId(), ComplaintType.YELLOW_WATER, "老街社区", "3栋2单元", 6,
                "陈阿姨", "13844440004", "复供后早上水龙头出黄水，约2分钟后变清",
                "3栋2单元601厨房水龙头", "https://img.example.com/yw/3-601-1.jpg", null));
        yellowWaterService.handle(yw1.getId(), new YellowWaterHandleReq(
                YwMethod.EXPLAIN_DISCHARGE, "管网残留所致，指导居民短时排放后水已清澈，电话回访确认",
                Responsibility.WATER_COMPANY, null));
        yellowWaterService.create(new YellowWaterCreateReq(
                o3.getId(), ComplaintType.YELLOW_WATER, "老街社区", "5栋", 18,
                "周先生", "13855550005", "高层住户持续黄水，放水半小时仍发黄",
                "5栋1503厨房水龙头", "https://img.example.com/yw/5-1503-1.jpg,https://img.example.com/yw/5-1503-2.jpg", null));
        yellowWaterService.create(new YellowWaterCreateReq(
                o3.getId(), ComplaintType.ODOR, "老街社区", "6栋", 18,
                "吴女士", "13866660006", "自来水有异味，疑似二次供水水箱污染",
                "6栋1202厨房水龙头", null, null));
    }

    private ValveZone zone(String code, String name, String desc) {
        ValveZone z = new ValveZone();
        z.setCode(code);
        z.setName(name);
        z.setDescription(desc);
        return zoneRepo.save(z);
    }

    private PipeSegment pipe(String code, ValveZone zone, int diameter, String material, int year, String road) {
        PipeSegment p = new PipeSegment();
        p.setCode(code);
        p.setZone(zone);
        p.setDiameterMm(diameter);
        p.setMaterial(material);
        p.setInstallYear(year);
        p.setRoadName(road);
        return pipeRepo.save(p);
    }

    private void valve(String code, String location, ValveZone zone) {
        Valve v = new Valve();
        v.setCode(code);
        v.setLocation(location);
        v.setZone(zone);
        valveRepo.save(v);
    }

    private void facility(String name, FacilityType type, ValveZone zone, int population, boolean highRise,
                          String contactName, String contactPhone, String address, String note) {
        Facility f = new Facility();
        f.setName(name);
        f.setType(type);
        f.setZone(zone);
        f.setPopulation(population);
        f.setHighRise(highRise);
        f.setContactName(contactName);
        f.setContactPhone(contactPhone);
        f.setAddress(address);
        f.setNote(note);
        facilityRepo.save(f);
    }
}
