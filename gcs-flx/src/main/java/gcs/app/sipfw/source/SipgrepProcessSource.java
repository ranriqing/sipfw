package gcs.app.sipfw.source;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * sipgrep 进程数据源，默认读取全量 SIP 消息（不加 -f 过滤）。
 */
@Getter
public class SipgrepProcessSource implements Closeable {
    private static final String DEFAULT_SIPGREP_PATH = "/usr/local/bin/sipgrep";
    private static final String DEFAULT_PORT_RANGE = "5060-19999";

    private final String command;
    private final Process process;
    private final BufferedReader reader;

    public SipgrepProcessSource(String sipgrepPath, String fromFilter, String portRange) throws IOException {
        this.command = buildCommand(sipgrepPath, fromFilter, portRange);
        this.process = Runtime.getRuntime().exec(command);
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    @Override
    public void close() throws IOException {
        try {
            reader.close();
        } finally {
            process.destroy();
        }
    }

    private String buildCommand(String sipgrepPath, String fromFilter, String portRange) {
        StringBuilder command = new StringBuilder();
        command.append(sipgrepPath == null || sipgrepPath.trim().isEmpty() ? DEFAULT_SIPGREP_PATH : sipgrepPath.trim());
        if (fromFilter != null && !fromFilter.trim().isEmpty()) {
            command.append(" -f ").append(fromFilter.trim());
        }
        command.append(" -P").append(portRange == null || portRange.trim().isEmpty() ? DEFAULT_PORT_RANGE : portRange.trim());
        command.append(" -C -d any");
        // return "/usr/local/bin/sipgrep -P5060-19999 -C -I /var/log/sip_message/dump-2026-03-12_103001.cap";
        return command.toString();
    }
}
