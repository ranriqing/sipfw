package gcs.app.sipfw.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分组统计信息（主叫分组或 IP 分组）。
 */
@Data
public class SipGroupStats {
    private Long definitionId;
    private String prefix;
    private String displayName;
    private String callDirection;

    private long processed;

    private long handling;
    private long answered;
    private long connected;
    private long trying;

    private final Map<String, Long> hangupTypeCount = new LinkedHashMap<String, Long>();
    private Map<String, String> specialSipCodeJson = new LinkedHashMap<String, String>();
    private boolean ratioTypeEnabled;
    private boolean qaEnabled;
    private Map<String, String> qaScriptJson = new LinkedHashMap<String, String>();
    private int qaFailCount;

    public void addHangupType(String hangupType, long delta) {
        Long current = hangupTypeCount.get(hangupType);
        hangupTypeCount.put(hangupType, (current == null ? 0L : current) + delta);
    }
}
