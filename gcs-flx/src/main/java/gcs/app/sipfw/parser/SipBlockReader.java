package gcs.app.sipfw.parser;

import gcs.app.sipfw.model.SipMessageBlock;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 逐行读取并切分 SIP 区块。
 */
@Slf4j
public class SipBlockReader {
    private static final Pattern BLOCK_START = Pattern.compile("^(U\\s+\\d{4}/\\d\\d/\\d\\d).*$");

    @FunctionalInterface
    public interface BlockConsumer {
        void accept(SipMessageBlock block);
    }

    /** 此处持续读取，block在这里 */
    public void continueRead(BufferedReader reader, BlockConsumer consumer) throws IOException {
        String line;
        String infoLine = null;
        List<String> messageLines = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (BLOCK_START.matcher(line).matches()) {
                // 遇到下一个区块头时，把上一个完整区块提交给消费者。
                if (infoLine != null && !messageLines.isEmpty()) {
                    consumer.accept(new SipMessageBlock(infoLine, new ArrayList<String>(messageLines)));
                }
                infoLine = line;
                messageLines = new ArrayList<String>();
            } else {
                messageLines.add(line);
            }
        }
        if (infoLine != null && !messageLines.isEmpty()) {
            consumer.accept(new SipMessageBlock(infoLine, messageLines));
        }
    }
}
