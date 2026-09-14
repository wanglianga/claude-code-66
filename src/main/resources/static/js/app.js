/* 城市供水爆管抢修指挥平台 - 前端逻辑 */
'use strict';

let ME = null;
let ENUMS = {};
let ZONES = [], PIPES = [], FACILITIES = [];
let CURRENT_TAB = 'dashboard';

/* ---------------- 基础工具 ---------------- */
const $ = s => document.querySelector(s);

function esc(s) {
    if (s === null || s === undefined) return '';
    return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function fmt(t) { return t ? String(t).substring(0, 16) : '-'; }

function label(v) {
    if (!v) return '-';
    for (const k in ENUMS) { if (ENUMS[k] && ENUMS[k][v]) return ENUMS[k][v]; }
    return v;
}

function badge(v) {
    if (!v) return '-';
    return `<span class="badge ${esc(v)}">${esc(label(v))}</span>`;
}

function toast(msg, isErr) {
    const t = $('#toast');
    t.textContent = msg;
    t.className = 'toast' + (isErr ? ' err' : '');
    t.style.display = 'block';
    setTimeout(() => t.style.display = 'none', 3200);
}

async function api(path, opts = {}) {
    const res = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...opts
    });
    if (res.status === 401) { location.href = '/login.html'; throw new Error('未登录'); }
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }
    if (!res.ok) {
        const msg = (data && data.error) ? data.error : ('请求失败 ' + res.status);
        throw new Error(msg);
    }
    return data;
}

async function post(path, body) {
    return api(path, { method: 'POST', body: body === undefined ? null : JSON.stringify(body) });
}

function openModal(title, html) {
    $('#modalTitle').textContent = title;
    $('#modalBody').innerHTML = html;
    $('#modalMask').style.display = 'flex';
}
function closeModal() { $('#modalMask').style.display = 'none'; }

function can(...roles) { return ME && roles.includes(ME.role); }
const canOps = () => can('DISPATCHER', 'ADMIN');          // 调度操作
const canCrew = () => can('CREW', 'DISPATCHER', 'ADMIN'); // 现场进度
const canQuality = () => can('QUALITY', 'DISPATCHER', 'ADMIN'); // 复供确认单
const canCs = () => can('CS', 'DISPATCHER', 'ADMIN');     // 客服/保障

function formData(formId) {
    const fd = new FormData(document.getElementById(formId));
    const o = {};
    for (const [k, v] of fd.entries()) o[k] = v === '' ? null : v;
    return o;
}

async function run(action, okMsg, reload) {
    try {
        await action();
        toast(okMsg || '操作成功');
        closeModal();
        if (reload) reload(); else render();
    } catch (e) { toast(e.message, true); }
}

/* ---------------- 初始化 ---------------- */
const TABS = [
    ['dashboard', '工作台'],
    ['events', '爆管事件'],
    ['orders', '工单与复供'],
    ['notifications', '客服通知'],
    ['issues', '复供后问题'],
    ['hospital', '医疗保障'],
    ['support', '停水保障'],
    ['base', '基础资料'],
];

async function init() {
    ME = await api('/api/me');
    ENUMS = await api('/api/base/enums');
    [ZONES, PIPES, FACILITIES] = await Promise.all([
        api('/api/base/zones'), api('/api/base/pipes'), api('/api/base/facilities')
    ]);
    $('#userInfo').textContent = `${ME.displayName}（${ME.roleLabel}）`;
    $('#nav').innerHTML = TABS.map(([k, n]) =>
        `<a data-tab="${k}" class="${k === CURRENT_TAB ? 'active' : ''}" onclick="switchTab('${k}')">${n}</a>`).join('');
    render();
}

function switchTab(tab) {
    CURRENT_TAB = tab;
    document.querySelectorAll('#nav a').forEach(a => a.classList.toggle('active', a.dataset.tab === tab));
    render();
}

function render() {
    const fn = { dashboard: renderDashboard, events: renderEvents, orders: renderOrders,
        notifications: renderNotifications, issues: renderIssues, hospital: renderHospital,
        support: renderSupport, base: renderBase }[CURRENT_TAB];
    fn();
}

/* ---------------- 工作台 ---------------- */
async function renderDashboard() {
    const d = await api('/api/dashboard');
    const s = d.support;
    $('#main').innerHTML = `
    <div class="cards">
        <div class="stat-card"><div class="num">${d.activeEvents}</div><div class="lbl">在办事件</div></div>
        <div class="stat-card"><div class="num">${d.restoredEvents}</div><div class="lbl">已复供</div></div>
        <div class="stat-card"><div class="num">${d.closedEvents}</div><div class="lbl">已关闭</div></div>
        <div class="stat-card"><div class="num ${d.openIssues > 0 ? 'warn' : ''}">${d.openIssues}</div><div class="lbl">待处理复供后问题</div></div>
        <div class="stat-card"><div class="num ${d.longOutageEvents.length > 0 ? 'danger' : ''}">${d.longOutageEvents.length}</div><div class="lbl">长时停水预警(>6h)</div></div>
        <div class="stat-card"><div class="num">${s.openWaterPoints}</div><div class="lbl">开放送水点</div></div>
        <div class="stat-card"><div class="num ${s.pendingDeliveries > 0 ? 'warn' : ''}">${s.pendingDeliveries}</div><div class="lbl">待送水老人</div></div>
        <div class="stat-card"><div class="num ${s.abnormalTanks > 0 ? 'danger' : ''}">${s.abnormalTanks}</div><div class="lbl">异常二供水箱</div></div>
        <div class="stat-card"><div class="num ${d.hospitalSupportActive > 0 ? 'warn' : ''}">${d.hospitalSupportActive}</div><div class="lbl">医疗保障待确认</div></div>
    </div>
    ${d.hospitalSupportActive > 0 ? `<div class="notice">🚑 医疗用水保障进行中：${d.hospitalSupports.filter(x => x.status !== 'CONFIRMED').map(x => esc(x.hospitalName) + '（' + label(x.status) + '）').join('、')}。医院确认后客服端将显示「医疗用水保障完成」，无需重复催问抢修队。</div>` : (d.hospitalSupports.length ? '<div class="notice" style="background:#f0fdf4;border-color:#bbf7d0;color:#166534">✅ 医疗用水保障完成，各医院用水已由医院方确认。</div>' : '')}
    ${d.longOutageEvents.length ? `<div class="notice danger">⚠️ 长时停水预警：${d.longOutageEvents.map(e => esc(e.eventNo + ' ' + e.location)).join('；')}，请关注临时水点、老人送水与二次供水水箱。</div>` : ''}
    <div class="grid-2">
        <div class="panel"><h3>在办事件</h3>${eventTable(d.activeList, true)}</div>
        <div class="panel"><h3>最新通知 <span class="tag">内容随抢修进展自动更新</span></h3>${notifyTable(d.recentNotifications.slice(0, 8))}</div>
    </div>
    <div class="panel"><h3>待处理复供后问题</h3>${issueTable(d.openIssueList, true)}</div>`;
}

/* ---------------- 爆管事件 ---------------- */
async function renderEvents(status) {
    const [list, hsList] = await Promise.all([
        api('/api/events' + (status ? '?status=' + status : '')),
        api('/api/hospital-support')
    ]);
    // 事件 -> 医疗保障状态映射（客服端可见，避免重复催问抢修队）
    const hsMap = {};
    for (const s of hsList) {
        const k = s.event.id;
        hsMap[k] = hsMap[k] || [];
        hsMap[k].push(s);
    }
    const statusOpts = Object.entries(ENUMS.eventStatus).map(([k, v]) =>
        `<option value="${k}" ${k === status ? 'selected' : ''}>${v}</option>`).join('');
    $('#main').innerHTML = `
    <div class="toolbar">
        <div class="filters">状态筛选：
            <select onchange="renderEvents(this.value || undefined)">
                <option value="">全部</option>${statusOpts}
            </select>
        </div>
        ${canOps() ? '<button class="btn btn-primary" onclick="showEventCreate()">＋ 新建报警</button>' : ''}
    </div>
    <div class="panel">${eventTable(list, false, hsMap)}</div>`;
}

function hsBadge(list) {
    if (!list || !list.length) return '<span class="muted">—</span>';
    return list.every(s => s.status === 'CONFIRMED')
        ? '<span class="badge CONFIRMED">✅ 医疗用水保障完成</span>'
        : '<span class="badge DELIVERING">🚑 医疗保障中</span>';
}

function eventTable(list, compact, hsMap) {
    if (!list || !list.length) return '<div class="empty">暂无事件</div>';
    const showHs = !!hsMap;
    return `<table><thead><tr>
        <th>事件编号</th><th>来源</th><th>位置</th><th>分区</th><th>级别</th><th>状态</th>${showHs ? '<th>医疗保障</th>' : ''}<th>报警时间</th><th>操作</th>
        </tr></thead><tbody>` + list.map(e => `<tr>
        <td>${esc(e.eventNo)}</td><td>${esc(label(e.source))}</td><td>${esc(e.location)}</td>
        <td>${esc(e.zone ? e.zone.name : '')}</td><td>${badge(e.severity)}</td><td>${badge(e.status)}</td>
        ${showHs ? `<td>${hsBadge(hsMap[e.id])}</td>` : ''}
        <td>${fmt(e.createdAt)}</td>
        <td><button class="btn btn-sm" onclick="showEventDetail(${e.id})">详情</button></td>
        </tr>`).join('') + '</tbody></table>';
}

function showEventCreate() {
    const srcOpts = Object.entries(ENUMS.eventSource).map(([k, v]) => `<option value="${k}">${v}</option>`).join('');
    const zoneOpts = ZONES.map(z => `<option value="${z.id}">${esc(z.code)} ${esc(z.name)}</option>`).join('');
    const pipeOpts = '<option value="">（暂不关联）</option>' + PIPES.map(p =>
        `<option value="${p.id}">${esc(p.code)} DN${p.diameterMm} ${esc(p.material)} ${esc(p.roadName)}</option>`).join('');
    openModal('新建爆管报警', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label>报警来源<select name="source">${srcOpts}</select></label>
        <label>所在分区<select name="zoneId">${zoneOpts}</select></label>
        <label class="full">事发位置<input name="location" required placeholder="如 中山路与解放路交叉口"></label>
        <label class="full">情况描述<textarea name="description" placeholder="涌水情况、影响描述"></textarea></label>
        <label class="full">关联管段（历史管线资料）<select name="pipeSegmentId">${pipeOpts}</select></label>
        <label>报告人<input name="reporterName"></label>
        <label>联系电话<input name="reporterPhone"></label>
    </form>
    <div class="form-actions">
        <button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitEventCreate()">提交报警</button>
    </div>`);
}

async function submitEventCreate() {
    const d = formData('f');
    if (!d.location) { toast('请填写事发位置', true); return; }
    d.zoneId = Number(d.zoneId);
    d.pipeSegmentId = d.pipeSegmentId ? Number(d.pipeSegmentId) : null;
    await run(() => post('/api/events', d), '报警已登记');
}

async function showEventDetail(id) {
    const d = await api('/api/events/' + id);
    const e = d.event, a = d.assessment;
    let html = `
    <div class="detail-section">
        <h4>事件信息 ${badge(e.status)} ${badge(e.severity)}</h4>
        <div class="kv">
            <div><b>编号</b>${esc(e.eventNo)}</div>
            <div><b>来源</b>${esc(label(e.source))}</div>
            <div><b>位置</b>${esc(e.location)}</div>
            <div><b>分区</b>${esc(e.zone.name)}</div>
            <div><b>描述</b>${esc(e.description) || '-'}</div>
            <div><b>报告人</b>${esc(e.reporterName) || '-'} ${esc(e.reporterPhone) || ''}</div>
            <div><b>报警时间</b>${fmt(e.createdAt)}</div>
            <div><b>复供时间</b>${fmt(e.restoredAt)}</div>
        </div>
        <div class="actions" style="margin-top:10px">
            ${canOps() && e.status === 'REPORTED' ? `<button class="btn btn-primary btn-sm" onclick="doAssess(${e.id})">生成影响评估</button>` : ''}
            ${canOps() && (e.status === 'ASSESSED' || e.status === 'DISPATCHED') ? `<button class="btn btn-primary btn-sm" onclick="showDispatch(${e.id})">调度派单</button>` : ''}
            ${canOps() && e.status === 'RESTORED' ? `<button class="btn btn-ok btn-sm" onclick="doCloseEvent(${e.id})">关闭事件</button>` : ''}
        </div>
    </div>`;

    if (a) {
        html += `
    <div class="detail-section"><h4>影响评估（管径 / 阀门分区 / 周边设施 / 历史管线）</h4>
        <div class="kv">
            <div><b>影响人口</b>${a.affectedPopulation} 人（约 ${a.affectedHouseholds} 户）</div>
            <div><b>预计停水</b>${a.estimatedOutageHours} 小时</div>
            <div><b>医院</b>${a.hospitalAffected ? '⚠️ 涉及' : '不涉及'}</div>
            <div><b>学校</b>${a.schoolAffected ? '⚠️ 涉及' : '不涉及'}</div>
            <div><b>高层二供</b>${a.highRiseAffected ? '⚠️ 涉及' : '不涉及'}</div>
            <div><b>餐饮街</b>${a.restaurantAffected ? '⚠️ 涉及' : '不涉及'}</div>
        </div>
        <div class="kv" style="margin-top:6px">
            <div><b>关阀方案</b>${esc(a.valvePlan)}</div>
            <div><b>管线分析</b>${esc(a.pipeAnalysis)}</div>
            <div><b>交通影响</b>${esc(a.trafficImpact)}</div>
            <div><b>评估人</b>${esc(a.assessor)} ${fmt(a.createdAt)}</div>
        </div>
        ${a.affectedFacilities && a.affectedFacilities.length ? `<table style="margin-top:8px"><thead><tr><th>受影响设施</th><th>类型</th><th>人口</th><th>联系人</th><th>备注</th></tr></thead><tbody>
            ${a.affectedFacilities.map(f => `<tr><td>${esc(f.name)}${f.highRise ? '（高层）' : ''}</td><td>${esc(label(f.type))}</td><td>${f.population}</td><td>${esc(f.contactName) || '-'} ${esc(f.contactPhone) || ''}</td><td>${esc(f.note) || '-'}</td></tr>`).join('')}
        </tbody></table>` : ''}
    </div>`;
    }

    html += `<div class="detail-section"><h4>抢修工单（${d.orders.length}）</h4>`;
    if (!d.orders.length) html += '<div class="empty">尚未派单</div>';
    for (const od of d.orders) html += orderBlock(od);
    html += '</div>';

    if (d.hospitalSupports && d.hospitalSupports.length) {
        html += `<div class="detail-section"><h4>医院应急供水保障（${d.hospitalSupports.length}）</h4>`
            + d.hospitalSupports.map(hsCard).join('') + '</div>';
    }

    html += `<div class="detail-section"><h4>客服通知记录（${d.notifications.length}）</h4>${notifyTable(d.notifications)}</div>`;

    html += `<div class="detail-section"><h4>停水保障</h4>
        <div class="kv">
            <div><b>送水点</b>${d.waterPoints.map(p => esc(p.name) + '(排队' + p.queueLength + '人)').join('、') || '无'}</div>
            <div><b>老人送水</b>${d.elderlyDeliveries.map(x => esc(x.elderName) + '[' + label(x.status) + ']').join('、') || '无'}</div>
            <div><b>商户损失</b>${d.merchantLosses.map(x => esc(x.merchantName) + '[' + label(x.status) + ']').join('、') || '无'}</div>
            <div><b>二供水箱</b>${d.tanks.map(x => esc(x.community + x.building) + '水位' + x.levelPercent + '%').join('、') || '无'}</div>
        </div>
        <div class="muted" style="margin-top:4px">在「停水保障」页签中维护以上记录。</div>
    </div>`;

    openModal('事件详情 ' + e.eventNo, html);
}

function orderBlock(od) {
    const o = od.order, c = od.check;
    const nextStages = Object.keys(ENUMS.repairStage).filter(s => s !== 'COMPLETED' &&
        stageOrder(s) > stageOrder(o.stage));
    return `<div class="panel" style="background:#fbfdff">
        <div class="kv">
            <div><b>工单号</b>${esc(o.orderNo)} ${badge(o.stage)}</div>
            <div><b>抢修队</b>${esc(o.teamName)} ${esc(o.crewLeader)} ${esc(o.crewPhone) || ''}</div>
            <div><b>预计复供</b>${fmt(o.estimatedRestoreTime)}</div>
            <div><b>开挖许可</b>${esc(o.excavationPermitNo) || '-'}</div>
            <div><b>阀门操作</b>${esc(o.valveOps) || '-'}</div>
            <div><b>备件</b>${esc(o.spareParts) || '-'}</div>
            <div><b>送水点</b>${esc(o.waterPoints) || '-'}</div>
            <div><b>交通协管</b>${o.trafficControl ? '需要 ' + esc(o.trafficPlan || '') : '不需要'}</div>
            <div><b>协同方</b>${['involveCs:客服','involveStreet:街道','involveProperty:物业','involveWaterTruck:供水车','involveQuality:水质检测']
                .filter(s => o[s.split(':')[0]]).map(s => s.split(':')[1]).join('、') || '-'}</div>
        </div>
        <div class="actions" style="margin:8px 0">
            ${canCrew() && nextStages.length && o.stage !== 'COMPLETED' ? `<button class="btn btn-primary btn-sm" onclick="showProgress(${o.id},'${o.stage}')">上报现场进度</button>` : ''}
            ${canQuality() && o.stage !== 'COMPLETED' && stageOrder(o.stage) >= 2 ? `<button class="btn btn-sm" onclick="showRestoreCheck(${o.id})">复供确认单</button>` : ''}
            ${canOps() && o.stage !== 'COMPLETED' ? `<button class="btn btn-ok btn-sm" onclick="doConfirmRestore(${o.id})">确认复供</button>` : ''}
        </div>
        <div class="timeline">
            ${od.logs.map(l => `<div class="t-item"><b>${esc(label(l.stage))}</b> ${esc(l.note) || ''}
                <div class="t-time">${fmt(l.createdAt)} · ${esc(l.operatorName)}</div></div>`).join('') || '<div class="empty">暂无现场记录</div>'}
        </div>
        ${c ? `<div class="notice" style="margin-top:8px">复供确认：水压${c.pressureOk ? '✅' : '⬜'} 水质${c.qualityOk ? '✅' : '⬜'} 冲洗${c.flushingOk ? '✅' : '⬜'} 通知${c.notificationOk ? '✅' : '⬜'}
            ${c.turbidity != null ? '｜浊度 ' + c.turbidity + ' NTU' : ''}${c.residualChlorine != null ? '｜余氯 ' + c.residualChlorine + ' mg/L' : ''}
            ${c.confirmedBy ? '｜复供确认人：' + esc(c.confirmedBy) + ' ' + fmt(c.confirmedAt) : ''}</div>` : ''}
        ${od.issues.length ? `<table style="margin-top:6px"><thead><tr><th>复供后问题</th><th>类型</th><th>状态</th><th>时间</th></tr></thead><tbody>
            ${od.issues.map(i => `<tr><td>${esc(i.description)}</td><td>${esc(label(i.type))}</td><td>${badge(i.status)}</td><td>${fmt(i.createdAt)}</td></tr>`).join('')}
        </tbody></table>` : ''}
    </div>`;
}

function stageOrder(s) {
    return ['DISPATCHED','ARRIVED','VALVE_CLOSED','EXCAVATION','PIPE_REPLACED','FLUSHING','DISINFECTION','PRESSURE_TEST','ROAD_RESTORED','COMPLETED'].indexOf(s);
}

async function doAssess(id) {
    await run(() => post('/api/events/' + id + '/assess'), '影响评估已生成', () => showEventDetail(id));
}

async function doCloseEvent(id) {
    await run(() => post('/api/events/' + id + '/close'), '事件已关闭', () => showEventDetail(id));
}

async function showDispatch(eventId) {
    const d = await api('/api/events/' + eventId);
    const a = d.assessment;
    openModal('调度派单 - ' + d.event.eventNo, `
    ${a ? `<div class="notice">评估建议：${esc(a.valvePlan)}｜预计停水 ${a.estimatedOutageHours} 小时</div>` : ''}
    <form id="f" class="form-grid" onsubmit="return false">
        <label>抢修队<input name="teamName" required value="抢修一队"></label>
        <label>带队人<input name="crewLeader" required placeholder="李队长"></label>
        <label>联系电话<input name="crewPhone"></label>
        <label>预计停水时长(小时)<input name="estimatedRestoreHours" type="number" min="1" value="${a ? a.estimatedOutageHours : 4}"></label>
        <label class="full">阀门位置与关阀操作<textarea name="valveOps">${a ? esc(a.valvePlan) : ''}</textarea></label>
        <label>开挖许可编号 *<input name="excavationPermitNo" required placeholder="挖许字2026-XXXX"></label>
        <label>备件库存确认 *<input name="spareParts" required placeholder="管段/管件/消毒剂"></label>
        <label class="full">应急送水点 *<input name="waterPoints" required placeholder="如 阳光高层小区东门、人民医院急诊楼前"></label>
        <label class="full">交通协管方案<input name="trafficPlan" placeholder="如 中山路双向各封闭一条车道"></label>
        <div class="full checks">
            <label><input type="checkbox" name="trafficControl" ${a && a.roadAffected ? 'checked' : ''}> 需要交通协管</label>
            <label><input type="checkbox" name="involveCs" checked> 客服</label>
            <label><input type="checkbox" name="involveStreet" ${a && (a.restaurantAffected || a.roadAffected) ? 'checked' : ''}> 街道</label>
            <label><input type="checkbox" name="involveProperty" ${a && a.highRiseAffected ? 'checked' : ''}> 物业</label>
            <label><input type="checkbox" name="involveWaterTruck" ${a && (a.highRiseAffected || a.hospitalAffected) ? 'checked' : ''}> 供水车</label>
            <label><input type="checkbox" name="involveQuality" ${a && (a.hospitalAffected || a.schoolAffected) ? 'checked' : ''}> 水质检测</label>
        </div>
    </form>
    <div class="form-actions">
        <button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitDispatch(${eventId})">派出抢修队</button>
    </div>`);
}

async function submitDispatch(eventId) {
    const d = formData('f');
    if (!d.teamName || !d.crewLeader) { toast('请填写抢修队与带队人', true); return; }
    if (!d.excavationPermitNo || !d.spareParts || !d.waterPoints) {
        toast('开挖许可、备件库存、应急送水点为派单必填项', true); return;
    }
    for (const k of ['trafficControl','involveCs','involveStreet','involveProperty','involveWaterTruck','involveQuality'])
        d[k] = d[k] === 'on';
    d.estimatedRestoreHours = Number(d.estimatedRestoreHours || 4);
    await run(() => post(`/api/events/${eventId}/dispatch`, d), '已派单并生成停水通知', () => showEventDetail(eventId));
}

function showProgress(orderId, currentStage) {
    // 严格按顺序推进：只允许上报下一顺序阶段，保证时间线保留全部现场阶段
    const next = Object.keys(ENUMS.repairStage)
        .find(s => s !== 'COMPLETED' && stageOrder(s) === stageOrder(currentStage) + 1);
    if (!next) { toast('现场阶段已全部记录完毕，请提交复供确认', true); return; }
    openModal('上报现场进度', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label>当前阶段<select disabled><option>${esc(label(currentStage))}</option></select></label>
        <label>推进到（按顺序）<select name="stage"><option value="${next}">${esc(label(next))}</option></select></label>
        <label class="full">现场记录<textarea name="note" placeholder="如 V-101、V-102 已关闭，止水完成"></textarea></label>
    </form>
    <div class="form-actions">
        <button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitProgress(${orderId})">提交</button>
    </div>`);
}

async function submitProgress(orderId) {
    const d = formData('f');
    await run(() => post(`/api/orders/${orderId}/progress`, d), '进度已上报，通知内容已同步更新', render);
}

async function showRestoreCheck(orderId) {
    const d = await api('/api/orders/' + orderId);
    const ev = await api('/api/events/' + d.event.id);
    const od = ev.orders.find(x => x.order.id === orderId);
    const c = od && od.check ? od.check : {};
    openModal('复供确认单（复供前四项必须全部确认）', `
    <form id="f" class="form-grid" onsubmit="return false">
        <div class="full checks">
            <label><input type="checkbox" name="pressureOk" ${c.pressureOk ? 'checked' : ''}> 水压测试合格</label>
            <label><input type="checkbox" name="qualityOk" ${c.qualityOk ? 'checked' : ''}> 水质检测合格</label>
            <label><input type="checkbox" name="flushingOk" ${c.flushingOk ? 'checked' : ''}> 管网冲洗完成</label>
            <label><input type="checkbox" name="notificationOk" ${c.notificationOk ? 'checked' : ''}> 用户通知到位</label>
        </div>
        <label>浊度 (NTU)<input name="turbidity" type="number" step="0.01" value="${c.turbidity ?? ''}"></label>
        <label>余氯 (mg/L)<input name="residualChlorine" type="number" step="0.01" value="${c.residualChlorine ?? ''}"></label>
        <label class="full">备注<textarea name="note">${esc(c.note) || ''}</textarea></label>
    </form>
    <div class="form-actions">
        <button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitRestoreCheck(${orderId})">保存确认单</button>
    </div>`);
}

async function submitRestoreCheck(orderId) {
    const d = formData('f');
    for (const k of ['pressureOk','qualityOk','flushingOk','notificationOk']) d[k] = d[k] === 'on';
    d.turbidity = d.turbidity ? Number(d.turbidity) : null;
    d.residualChlorine = d.residualChlorine ? Number(d.residualChlorine) : null;
    await run(() => post(`/api/orders/${orderId}/restore-check`, d), '复供确认单已保存', render);
}

async function doConfirmRestore(orderId) {
    await run(() => post(`/api/orders/${orderId}/confirm-restore`), '已复供，复供通知已发送', render);
}

/* ---------------- 工单与复供 ---------------- */
async function renderOrders() {
    const list = await api('/api/orders');
    $('#main').innerHTML = `<div class="panel"><h3>抢修工单</h3>${
        !list.length ? '<div class="empty">暂无工单</div>' : `<table><thead><tr>
        <th>工单号</th><th>事件</th><th>位置</th><th>抢修队</th><th>阶段</th><th>预计复供</th><th>协同方</th><th>操作</th>
        </tr></thead><tbody>` + list.map(o => `<tr>
        <td>${esc(o.orderNo)}</td><td>${esc(o.event.eventNo)}</td><td>${esc(o.event.location)}</td>
        <td>${esc(o.teamName)}</td><td>${badge(o.stage)}</td><td>${fmt(o.estimatedRestoreTime)}</td>
        <td>${['involveCs:客服','involveStreet:街道','involveProperty:物业','involveWaterTruck:供水车','involveQuality:水质']
            .filter(s => o[s.split(':')[0]]).map(s => s.split(':')[1]).join('、') || '-'}</td>
        <td><button class="btn btn-sm" onclick="showEventDetail(${o.event.id})">详情</button></td>
        </tr>`).join('') + '</tbody></table>'}</div>`;
}

/* ---------------- 客服通知 ---------------- */
async function renderNotifications() {
    const list = await api('/api/notifications');
    $('#main').innerHTML = `
    <div class="toolbar"><div class="muted">通知内容随抢修进展自动变化，也可手动补发。</div>
        ${canCs() ? '<button class="btn btn-primary" onclick="showNotifySend()">＋ 发送通知</button>' : ''}</div>
    <div class="panel">${notifyTable(list)}</div>`;
}

function notifyTable(list) {
    if (!list || !list.length) return '<div class="empty">暂无通知</div>';
    return `<table><thead><tr><th>时间</th><th>渠道</th><th>目标人群</th><th>内容</th><th>阶段</th><th>方式</th><th>发送人</th></tr></thead><tbody>`
        + list.map(n => `<tr>
        <td>${fmt(n.createdAt)}</td><td>${esc(label(n.channel))}</td><td>${esc(n.audience)}</td>
        <td style="max-width:420px">${esc(n.content)}</td>
        <td>${esc(label(n.stage))}</td><td><span class="badge ${n.sendMode}">${n.sendMode === 'AUTO' ? '自动' : '手动'}</span></td><td>${esc(n.sentBy)}</td>
        </tr>`).join('') + '</tbody></table>';
}

async function showNotifySend() {
    const events = await api('/api/events');
    const evOpts = events.map(e => `<option value="${e.id}">${esc(e.eventNo)} ${esc(e.location)}（${esc(label(e.status))}）</option>`).join('');
    const chOpts = Object.entries(ENUMS.notifyChannel).map(([k, v]) => `<option value="${k}">${v}</option>`).join('');
    openModal('发送客服通知', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label>关联事件<select name="eventId" id="nfEvent">${evOpts}</select></label>
        <label>通知渠道<select name="channel" id="nfChannel">${chOpts}</select></label>
        <label class="full">目标人群<input name="audience" id="nfAudience"></label>
        <label class="full">通知内容<textarea name="content" id="nfContent" rows="4"></textarea></label>
    </form>
    <div class="form-actions">
        <button class="btn" onclick="previewNotify()">按当前进展生成模板</button>
        <button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitNotify()">发送</button>
    </div>`);
    previewNotify();
}

async function previewNotify() {
    const eventId = $('#nfEvent').value, channel = $('#nfChannel').value;
    if (!eventId) return;
    const t = await api(`/api/notifications/template?eventId=${eventId}&channel=${channel}`);
    $('#nfAudience').value = t.audience;
    $('#nfContent').value = t.content;
}

async function submitNotify() {
    const d = formData('f');
    if (!d.audience || !d.content) { toast('请填写目标人群与内容', true); return; }
    d.eventId = Number(d.eventId);
    await run(() => post('/api/notifications', d), '通知已发送');
}

/* ---------------- 复供后问题 ---------------- */
async function renderIssues() {
    const list = await api('/api/issues');
    $('#main').innerHTML = `
    <div class="toolbar"><div class="muted">黄水投诉、二次漏水、道路沉降、赔付申请均回到原抢修单闭环处理。</div>
        ${canCs() ? '<button class="btn btn-primary" onclick="showIssueCreate()">＋ 登记问题</button>' : ''}</div>
    <div class="panel">${issueTable(list)}</div>`;
}

function issueTable(list, compact) {
    if (!list || !list.length) return '<div class="empty">暂无记录</div>';
    return `<table><thead><tr><th>时间</th><th>工单</th><th>类型</th><th>描述</th><th>联系人</th><th>状态</th><th>处理</th></tr></thead><tbody>`
        + list.map(i => `<tr>
        <td>${fmt(i.createdAt)}</td><td>${esc(i.order ? i.order.orderNo : '')}</td><td>${esc(label(i.type))}</td>
        <td style="max-width:320px">${esc(i.description)}${i.handleNote ? '<br><span class="muted">处理：' + esc(i.handleNote) + '</span>' : ''}</td>
        <td>${esc(i.contactName) || '-'} ${esc(i.contactPhone) || ''}</td><td>${badge(i.status)}</td>
        <td>${canCs() && i.status !== 'RESOLVED' ? `<div class="actions">
            ${i.status === 'OPEN' ? `<button class="btn btn-sm" onclick="issueStatus(${i.id},'PROCESSING')">受理</button>` : ''}
            <button class="btn btn-ok btn-sm" onclick="issueResolve(${i.id})">办结</button></div>` : (i.handler ? esc(i.handler) : '')}</td>
        </tr>`).join('') + '</tbody></table>';
}

async function showIssueCreate() {
    const orders = (await api('/api/orders')).filter(o => ['RESTORED', 'CLOSED'].includes(o.event.status));
    if (!orders.length) { toast('暂无已复供的工单，复供后才能登记问题', true); return; }
    const oOpts = orders.map(o => `<option value="${o.id}">${esc(o.orderNo)} ${esc(o.event.location)}</option>`).join('');
    const tOpts = Object.entries(ENUMS.issueType).map(([k, v]) => `<option value="${k}">${v}</option>`).join('');
    openModal('登记复供后问题', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label class="full">原抢修工单<select name="orderId">${oOpts}</select></label>
        <label>问题类型<select name="type">${tOpts}</select></label>
        <label>联系人<input name="contactName" placeholder="姓名"></label>
        <label>联系电话<input name="contactPhone"></label>
        <label class="full">问题描述<textarea name="description" required placeholder="如 老街社区3栋居民反映复供后出黄水"></textarea></label>
    </form>
    <div class="form-actions">
        <button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitIssue()">登记</button>
    </div>`);
}

async function submitIssue() {
    const d = formData('f');
    if (!d.description) { toast('请填写问题描述', true); return; }
    d.orderId = Number(d.orderId);
    await run(() => post('/api/issues', d), '问题已登记到原抢修单');
}

async function issueStatus(id, status) {
    await run(() => post(`/api/issues/${id}/status`, { status }), '已受理');
}

function issueResolve(id) {
    openModal('办结问题', `
    <form id="f" onsubmit="return false">
        <label style="display:block">处理说明<textarea name="handleNote" style="width:100%;margin-top:6px;padding:8px;border:1px solid var(--border);border-radius:6px" rows="3" placeholder="如 已上门排放黄水并复检水质合格"></textarea></label>
    </form>
    <div class="form-actions">
        <button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-ok" onclick="submitIssueResolve(${id})">办结</button>
    </div>`);
}

async function submitIssueResolve(id) {
    const d = formData('f');
    await run(() => post(`/api/issues/${id}/status`, { status: 'RESOLVED', handleNote: d.handleNote }), '问题已办结');
}

/* ---------------- 停水保障 ---------------- */
let SUPPORT_EVENT_ID = null;

async function renderSupport() {
    const events = await api('/api/events');
    if (!SUPPORT_EVENT_ID && events.length) SUPPORT_EVENT_ID = events[0].id;
    const q = SUPPORT_EVENT_ID ? '?eventId=' + SUPPORT_EVENT_ID : '';
    const [wps, elders, losses, tanks] = await Promise.all([
        api('/api/support/water-points' + q), api('/api/support/elderly' + q),
        api('/api/support/merchant-loss' + q), api('/api/support/tanks' + q)]);
    const evOpts = events.map(e => `<option value="${e.id}" ${e.id === SUPPORT_EVENT_ID ? 'selected' : ''}>${esc(e.eventNo)} ${esc(e.location)}</option>`).join('');
    $('#main').innerHTML = `
    <div class="toolbar"><div class="filters">关联事件：<select onchange="SUPPORT_EVENT_ID=Number(this.value);renderSupport()">${evOpts}</select></div>
        <div class="muted">长时间停水保障：临时水点 · 老人送水 · 商户停业损失 · 二次供水水箱</div></div>
    <div class="grid-2">
        <div class="panel"><h3>应急送水点 ${canCs() ? `<button class="btn btn-sm btn-primary" onclick="showWpAdd()">＋</button>` : ''}</h3>
            ${!wps.length ? '<div class="empty">暂无</div>' : `<table><thead><tr><th>名称</th><th>位置</th><th>排队</th><th>状态</th><th>操作</th></tr></thead><tbody>
            ${wps.map(p => `<tr><td>${esc(p.name)}</td><td>${esc(p.location)}</td><td>${p.queueLength} 人</td><td>${badge(p.status)}</td>
            <td>${canCs() ? `<button class="btn btn-sm" onclick="showWpUpdate(${p.id},${p.queueLength},'${p.status}')">更新</button>` : ''}</td></tr>`).join('')}
            </tbody></table>`}</div>
        <div class="panel"><h3>老人/特殊用户送水 ${canCs() ? `<button class="btn btn-sm btn-primary" onclick="showElderAdd()">＋</button>` : ''}</h3>
            ${!elders.length ? '<div class="empty">暂无</div>' : `<table><thead><tr><th>姓名</th><th>地址</th><th>送水人</th><th>状态</th><th>操作</th></tr></thead><tbody>
            ${elders.map(x => `<tr><td>${esc(x.elderName)}</td><td>${esc(x.address)}</td><td>${esc(x.deliverer) || '-'}</td><td>${badge(x.status)}</td>
            <td>${canCs() && x.status !== 'DONE' ? `<button class="btn btn-sm" onclick="elderNext(${x.id},'${x.status}')">${x.status === 'PENDING' ? '开始送水' : '送达'}</button>` : ''}</td></tr>`).join('')}
            </tbody></table>`}</div>
        <div class="panel"><h3>商户停业损失 ${canCs() ? `<button class="btn btn-sm btn-primary" onclick="showLossAdd()">＋</button>` : ''}</h3>
            ${!losses.length ? '<div class="empty">暂无</div>' : `<table><thead><tr><th>商户</th><th>类别</th><th>申报金额</th><th>状态</th><th>操作</th></tr></thead><tbody>
            ${losses.map(x => `<tr><td>${esc(x.merchantName)}</td><td>${esc(x.category) || '-'}</td><td>${x.lossAmount != null ? '¥' + x.lossAmount : '-'}</td><td>${badge(x.status)}</td>
            <td>${canCs() && x.status !== 'SETTLED' ? `<button class="btn btn-sm" onclick="lossNext(${x.id},'${x.status}')">${x.status === 'REPORTED' ? '受理审核' : '完成赔付'}</button>` : ''}</td></tr>`).join('')}
            </tbody></table>`}</div>
        <div class="panel"><h3>二次供水水箱 ${canCs() ? `<button class="btn btn-sm btn-primary" onclick="showTankAdd()">＋</button>` : ''}</h3>
            ${!tanks.length ? '<div class="empty">暂无</div>' : `<table><thead><tr><th>小区</th><th>楼栋/水箱</th><th>水位</th><th>状态</th><th>检查人</th><th>操作</th></tr></thead><tbody>
            ${tanks.map(x => `<tr><td>${esc(x.community)}</td><td>${esc(x.building)}</td><td>${x.levelPercent}%</td><td>${badge(x.status)}</td><td>${esc(x.checkedBy) || '-'}</td>
            <td>${canCs() ? `<button class="btn btn-sm" onclick="showTankUpdate(${x.id},${x.levelPercent},'${x.status}')">更新</button>` : ''}</td></tr>`).join('')}
            </tbody></table>`}</div>
    </div>`;
}

function eventSelectHtml() {
    return `<label class="full">关联事件<select name="eventId" id="spEvent"></select></label>`;
}
async function fillEventSelect() {
    const events = await api('/api/events');
    $('#spEvent').innerHTML = events.map(e => `<option value="${e.id}" ${e.id === SUPPORT_EVENT_ID ? 'selected' : ''}>${esc(e.eventNo)} ${esc(e.location)}</option>`).join('');
}

async function showWpAdd() {
    openModal('新增应急送水点', `<form id="f" class="form-grid" onsubmit="return false">${eventSelectHtml()}
        <label>名称<input name="name" required placeholder="如 阳光高层东门送水点"></label>
        <label>位置<input name="location" required></label>
        <label class="full">备注<input name="note" placeholder="如 供水车2辆驻点"></label></form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitWpAdd()">保存</button></div>`);
    fillEventSelect();
}
async function submitWpAdd() {
    const d = formData('f'); d.eventId = Number(d.eventId);
    await run(() => post('/api/support/water-points', d), '送水点已设立');
}
function showWpUpdate(id, queue, status) {
    const sOpts = Object.entries(ENUMS.waterPointStatus).map(([k, v]) => `<option value="${k}" ${k === status ? 'selected' : ''}>${v}</option>`).join('');
    openModal('更新送水点', `<form id="f" class="form-grid" onsubmit="return false">
        <label>当前排队人数<input name="queueLength" type="number" min="0" value="${queue}"></label>
        <label>状态<select name="status">${sOpts}</select></label>
        <label class="full">备注<input name="note"></label></form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitWpUpdate(${id})">保存</button></div>`);
}
async function submitWpUpdate(id) {
    const d = formData('f'); d.queueLength = Number(d.queueLength || 0);
    await run(() => post('/api/support/water-points/' + id, d), '已更新');
}

async function showElderAdd() {
    openModal('登记老人送水', `<form id="f" class="form-grid" onsubmit="return false">${eventSelectHtml()}
        <label>姓名<input name="elderName" required></label>
        <label>电话<input name="phone"></label>
        <label class="full">地址<input name="address" required placeholder="楼栋门牌"></label>
        <label>送水人<input name="deliverer" placeholder="志愿者/物业"></label>
        <label class="full">备注<input name="note" placeholder="如 行动不便需送水上楼"></label></form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitElderAdd()">保存</button></div>`);
    fillEventSelect();
}
async function submitElderAdd() {
    const d = formData('f'); d.eventId = Number(d.eventId);
    await run(() => post('/api/support/elderly', d), '已登记');
}
async function elderNext(id, status) {
    const next = status === 'PENDING' ? 'DELIVERING' : 'DONE';
    await run(() => post(`/api/support/elderly/${id}/status`, { status: next }), '已更新');
}

async function showLossAdd() {
    openModal('登记商户停业损失', `<form id="f" class="form-grid" onsubmit="return false">${eventSelectHtml()}
        <label>商户名称<input name="merchantName" required></label>
        <label>经营类别<input name="category" placeholder="餐饮/洗浴/洗车"></label>
        <label>申报损失金额(元)<input name="lossAmount" type="number" step="0.01"></label>
        <label class="full">情况说明<textarea name="description"></textarea></label></form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitLossAdd()">保存</button></div>`);
    fillEventSelect();
}
async function submitLossAdd() {
    const d = formData('f'); d.eventId = Number(d.eventId);
    d.lossAmount = d.lossAmount ? Number(d.lossAmount) : null;
    await run(() => post('/api/support/merchant-loss', d), '已登记');
}
async function lossNext(id, status) {
    const next = status === 'REPORTED' ? 'REVIEWING' : 'SETTLED';
    const note = next === 'SETTLED' ? '赔付已完成' : '转入审核';
    await run(() => post(`/api/support/merchant-loss/${id}/status`, { status: next, handleNote: note }), '已更新');
}

async function showTankAdd() {
    openModal('登记二次供水水箱', `<form id="f" class="form-grid" onsubmit="return false">${eventSelectHtml()}
        <label>小区<input name="community" required></label>
        <label>楼栋/水箱<input name="building" required placeholder="如 3栋水箱"></label>
        <label>水位(%)<input name="levelPercent" type="number" min="0" max="100" value="100"></label>
        <label>状态<select name="status">${Object.entries(ENUMS.tankStatus).map(([k, v]) => `<option value="${k}">${v}</option>`).join('')}</select></label>
        <label class="full">备注<input name="note"></label></form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitTankAdd()">保存</button></div>`);
    fillEventSelect();
}
async function submitTankAdd() {
    const d = formData('f'); d.eventId = Number(d.eventId);
    d.levelPercent = Number(d.levelPercent || 0);
    await run(() => post('/api/support/tanks', d), '已登记');
}
function showTankUpdate(id, level, status) {
    const sOpts = Object.entries(ENUMS.tankStatus).map(([k, v]) => `<option value="${k}" ${k === status ? 'selected' : ''}>${v}</option>`).join('');
    openModal('更新水箱状态', `<form id="f" class="form-grid" onsubmit="return false">
        <label>水位(%)<input name="levelPercent" type="number" min="0" max="100" value="${level}"></label>
        <label>状态<select name="status">${sOpts}</select></label>
        <label class="full">备注<input name="note"></label></form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitTankUpdate(${id})">保存</button></div>`);
}
async function submitTankUpdate(id) {
    const d = formData('f'); d.levelPercent = Number(d.levelPercent || 0);
    await run(() => post('/api/support/tanks/' + id, d), '已更新');
}

/* ---------------- 医院应急供水保障 ---------------- */
function hsNeeds(s) {
    const n = [];
    if (s.needDialysis) n.push('透析');
    if (s.needSurgery) n.push('手术');
    if (s.needSterileSupply) n.push('消毒供应');
    if (s.needInpatient) n.push('住院楼');
    return n.join('、') || '-';
}

function hsActions(s) {
    const b = [];
    if (canOps() && (s.status === 'PENDING' || s.status === 'DELIVERING'))
        b.push(`<button class="btn btn-primary btn-sm" onclick="showHsDispatch(${s.id})">调度供水车</button>`);
    if (canOps() && s.status === 'DELIVERING')
        b.push(`<button class="btn btn-ok btn-sm" onclick="showHsSupply(${s.id})">供水到位</button>`);
    if (canCs() && s.status !== 'CONFIRMED' && !s.truckAccessIssue)
        b.push(`<button class="btn btn-sm" onclick="showHsAccess(${s.id})">车辆无法进院</button>`);
    if (canCs() && s.status === 'SUPPLIED')
        b.push(`<button class="btn btn-ok btn-sm" onclick="showHsConfirm(${s.id})">医院确认</button>`);
    if (canOps())
        b.push(`<button class="btn btn-sm" onclick="showHsReview(${s.id})">复盘记录</button>`);
    return b.join('');
}

function hsCard(s) {
    return `<div class="panel" style="background:#fbfdff">
        <div class="kv">
            <div><b>医院</b>${esc(s.hospitalName)} ${badge(s.status)}</div>
            <div><b>优先需求</b>${hsNeeds(s)}</div>
            <div><b>后勤联系</b>${esc(s.logisticsContact) || '-'} ${esc(s.logisticsPhone) || ''}</div>
            <div><b>供水车</b>${esc(s.waterTrucks) || '待调度'}</div>
            <div><b>临时水箱</b>${esc(s.tempTanks) || '-'}</div>
            <div><b>供水到位</b>${fmt(s.arrivedAt)}</div>
            <div><b>用水量</b>${s.waterAmountM3 != null ? s.waterAmountM3 + ' m³' : '-'}</div>
            <div><b>复供时间</b>${fmt(s.restoreTime)}</div>
            ${s.truckAccessIssue ? `<div><b>车辆无法进院</b>改设水点：${esc(s.altWaterPoint) || '-'}；志愿者送水：${esc(s.volunteers) || '-'}</div>` : ''}
            ${s.hospitalConfirmer ? `<div><b>医院确认</b>${esc(s.hospitalConfirmer)} ${fmt(s.confirmedAt)}</div>` : ''}
            ${s.reviewNote ? `<div><b>复盘</b>${esc(s.reviewNote)}</div>` : ''}
        </div>
        <div class="actions" style="margin-top:8px">${hsActions(s)}</div>
    </div>`;
}

async function renderHospital() {
    const list = await api('/api/hospital-support');
    $('#main').innerHTML = `
    <div class="toolbar">
        <div class="muted">爆管影响医院时自动建单：优先保障透析 / 手术 / 消毒供应 / 住院楼。医院确认后客服端显示「医疗用水保障完成」，无需重复催问抢修队。</div>
        ${canOps() ? '<button class="btn btn-primary" onclick="showHsCreate()">＋ 手动登记</button>' : ''}
    </div>
    <div class="panel">${!list.length ? '<div class="empty">暂无医疗保障单（派单时按影响评估自动创建）</div>' :
        `<table><thead><tr><th>事件</th><th>医院</th><th>优先需求</th><th>供水车/水箱</th><th>状态</th><th>到位/用水量</th><th>复供时间</th><th>确认人</th><th>操作</th></tr></thead><tbody>`
        + list.map(s => `<tr>
            <td>${esc(s.event.eventNo)}</td>
            <td>${esc(s.hospitalName)}${s.truckAccessIssue ? '<br><span class="badge CROWDED">车辆改设水点</span>' : ''}</td>
            <td>${hsNeeds(s)}</td>
            <td>${esc(s.waterTrucks) || '待调度'}${s.tempTanks ? '<br>' + esc(s.tempTanks) : ''}</td>
            <td>${badge(s.status)}${s.status === 'CONFIRMED' ? '<br><span class="badge CONFIRMED">医疗用水保障完成</span>' : ''}</td>
            <td>${fmt(s.arrivedAt)}${s.waterAmountM3 != null ? '<br>' + s.waterAmountM3 + ' m³' : ''}</td>
            <td>${fmt(s.restoreTime)}</td>
            <td>${esc(s.hospitalConfirmer) || '-'}</td>
            <td><div class="actions">${hsActions(s)}</div></td>
        </tr>`).join('') + '</tbody></table>'}</div>`;
}

async function showHsCreate() {
    const events = await api('/api/events');
    const evOpts = events.map(e => `<option value="${e.id}">${esc(e.eventNo)} ${esc(e.location)}</option>`).join('');
    openModal('手动登记医疗保障单', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label class="full">关联事件<select name="eventId">${evOpts}</select></label>
        <label>医院名称<input name="hospitalName" required></label>
        <label>后勤联系人<input name="logisticsContact"></label>
        <label>联系电话<input name="logisticsPhone"></label>
        <div class="full checks">
            <label><input type="checkbox" name="needDialysis" checked> 透析</label>
            <label><input type="checkbox" name="needSurgery" checked> 手术</label>
            <label><input type="checkbox" name="needSterileSupply" checked> 消毒供应</label>
            <label><input type="checkbox" name="needInpatient" checked> 住院楼</label>
        </div>
    </form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitHsCreate()">登记</button></div>`);
}
async function submitHsCreate() {
    const d = formData('f');
    if (!d.hospitalName) { toast('请填写医院名称', true); return; }
    d.eventId = Number(d.eventId);
    for (const k of ['needDialysis','needSurgery','needSterileSupply','needInpatient']) d[k] = d[k] === 'on';
    await run(() => post('/api/hospital-support', d), '保障单已登记');
}

function showHsDispatch(id) {
    openModal('调度供水车与临时水箱', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label class="full">供水车 *<input name="waterTrucks" required placeholder="如 供水车2辆（浙A·D1234、浙A·D5678）"></label>
        <label class="full">临时水箱<input name="tempTanks" placeholder="如 5m³×2（住院楼前）"></label>
    </form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitHsDispatch(${id})">派出</button></div>`);
}
async function submitHsDispatch(id) {
    const d = formData('f');
    if (!d.waterTrucks) { toast('请填写供水车信息', true); return; }
    await run(() => post(`/api/hospital-support/${id}/dispatch`, d), '供水车已派出');
}

function showHsSupply(id) {
    openModal('供水到位登记（写入抢修事件）', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label>累计用水量 (m³) *<input name="waterAmountM3" type="number" step="0.1" min="0" required></label>
        <label>预计/实际复供时间（可留空，复供后自动回填）<input name="restoreTime" type="datetime-local"></label>
    </form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitHsSupply(${id})">确认到位</button></div>`);
}
async function submitHsSupply(id) {
    const d = formData('f');
    if (!d.waterAmountM3) { toast('请填写用水量', true); return; }
    d.waterAmountM3 = Number(d.waterAmountM3);
    await run(() => post(`/api/hospital-support/${id}/supply`, d), '供水到位已记录');
}

function showHsAccess(id) {
    openModal('供水车无法进入院区', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label class="full">改设水点 *<input name="altWaterPoint" required placeholder="如 医院东门对面人行道临时水点"></label>
        <label class="full">志愿者送水安排<input name="volunteers" placeholder="如 志愿者3人轮班送水至住院楼、透析中心"></label>
    </form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitHsAccess(${id})">记录</button></div>`);
}
async function submitHsAccess(id) {
    const d = formData('f');
    if (!d.altWaterPoint) { toast('请填写改设水点', true); return; }
    await run(() => post(`/api/hospital-support/${id}/access-issue`, d), '已记录改设水点与志愿者送水');
}

function showHsConfirm(id) {
    openModal('医院确认用水保障', `
    <form id="f" class="form-grid" onsubmit="return false">
        <label class="full">医院确认人 *<input name="hospitalConfirmer" required placeholder="如 后勤科-李主任"></label>
    </form>
    <div class="notice">医院确认后，客服端将显示「医疗用水保障完成」，普通客服无需再催问抢修队。</div>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-ok" onclick="submitHsConfirm(${id})">医院已确认</button></div>`);
}
async function submitHsConfirm(id) {
    const d = formData('f');
    if (!d.hospitalConfirmer) { toast('请填写医院确认人', true); return; }
    await run(() => post(`/api/hospital-support/${id}/confirm`, d), '医疗用水保障完成');
}

function showHsReview(id) {
    openModal('复盘记录（纳入事件复盘）', `
    <form id="f" onsubmit="return false">
        <label style="display:block">保障过程复盘<textarea name="reviewNote" rows="4" style="width:100%;margin-top:6px;padding:8px;border:1px solid var(--border);border-radius:6px" placeholder="如 供水车无法进院，改设东门水点并由志愿者送水，透析中心未中断治疗"></textarea></label>
    </form>
    <div class="form-actions"><button class="btn" onclick="closeModal()">取消</button>
        <button class="btn btn-primary" onclick="submitHsReview(${id})">保存复盘</button></div>`);
}
async function submitHsReview(id) {
    const d = formData('f');
    if (!d.reviewNote) { toast('请填写复盘内容', true); return; }
    await run(() => post(`/api/hospital-support/${id}/review`, d), '复盘已记录');
}

/* ---------------- 基础资料 ---------------- */
async function renderBase() {
    const valves = await api('/api/base/valves');
    const zoneName = id => (ZONES.find(z => z.id === id) || {}).name || '';
    $('#main').innerHTML = `
    <div class="grid-2">
        <div class="panel"><h3>阀门分区</h3><table><thead><tr><th>编码</th><th>名称</th><th>说明</th></tr></thead><tbody>
            ${ZONES.map(z => `<tr><td>${esc(z.code)}</td><td>${esc(z.name)}</td><td>${esc(z.description)}</td></tr>`).join('')}</tbody></table></div>
        <div class="panel"><h3>阀门台账</h3><table><thead><tr><th>编号</th><th>位置</th><th>分区</th></tr></thead><tbody>
            ${valves.map(v => `<tr><td>${esc(v.code)}</td><td>${esc(v.location)}</td><td>${esc(v.zone.name)}</td></tr>`).join('')}</tbody></table></div>
    </div>
    <div class="panel"><h3>历史管线资料</h3><table><thead><tr><th>管段编号</th><th>分区</th><th>管径</th><th>材质</th><th>敷设年份</th><th>道路</th></tr></thead><tbody>
        ${PIPES.map(p => `<tr><td>${esc(p.code)}</td><td>${esc(p.zone.name)}</td><td>DN${p.diameterMm}</td><td>${esc(p.material)}</td><td>${p.installYear}</td><td>${esc(p.roadName)}</td></tr>`).join('')}</tbody></table></div>
    <div class="panel"><h3>重点设施（小区 / 医院 / 学校 / 餐饮街 / 道路）</h3><table><thead><tr><th>名称</th><th>类型</th><th>分区</th><th>人口</th><th>高层</th><th>联系人</th><th>备注</th></tr></thead><tbody>
        ${FACILITIES.map(f => `<tr><td>${esc(f.name)}</td><td>${esc(label(f.type))}</td><td>${esc(f.zone.name)}</td><td>${f.population}</td>
        <td>${f.highRise ? '是' : '否'}</td><td>${esc(f.contactName) || '-'} ${esc(f.contactPhone) || ''}</td><td>${esc(f.note) || '-'}</td></tr>`).join('')}</tbody></table></div>`;
}

/* ---------------- 启动 ---------------- */
init().catch(e => {
    if (e.message !== '未登录') $('#main').innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>';
});
