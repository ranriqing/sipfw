package gcs.app.sipfw.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SIP 前缀定义快照（运行时统一使用结构）。
 */
@Data
public class SipDefinitionSnapshot {
    private static final String IPV4_REGEX = "^\\d{1,3}(?:\\.\\d{1,3}){3}$";

    private Long id;
    private String prefix;
    private String displayName;
    private SipDefinitionCallDirection callDirection = SipDefinitionCallDirection.INBOUND;
    private boolean ratioTypeEnabled;
    private String specialSipCodeJson = "{}";
    private Map<String, String> specialSipCodeMap = new LinkedHashMap<String, String>();
    private boolean qaEnabled;
    private Map<String, String> qaScriptMap = new LinkedHashMap<String, String>();
    private List<SipQaFailRecord> qaFailRecords = new ArrayList<SipQaFailRecord>();

    @Data
    public static class SipQaFailRecord {
        private String inboundCaller;
        private String inboundCallee;
        private String outboundCaller;
        private String outboundCallee;
        private String recordUrl;
        private String transcript;
        private String summary;
        private long timestamp;
    }

    public String normalizedPrefix() {
        return prefix == null ? "" : prefix.trim();
    }

    public String displayGroupKey() {
        // String show = displayName == null ? "" : displayName.trim();
        // return show.isEmpty() ? normalizedPrefix() : show;
        return normalizedPrefix();
    }

    public boolean matchesDirection(CallDirection direction) {
        if (direction == CallDirection.INBOUND) {
            return callDirection == SipDefinitionCallDirection.INBOUND;
        }
        if (direction == CallDirection.OUTBOUND) {
            return callDirection == SipDefinitionCallDirection.OUTBOUND;
        }
        return false;
    }

    public boolean matchesNumber(String number) {
        String p = normalizedPrefix();
        if (p.isEmpty()) {
            return false;
        }
        if (isIpPrefix()) {
            return false;
        }

        String normalized = number == null ? "" : number.trim();
        if (normalized.startsWith(p)) {
            return true;
        }

        String noCountryCode = stripChinaCountryCode(toDigits(normalized));
        return noCountryCode.startsWith(p);
    }

    public boolean isIpPrefix() {
        return normalizedPrefix().matches(IPV4_REGEX);
    }

    public boolean matchesIp(String ip) {
        if (!isIpPrefix()) {
            return false;
        }
        String targetIp = ip == null ? "" : ip.trim();
        return !targetIp.isEmpty() && normalizedPrefix().equals(targetIp);
    }

    private String toDigits(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private String stripChinaCountryCode(String digits) {
        if (digits == null || digits.isEmpty()) {
            return "";
        }
        if (digits.startsWith("0086")) {
            return digits.substring(4);
        }
        if (digits.startsWith("86") && digits.length() > 2) {
            return digits.substring(2);
        }
        return digits;
    }
}
