package gcs.app.sipfw.model;

/**
 * SIP 前缀定义适用方向。
 */
public enum SipDefinitionCallDirection {
    INBOUND,
    OUTBOUND;

    public static SipDefinitionCallDirection safeValueOf(String value) {
        if (value == null || value.trim().isEmpty()) {
            return INBOUND;
        }
        try {
            return SipDefinitionCallDirection.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return INBOUND;
        }
    }
}
