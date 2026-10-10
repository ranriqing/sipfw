package gcs.app.sipfw.parser;

import gcs.app.sipfw.model.CallDirection;
import gcs.app.sipfw.model.SipAnalysisOptions;
import gcs.app.sipfw.model.SipCallSession;
import gcs.app.sipfw.model.SipMessageBlock;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 根据 SIP 区块更新通话会话状态。
 */
@Slf4j
public class SipSessionUpdater {
    private static final Pattern BLOCK_CALLID = Pattern.compile("^Call-ID:\\s+(\\S+)\\.$");
    private static final Pattern INFO_TIME = Pattern.compile("^U\\s+(\\d{4}/\\d\\d/\\d\\d\\s+\\d\\d:\\d\\d:\\d\\d\\.\\d{3})\\d{3}\\s+.*$");
    private static final Pattern INFO_IP_PORT = Pattern.compile("^.*\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{4,5})\\s+->\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{4,5})$");
    private static final Pattern SIP_LINE = Pattern.compile("^SIP/2\\.0\\s+(\\d{3})\\s+.*$");
    private static final Pattern INVITE_FROM = Pattern.compile("^From:.*<(sip|tel):(\\+?[^@>]+)(?:@[^>]+)?>.*$");
    private static final Pattern INVITE_TO   = Pattern.compile("^To:.*<(sip|tel):(\\+?[^@>]+)(?:@[^>]+)?>.*$");

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private final List<Pattern> localIpPatterns;

    public SipSessionUpdater(SipAnalysisOptions options) {
        this.localIpPatterns = buildLocalIpPatterns(options);
    }

    public String extractCallId(SipMessageBlock block) {
        String callIdLine = findLineByPattern(block.getMessageLines(), BLOCK_CALLID);
        if (callIdLine == null) {
            return null;
        }
        Matcher matcher = BLOCK_CALLID.matcher(callIdLine);
        return matcher.matches() ? matcher.group(1) : null;
    }

    public void enrich(SipCallSession session, SipMessageBlock block) {
        String infoLine = block.getInfoLine();
        List<String> messageLines = block.getMessageLines();
        if (messageLines.isEmpty()) {
            return;
        }

        String sipFirstLine = messageLines.get(0);
        // 这些消息不改变会话关键状态，直接跳过。
        if (isIgnorableFirstLine(sipFirstLine)) {
            return;
        }

        if (sipFirstLine.startsWith("INVITE sip:")) {
            // 处理方向
            Matcher ipMatcher = INFO_IP_PORT.matcher(infoLine);
            if (!ipMatcher.matches()) {
                return;
            }
            String srcIp = ipMatcher.group(1);
            String dstIp = ipMatcher.group(3);
            // 方向只在首个 INVITE（或方向未知）时确定，避免被后续 reINVITE 覆盖。
            if (session.getStartStamp() == null || session.getDirection() == CallDirection.UNKNOWN) {
                session.setDirection(resolveDirection(session.getCallId(), srcIp, dstIp));
            }
            // 处理Invite
            processInvite(session, infoLine, messageLines, srcIp, dstIp);
            return;
        }

        if (sipFirstLine.startsWith("SIP/2.0 200 OK")) {
            process200Ok(session, infoLine, messageLines);
            return;
        }

        if (sipFirstLine.matches("^SIP/2\\.0 18\\d\\s+.*")) {
            session.setConnected(true);
            return;
        }

        if (sipFirstLine.matches("^SIP/2\\.0 (4|5|6)\\d\\d\\s+.*")) {
            Date eventTime = parseEventTime(infoLine);
            if (eventTime != null) {
                session.setEndStamp(eventTime);
            }
            Matcher codeMatcher = SIP_LINE.matcher(sipFirstLine);
            if (codeMatcher.matches()) {
                session.setTermCode(codeMatcher.group(1));
                session.setHangup(true);
            }
        }
    }

    private void processInvite(SipCallSession session, String infoLine, List<String> messageLines, String srcIp, String dstIp) {
        if (session.getStartStamp() != null) {
            // 同一 Call-ID 二次 INVITE，按脚本逻辑标记为 reinvite。
            session.setReinvite(true);
            return;
        }
        session.setSrcIp(srcIp);
        session.setDstIp(dstIp);
        Date eventTime = parseEventTime(infoLine);
        if (eventTime != null) {
            session.setStartStamp(eventTime);
        }
        String fromLine = findFirstPrefix(messageLines, "From:");
        Matcher fromMatcher = fromLine == null ? null : INVITE_FROM.matcher(fromLine);
        if (fromMatcher != null && fromMatcher.matches()) {
            session.setCaller(fromMatcher.group(2));
        }
        String toLine = findFirstPrefix(messageLines, "To:");
        Matcher toMatcher = toLine == null ? null : INVITE_TO.matcher(toLine);
        if (toMatcher != null && toMatcher.matches()) {
            session.setCallee(toMatcher.group(2));
        }
    }

    private void process200Ok(SipCallSession session, String infoLine, List<String> messageLines) {
        String cSeqLine = findFirstPrefix(messageLines, "CSeq:");
        if (cSeqLine == null) {
            return;
        }
        if (cSeqLine.contains("REGISTER") || cSeqLine.contains("PRACK") || cSeqLine.contains("OPTIONS")) {
            return;
        }
        Date eventTime = parseEventTime(infoLine);
        if (cSeqLine.contains("INVITE")) {
            session.setAnswered(true);
            if (eventTime != null) {
                session.setAnswerStamp(eventTime);
            }
        } else if (cSeqLine.contains("CANCEL")) {
            session.setCanceled(true);
            if (eventTime != null) {
                session.setCancelStamp(eventTime);
            }
        } else if (cSeqLine.contains("BYE")) {
            session.setHangup(true);
            session.setTermCode("200");
            if (eventTime != null) {
                session.setEndStamp(eventTime);
            }
        }
    }

    private CallDirection resolveDirection(String callId, String srcIp, String dstIp) {
        if (isLocalHost(srcIp)) {
            return CallDirection.OUTBOUND;
        }
        if (isLocalHost(dstIp)) {
            return CallDirection.INBOUND;
        }
        return CallDirection.UNKNOWN;
    }

    private List<Pattern> buildLocalIpPatterns(SipAnalysisOptions options) {
        List<Pattern> patterns = new ArrayList<Pattern>();
        List<String> regexList = options.getLocalIpRegexList();
        if (regexList != null) {
            for (String item : regexList) {
                addPattern(patterns, item);
            }
        }
        if (patterns.isEmpty()) {
            return Collections.singletonList(Pattern.compile("127\\.0\\.0\\.\\d{1,3}"));
        }
        return patterns;
    }

    private void addPattern(List<Pattern> patterns, String regex) {
        if (regex == null) {
            return;
        }
        String value = regex.trim();
        if (value.isEmpty()) {
            return;
        }
        patterns.add(Pattern.compile(value));
    }

    private boolean isLocalHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }
        for (Pattern pattern : localIpPatterns) {
            if (pattern.matcher(host).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isIgnorableFirstLine(String firstLine) {
        return firstLine.startsWith("OPTIONS sip:")
                || firstLine.startsWith("REGISTER sip:")
                || firstLine.startsWith("SUBSCRIBE sip:")
                || firstLine.startsWith("UPDATE sip:")
                || firstLine.startsWith("PRACK sip:")
                || firstLine.startsWith("CANCEL sip:")
                || firstLine.startsWith("BYE sip:")
                || firstLine.startsWith("ACK sip:")
                || firstLine.matches("^SIP/2\\.0 100\\s+.*");
    }

    private Date parseEventTime(String infoLine) {
        Matcher matcher = INFO_TIME.matcher(infoLine);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return sdf.parse(matcher.group(1));
        } catch (ParseException e) {
            return null;
        }
    }

    private String findLineByPattern(List<String> lines, Pattern pattern) {
        for (String line : lines) {
            if (pattern.matcher(line).matches()) {
                return line;
            }
        }
        return null;
    }

    private String findFirstPrefix(List<String> lines, String prefix) {
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        return null;
    }
}
