package gcs.app.sipfw.model;

import lombok.Data;

/**
 * 质检抽检通话配对。
 */
@Data
public class SipQaCallPair {
    private final SipCallSession inboundCall;
    private final SipCallSession outboundCall;
    private final String matchedSuffix;
    private final long inboundDurationSeconds;

    private SipQaCallPair(SipCallSession inboundCall,
                          SipCallSession outboundCall,
                          String matchedSuffix,
                          long inboundDurationSeconds) {
        this.inboundCall = inboundCall;
        this.outboundCall = outboundCall;
        this.matchedSuffix = matchedSuffix;
        this.inboundDurationSeconds = inboundDurationSeconds;
    }

    public static SipQaCallPair matched(SipCallSession inboundCall,
                                         SipCallSession outboundCall,
                                         String matchedSuffix,
                                         long inboundDurationSeconds) {
        return new SipQaCallPair(inboundCall, outboundCall, matchedSuffix, inboundDurationSeconds);
    }

    public boolean isMatched() {
        return outboundCall != null;
    }
}
