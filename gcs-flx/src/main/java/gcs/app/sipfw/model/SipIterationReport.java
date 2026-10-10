package gcs.app.sipfw.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单轮统计快照。
 */
@Data
public class SipIterationReport {
    private long handling;
    private long handlingAnswered;
    private long handlingConnected;
    private long handlingTrying;
    private long processed;

    private BigDecimal handlingAnswerRate = BigDecimal.ZERO;
    private BigDecimal handlingConnectRate = BigDecimal.ZERO;
    private BigDecimal processedAnswerRate = BigDecimal.ZERO;

    // 只记录呼入通话
    private final Map<String, Long> dimensionCount = new LinkedHashMap<String, Long>();

    private final Map<String, SipGroupStats> inboundCallerGroup = new LinkedHashMap<String, SipGroupStats>();
    private final Map<String, SipGroupStats> inboundIpGroup = new LinkedHashMap<String, SipGroupStats>();
    private final Map<String, SipGroupStats> outboundCallerGroup = new LinkedHashMap<String, SipGroupStats>();
    private final Map<String, SipGroupStats> outboundIpGroup = new LinkedHashMap<String, SipGroupStats>();
}
