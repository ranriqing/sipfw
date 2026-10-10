package gcs.app.sipfw.model;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * SIP 报文区块（对应 sipgrep 的一个 U 开头块）。
 */
@Getter
public class SipMessageBlock {
    private final String infoLine;
    private final List<String> messageLines;

    public SipMessageBlock(String infoLine, List<String> messageLines) {
        this.infoLine = infoLine;
        this.messageLines = messageLines == null ? Collections.<String>emptyList() : messageLines;
    }
}
