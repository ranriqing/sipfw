package gcs.app.sipfw.output;

import gcs.app.sipfw.model.SipCallSession;
import gcs.app.sipfw.model.SipIterationReport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 控制台输出格式化器，保持与历史 groovy 报表风格一致。
 */
@Component
public class SipConsoleOutputFormatter {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss");

    private final String startTime = LocalDateTime.now().format(DTF);
    private final AtomicLong round = new AtomicLong(0L);

    public String formatReport(SipIterationReport report) {
        long currentRound = round.getAndIncrement();
        DecimalFormat pcf = new DecimalFormat("0.00%");
        StringBuilder sb = new StringBuilder();
        sb.append("sipfw  ").append(startTime).append("  ~  ").append(LocalDateTime.now().format(DTF)).append('\n').append('\n');
        appendHandlingBlock(sb, report, currentRound, pcf);
        appendProcessedBlock(sb, report, pcf);
        return sb.toString();
    }

    private void appendHandlingBlock(StringBuilder sb, SipIterationReport report, long currentRound, DecimalFormat pcf) {
        long handling = report.getHandling();
        long answered = report.getHandlingAnswered();
        long connected = report.getHandlingConnected();
        long trying = report.getHandlingTrying();
        String marks = repeat(">", (int) (currentRound % 8) + 1);
        sb.append(String.format("%-10s %-10s    %-8d%n", "handling", marks, handling));
        if (handling > 0L) {
            sb.append(String.format("%10s %-10s    %-8d %8s%n", "", "answered", answered, formatPercent(answered, handling, pcf)));
            sb.append(String.format("%10s %-10s    %-8d %8s%n", "", "connected", connected, formatPercent(connected, handling, pcf)));
            sb.append(String.format("%10s %-10s    %-8d %8s%n", "", "trying", trying, formatPercent(trying, handling, pcf)));
        }
        sb.append('\n');
    }

    private void appendProcessedBlock(StringBuilder sb, SipIterationReport report, DecimalFormat pcf) {
        long processed = report.getProcessed();
        Map<String, Long> dimensions = report.getDimensionCount();
        BigDecimal bdProcessed = BigDecimal.valueOf(processed);
        sb.append(String.format("%-10s %-10s    %-8d%n", "processed", "~~~~~~~~", processed));
        if (processed <= 0L) {
            sb.append('\n');
            return;
        }

        printNormalLine(sb, SipCallSession.TYPE_ANSWERED,
                readDimension(dimensions, SipCallSession.TYPE_ANSWERED) + readDimension(dimensions, "conv20m"),
                processed, processed, pcf);
        printNormalLine(sb, SipCallSession.TYPE_REINVITE, readDimension(dimensions, SipCallSession.TYPE_REINVITE), processed, null, pcf);

        Map<String, BigDecimal> prefixTotalMap = new LinkedHashMap<String, BigDecimal>();
        List<Map.Entry<String, Long>> sortedDimensions = new ArrayList<Map.Entry<String, Long>>(dimensions.entrySet()).stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).collect(Collectors.toList());
        sb.append('\n');
        long connsipTotal = sumPrefix(dimensions, SipCallSession.PREFIX_CONNSIP);
        printNormalLine(sb, "connected", connsipTotal, processed, null, pcf);
        prefixTotalMap.put(SipCallSession.PREFIX_CONNSIP, BigDecimal.valueOf(connsipTotal));
        printPrefixTopN(sb, sortedDimensions, SipCallSession.PREFIX_CONNSIP, "[C]", 3, bdProcessed, prefixTotalMap.get(SipCallSession.PREFIX_CONNSIP), pcf);

        sb.append('\n');
        long othersipTotal = sumPrefix(dimensions, SipCallSession.PREFIX_OTHERSIP);
        printNormalLine(sb, "others", othersipTotal, processed, null, pcf);
        prefixTotalMap.put(SipCallSession.PREFIX_OTHERSIP, BigDecimal.valueOf(othersipTotal));
        printPrefixTopN(sb, sortedDimensions, SipCallSession.PREFIX_OTHERSIP, "[O]", 5, bdProcessed, prefixTotalMap.get(SipCallSession.PREFIX_OTHERSIP), pcf);
        sb.append('\n');
    }

    private void printNormalLine(StringBuilder sb, String alias, long value,
                                 long processed, Long processedWithoutBlacklist, DecimalFormat pcf) {
        if (value <= 0L || processed <= 0L) {
            return;
        }
        String percent = formatPercent(value, processed, pcf);
        if (SipCallSession.TYPE_ANSWERED.equals(alias) && processedWithoutBlacklist != null && processedWithoutBlacklist > 0L && value <= processedWithoutBlacklist) {
            String correctPercent = formatPercent(value, processedWithoutBlacklist, pcf);
            sb.append(String.format("%10s %-10s    %-8d %8s   / %8s%n", "", alias, value, percent, correctPercent));
            return;
        }
        sb.append(String.format("%10s %-10s    %-8d %8s%n", "", alias, value, percent));
    }

    private void printPrefixTopN(StringBuilder sb, List<Map.Entry<String, Long>> sortedDimensions,
                                 String prefix, String aliasPrefix, int maxNum,
                                 BigDecimal processed, BigDecimal prefixTotal, DecimalFormat pcf) {
        if (prefixTotal == null || prefixTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int num = 0;
        for (Map.Entry<String, Long> entry : sortedDimensions) {
            if (num >= maxNum) {
                return;
            }
            String key = entry.getKey();
            long value = entry.getValue() == null ? 0L : entry.getValue();
            if (!key.startsWith(prefix) || value <= 0L) {
                continue;
            }
            num++;
            BigDecimal bdValue = BigDecimal.valueOf(value);
            String percent = pcf.format(bdValue.divide(processed, 4, RoundingMode.HALF_UP));
            String prefixPercent = pcf.format(bdValue.divide(prefixTotal, 4, RoundingMode.HALF_UP));
            String display = aliasPrefix + key.substring(prefix.length());
            sb.append(String.format("%10s %-10s    %-8d %8s   / %8s%n", "", display, value, percent, prefixPercent));
        }
    }

    private long sumPrefix(Map<String, Long> dimensions, String prefix) {
        long total = 0L;
        for (Map.Entry<String, Long> entry : dimensions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                total += readDimension(dimensions, entry.getKey());
            }
        }
        return total;
    }

    private long readDimension(Map<String, Long> dimensions, String key) {
        Long value = dimensions.get(key);
        return value == null ? 0L : value;
    }

    private String formatPercent(long numerator, long denominator, DecimalFormat pcf) {
        if (denominator <= 0L) {
            return pcf.format(0);
        }
        BigDecimal value = BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
        return pcf.format(value);
    }

    private String repeat(String value, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(value);
        }
        return sb.toString();
    }
}
