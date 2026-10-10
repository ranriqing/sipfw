package gcs.app.sipfw.model;

import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 通话会话（按 Call-ID 聚合）。
 */
@Data
public class SipCallSession {
    public static final String PREFIX_CONNSIP = "connsip";
    public static final String PREFIX_OTHERSIP = "othersip";
    public static final String TYPE_ANSWERED = "answered";
    public static final String TYPE_REINVITE = "reinvite";

    public static final Set<String> BLACKLIST_CODE = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("486", "603"))
    );

    private String callId;
    private String srcIp;
    private String dstIp;
    private String caller;
    private String callee;
    private CallDirection direction = CallDirection.UNKNOWN;

    private boolean reinvite;
    private boolean connected;
    private boolean canceled;
    private boolean answered = false;
    private boolean hangup = false;
    private String termCode;

    private Date startStamp;
    private Date answerStamp;
    private Date cancelStamp;
    private Date endStamp;

    public boolean isInvalid() {
        return callId == null || startStamp == null;
    }

    public boolean isInbound() {
        return direction == CallDirection.INBOUND;
    }

    public String getHangupType() {
        if (isInvalid() || !hangup) {
            return "none";
        }
        if (answered) {
            return TYPE_ANSWERED;
        }
        if (reinvite) {
            return TYPE_REINVITE;
        }
        if (connected) {
            if (canceled && cancelStamp != null) {
                long waitMillis = cancelStamp.getTime() - startStamp.getTime();
                if (waitMillis <= 15L * 1000L) {
                    return PREFIX_CONNSIP + "C15";
                }
                if (waitMillis <= 30L * 1000L) {
                    return PREFIX_CONNSIP + "C30";
                }
            }
            return PREFIX_CONNSIP + safeCode(termCode);
        }
        return PREFIX_OTHERSIP + safeCode(termCode);
    }

    private String safeCode(String code) {
        return code == null ? "UNK" : code;
    }
}
