package gcs.app.sipfw.service;

import gcs.app.sipfw.model.CallDirection;
import gcs.app.sipfw.model.SipAnalysisOptions;
import gcs.app.sipfw.model.SipCallSession;
import gcs.app.sipfw.model.SipDefinitionSnapshot;
import gcs.app.sipfw.model.SipGroupStats;
import gcs.app.sipfw.model.SipIterationReport;
import gcs.app.sipfw.model.SipMessageBlock;
import gcs.app.sipfw.model.SipQaCallPair;
import gcs.app.sipfw.parser.SipBlockReader;
import gcs.app.sipfw.parser.SipSessionUpdater;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * SIP 统计引擎，支持迭代归档和分组统计。
 */
@Slf4j
@Getter
public class SipAnalysisEngine {
    private static final String DIM_PROCESSED = "processed";
    private static final String DIM_CONV_20M = "conv20m";
    private static final String DIM_ANSWERED = "answered";
    private static final String NONE_CALLER = "NONE_CALLER";
    private static final String NONE_IP = "NONE_IP";

    private final SipAnalysisOptions options;
    private final SipDefinitionSnapshotService definitionSnapshotService;
    // 主线程阻塞读取，不间断完善通话
    private final Map<String, SipCallSession> activeCallMap = new ConcurrentHashMap<String, SipCallSession>();
    // 报告线程持续归档，统计不同状态通话
    private final Map<String, Long> archivedInboundMap = new ConcurrentHashMap<String, Long>();

    public SipAnalysisEngine(SipAnalysisOptions options, SipDefinitionSnapshotService definitionSnapshotService) {
        this.options = options;
        this.definitionSnapshotService = definitionSnapshotService;
    }

    /**
     * 持续消费 SIP 报文，并按固定时间间隔输出迭代统计结果。
     */
    public void consumeWithIteration(BufferedReader reader,
                                     long reportIntervalMillis,
                                     Consumer<SipIterationReport> reportConsumer) throws IOException {
        long interval = reportIntervalMillis > 0L ? reportIntervalMillis : 3000L;
        // 1) 先启动周期性快照线程。
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> reportConsumer.accept(buildIterationReport()),
                interval,
                interval,
                TimeUnit.MILLISECONDS
        );
        // 2) 在当前线程阻塞读取 reader
        try {
            SipSessionUpdater updater = new SipSessionUpdater(options);
            SipBlockReader blockReader = new SipBlockReader();
            blockReader.continueRead(reader, block -> processBlock(updater, block));
            // Sipgrep退出的最终快照
            reportConsumer.accept(buildIterationReport());
        } finally {
            scheduler.shutdownNow();
        }
    }

    private void processBlock(SipSessionUpdater updater, SipMessageBlock block) {
        String callId = updater.extractCallId(block);
        if (callId == null || callId.trim().isEmpty()) {
            return;
        }
        SipCallSession session = activeCallMap.computeIfAbsent(callId, id -> {
            SipCallSession created = new SipCallSession();
            created.setCallId(id);
            return created;
        });
        updater.enrich(session, block);
        if (session.isInvalid()) {
            activeCallMap.remove(callId);
        }
    }

    public SipIterationReport buildIterationReport() {
        long now = System.currentTimeMillis();
        archiveExpiredCalls(now);
        return buildLiveReport();
    }

    private void archiveExpiredCalls(long nowMillis) {
        long beforeArchive = nowMillis - options.getArchiveAfterMillis();
        long beforeForceArchive = nowMillis - options.getForceArchiveAfterMillis();
        Iterator<Map.Entry<String, SipCallSession>> iterator = activeCallMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SipCallSession> entry = iterator.next();
            SipCallSession call = entry.getValue();
            if (call.isInvalid()) {
                iterator.remove();
                continue;
            }
            Date startStamp = call.getStartStamp();
            if (startStamp == null) {
                iterator.remove();
                continue;
            }
            if (startStamp.getTime() < beforeArchive) {
                if (call.isHangup()) {
                    iterator.remove();
                    if(call.isInbound()){  // 存档呼入通话
                        incArchived(DIM_PROCESSED, 1L);
                        incArchived(call.getHangupType(), 1L);
                    }
                } else if(!call.isAnswered()){
                    iterator.remove();
                    if(call.isInbound()){   // 5分钟未接通
                        incArchived(DIM_PROCESSED, 1L);
                        incArchived(call.getHangupType(), 1L);
                    }
                } else if (startStamp.getTime() < beforeForceArchive) {
                    iterator.remove();
                    if(call.isInbound()){ // 存档呼入通话
                        incArchived(DIM_CONV_20M, 1L);
                        incArchived(DIM_PROCESSED, 1L);
                    }
                }
            }
        }
    }

    private SipIterationReport buildLiveReport() {
        SipIterationReport report = new SipIterationReport();
        // 已归档通话(只统计呼入)
        report.getDimensionCount().putAll(new LinkedHashMap<String, Long>(archivedInboundMap));
        // 未归档通话
        for (SipCallSession call : activeCallMap.values()) {
            if (call.isInvalid()) {
                continue;
            }
            // 统计呼入通话
            if(call.isInbound()){
                if (call.isHangup()) {
                    incMap(report.getDimensionCount(), DIM_PROCESSED, 1L);
                    incMap(report.getDimensionCount(), call.getHangupType(), 1L);
                } else {
                    increaseHandlingStats(report, call);
                }
            }
            // 统计全部通话(呼入&呼出)
            updateDirectionalGroups(report, call);
        }
        finalizeInboundStats(report);
        return report;
    }

    private void increaseHandlingStats(SipIterationReport report, SipCallSession call) {
        report.setHandling(report.getHandling() + 1L);
        if (call.isAnswered()) {
            report.setHandlingAnswered(report.getHandlingAnswered() + 1L);
            return;
        }
        if (call.isConnected()) {
            report.setHandlingConnected(report.getHandlingConnected() + 1L);
        }
    }

    /**
     * 汇总主指标并计算比率，集中在一个方法中更容易核对统计口径。
     */
    private void finalizeInboundStats(SipIterationReport report) {
        report.setProcessed(getDimension(report.getDimensionCount(), DIM_PROCESSED));
        report.setHandlingTrying(report.getHandling() - report.getHandlingAnswered() - report.getHandlingConnected());
        report.setHandlingAnswerRate(safeRate(report.getHandlingAnswered(), report.getHandling()));
        report.setHandlingConnectRate(safeRate(report.getHandlingConnected(), report.getHandling()));
        report.setProcessedAnswerRate(safeRate(getDimension(report.getDimensionCount(), DIM_ANSWERED), report.getProcessed()));
    }

    /**
     * 解析呼入通话匹配的前缀定义，未匹配返回 null。
     */
    private SipDefinitionSnapshot resolveInboundDefinition(SipCallSession call) {
        return definitionSnapshotService.matchDefinition(
                call.getCaller(), CallDirection.INBOUND, call.getSrcIp());
    }

    /**
     * 解析呼入通话的 callerKey，与统计分组逻辑保持一致。
     */
    private String resolveInboundCallerKey(SipCallSession call) {
        SipDefinitionSnapshot def = definitionSnapshotService.matchDefinition(
                call.getCaller(),
                CallDirection.INBOUND,
                call.getSrcIp()
        );
        String callerKey = resolveGroupKey(call.getCaller(), def, NONE_CALLER);
        if (def == null) {
            callerKey = truncateGroupKey(callerKey, NONE_CALLER, options.getInboundPrefixGroupMaxLength());
            logUnmatchedGroup("INBOUND", call);
        }
        return callerKey;
    }

    /**
     * 按方向分配到不同分组池：inbound/outbound 各自维护 caller/ip 两套统计。
     * 如果主叫/被叫匹配特定前缀，则按前缀作为 key 分组统计。
     */
    private void updateDirectionalGroups(SipIterationReport report, SipCallSession call) {
        CallDirection direction = call.getDirection();
        if (direction == CallDirection.INBOUND) {
            SipDefinitionSnapshot def = definitionSnapshotService.matchDefinition(
                    call.getCaller(),
                    CallDirection.INBOUND,
                    call.getSrcIp()
            );
            String callerKey = resolveInboundCallerKey(call);
            updateGroup(report.getInboundCallerGroup(), callerKey, call, def);
            updateGroup(report.getInboundIpGroup(), resolveGroupIp(call), call);
            return;
        }
        if (direction == CallDirection.OUTBOUND) {
            SipDefinitionSnapshot def = definitionSnapshotService.matchDefinition(
                    call.getCaller(),
                    CallDirection.OUTBOUND,
                    call.getDstIp()
            );
            String callerKey = resolveGroupKey(call.getCaller(), def, NONE_CALLER);
            if (def == null) {
                callerKey = truncateGroupKey(callerKey, NONE_CALLER, options.getOutboundPrefixGroupMaxLength());
                logUnmatchedGroup("OUTBOUND", call);
            }
            updateGroup(report.getOutboundCallerGroup(), callerKey, call, def);
            updateGroup(report.getOutboundIpGroup(), resolveGroupIp(call), call);
            return;
        }
        if (direction == CallDirection.UNKNOWN) {
            // UNKNOWN 方向不丢弃，统一归档到 inbound 侧的 NONE_DIRECTION 分组便于排查。
            // String unknownCallerKey = normalize(call.getCaller(), NONE_DIRECTION + ":" + NONE_CALLER);
            // updateGroup(report.getInboundCallerGroup(), unknownCallerKey, call);
            // updateGroup(report.getInboundIpGroup(), NONE_DIRECTION + ":" + resolveGroupIp(call), call);
        }
    }

    private String resolveGroupKey(String number,
                                   SipDefinitionSnapshot definition,
                                   String fallback) {
        if (definition != null) {
            String key = definition.displayGroupKey();
            if (key != null && !key.trim().isEmpty()) {
                return key.trim();
            }
        }
        return normalize(number, fallback);
    }

    private String truncateGroupKey(String groupKey, String fallback, int maxPrefixLength) {
        if (groupKey == null || groupKey.trim().isEmpty()) {
            return fallback;
        }
        String normalized = groupKey.trim();
        if (normalized.equals(fallback)) {
            return normalized;
        }
        if (maxPrefixLength <= 0 || normalized.length() <= maxPrefixLength) {
            return normalized;
        }
        return normalized.substring(0, maxPrefixLength);
    }

    private void logUnmatchedGroup(String scene, SipCallSession call) {
        if (!options.isUnmatchedGroupLogEnabled()) {
            return;
        }
        log.info("sip group unmatched, scene={}, call={}", scene, call);
    }

    private void updateGroup(Map<String, SipGroupStats> groupMap, String groupKey, SipCallSession call) {
        updateGroup(groupMap, groupKey, call, null);
    }

    private void updateGroup(Map<String, SipGroupStats> groupMap, String groupKey, SipCallSession call, SipDefinitionSnapshot definition) {
        SipGroupStats stats = groupMap.computeIfAbsent(groupKey, key -> new SipGroupStats());
        if (definition != null) {
            stats.setDefinitionId(definition.getId());
            stats.setPrefix(definition.getPrefix());
            stats.setDisplayName(definition.getDisplayName());
            stats.setCallDirection(definition.getCallDirection().name());
            stats.setRatioTypeEnabled(definition.isRatioTypeEnabled());
            stats.setSpecialSipCodeJson(new LinkedHashMap<String, String>(definition.getSpecialSipCodeMap()));
            stats.setQaEnabled(definition.isQaEnabled());
            stats.setQaScriptJson(new LinkedHashMap<String, String>(definition.getQaScriptMap()));
            stats.setQaFailCount(definition.getQaFailRecords() != null ? definition.getQaFailRecords().size() : 0);
        }
        if (call.isHangup()) {
            stats.setProcessed(stats.getProcessed() + 1L);
            stats.addHangupType(call.getHangupType(), 1L);
            if (DIM_ANSWERED.equals(call.getHangupType())) {
                stats.setAnswered(stats.getAnswered() + 1L);
            }
        } else {
            // 处理中通话
            stats.setHandling(stats.getHandling() + 1L);
            if (call.isAnswered()) {
                stats.setAnswered(stats.getAnswered() + 1L);
            } else if (call.isConnected()) {
                stats.setConnected(stats.getConnected() + 1L);
            } else {
                stats.setTrying(stats.getTrying() + 1L);
            }
        }
    }

    /** 工具方法 */
    private void incArchived(String key, long delta) {
        archivedInboundMap.merge(key, delta, Long::sum);
    }

    private void incMap(Map<String, Long> map, String key, long delta) {
        map.merge(key, delta, Long::sum);
    }

    private BigDecimal safeRate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private String resolveGroupIp(SipCallSession call) {
        // 统一按“对端 IP”分组：
        if (call.getDirection() == CallDirection.INBOUND) {
            return normalize(call.getSrcIp(), NONE_IP);
        }
        if (call.getDirection() == CallDirection.OUTBOUND) {
            return normalize(call.getDstIp(), NONE_IP);
        }
        return normalize(call.getSrcIp(), normalize(call.getDstIp(), NONE_IP));
    }

    private String normalize(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private long getDimension(Map<String, Long> map, String key) {
        Long value = map.get(key);
        return value == null ? 0L : value;
    }

    /**
     * 提取号码中纯数字的后 n 位，用于被叫匹配。
     */
    static String lastDigits(String number, int n) {
        if (number == null || number.isEmpty()) {
            return null;
        }
        StringBuilder digits = new StringBuilder(number.length());
        for (int i = 0; i < number.length(); i++) {
            char ch = number.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            }
        }
        if (digits.length() < n) {
            return null;
        }
        return digits.substring(digits.length() - n);
    }

    private static final int MATCH_SUFFIX_LENGTH = 11;

    /**
     * 从活跃通话中抽取质检样本，按 callerKey 分组，每组取固定数量，
     * 按被叫后 11 位匹配已接通的呼出通话，未匹配丢弃。
     *
     * @param perGroupCount        每组抽取数量
     * @param minDurationSeconds   最小时长（秒）
     * @param allowedOutboundIps   允许的呼出被叫 IP 集合，呼出 dstIp 不在此集合则跳过；为 null 或空则不过滤
     * @return 匹配成功的通话配对列表
     */
    public List<SipQaCallPair> extractCallsForQA(int perGroupCount, int minDurationSeconds, Set<String> allowedOutboundIps) {
        long minDurationMillis = minDurationSeconds * 1000L;
        Set<String> filterIps = (allowedOutboundIps == null || allowedOutboundIps.isEmpty()) ? null : allowedOutboundIps;

        // 1) 按 callerKey 分组收集符合条件的呼入通话
        Map<String, List<SipCallSession>> groupedCandidates = new LinkedHashMap<String, List<SipCallSession>>();
        for (SipCallSession call : activeCallMap.values()) {
            if (call.isInvalid() || !call.isInbound() || !call.isHangup()) {
                continue;
            }
            if (call.getAnswerStamp() == null || call.getEndStamp() == null) {
                continue;
            }
            long duration = call.getEndStamp().getTime() - call.getAnswerStamp().getTime();
            if (duration < minDurationMillis) {
                continue;
            }
            // 只抽检开启质检且有质检规则的前缀分组
            SipDefinitionSnapshot def = resolveInboundDefinition(call);
            if (def == null || !def.isQaEnabled() || def.getQaScriptMap().isEmpty()) {
                continue;
            }
            String callerKey = resolveInboundCallerKey(call);
            groupedCandidates.computeIfAbsent(callerKey, k -> new ArrayList<SipCallSession>()).add(call);
        }

        // 2) 每组取前 perGroupCount 通
        List<SipCallSession> selected = new ArrayList<SipCallSession>();
        for (List<SipCallSession> group : groupedCandidates.values()) {
            int count = Math.min(perGroupCount, group.size());
            selected.addAll(group.subList(0, count));
        }

        // 3) 构建呼出被叫后 11 位 → 已接通呼出通话 的索引（按 dstIp 过滤）
        Map<String, SipCallSession> outboundBySuffix = new LinkedHashMap<String, SipCallSession>();
        for (SipCallSession call : activeCallMap.values()) {
            if (call.isInvalid() || call.getDirection() != CallDirection.OUTBOUND || !call.isAnswered()) {
                continue;
            }
            if (filterIps != null && !filterIps.contains(call.getDstIp())) {
                continue;
            }
            String suffix = lastDigits(call.getCallee(), MATCH_SUFFIX_LENGTH);
            if (suffix != null) {
                outboundBySuffix.putIfAbsent(suffix, call);
            }
        }

        // 4) 匹配，未匹配丢弃
        List<SipQaCallPair> result = new ArrayList<SipQaCallPair>();
        for (SipCallSession inbound : selected) {
            String suffix = lastDigits(inbound.getCallee(), MATCH_SUFFIX_LENGTH);
            if (suffix == null) {
                continue;
            }
            SipCallSession outbound = outboundBySuffix.get(suffix);
            if (outbound == null) {
                continue;
            }
            long duration = inbound.getEndStamp().getTime() - inbound.getAnswerStamp().getTime();
            result.add(SipQaCallPair.matched(inbound, outbound, suffix, duration / 1000L));
        }
        return result;
    }

}
