package gcs.app.sipfw.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SIP 分析运行参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip.analysis")
public class SipAnalysisOptions {
    /**
     * 是否启用 SIP 自动采集。
     */
    private boolean enabled = false;

    /**
     * sipgrep 可执行文件路径。
     */
    private String sipgrepPath = "/usr/local/bin/sipgrep";

    /**
     * 可选 from 过滤，空表示全量抓取。
     */
    private String fromFilter = "";

    /**
     * sip 端口范围。
     */
    private String portRange = "5060-19999";

    /**
     * 统计快照间隔（毫秒）。
     */
    private long reportIntervalMillis = 1000L;

    /**
     * 重连等待间隔（毫秒）。
     */
    private long restartDelayMillis = 3000L;

    /**
     * 是否输出控制台风格明细日志。
     */
    private boolean consoleLogEnabled = true;

    /**
     * 本机 IP 规则（兼容旧配置）：
     * 支持单条正则，或使用逗号/分号分隔多条规则。
     */
    private String localIpRegex = "127\\.0\\.0\\.\\d{1,3}";

    /**
     * 本机 IP 规则列表（推荐）。
     * 例如：
     * - 8\\.100\\.100\\.\\d{1,3}
     * - 10\\.10\\.\\d{1,3}\\.\\d{1,3}
     */
    private List<String> localIpRegexList = new ArrayList<String>();

    /**
     * 结束通话归档阈值，默认 5 分钟。
     */
    private long archiveAfterMillis = 5L * 60L * 1000L;

    /**
     * 超时通话归档阈值，默认 20 分钟。
     */
    private long forceArchiveAfterMillis = 20L * 60L * 1000L;

    /**
     * 定义缓存兜底刷新间隔（毫秒）。
     */
    private long definitionRefreshMillis = 60_000L;

    /**
     * 呼入分组兜底截断长度（0 表示不截断）。
     * 当呼入场景未命中前缀定义时，按该长度对号码取前缀作为分组 key。
     */
    private int inboundPrefixGroupMaxLength = 0;

    /**
     * 呼出分组兜底截断长度（0 表示不截断）。
     * 当呼出场景未命中前缀定义时，按该长度对号码取前缀作为分组 key。
     */
    private int outboundPrefixGroupMaxLength = 0;

    /**
     * 未匹配到分组定义时是否输出通话日志（SipCallSession）。
     */
    private boolean unmatchedGroupLogEnabled = false;

    /**
     * SIP 定义快照（YAML 配置来源），与 DB 记录统一合并后用于运行时匹配。
     */
    private List<SipDefinitionSnapshot> definitionSnapshotList = new ArrayList<SipDefinitionSnapshot>();
}
