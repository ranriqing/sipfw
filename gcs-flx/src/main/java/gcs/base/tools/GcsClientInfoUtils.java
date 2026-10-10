package gcs.base.tools;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;

/**
 * 从 User-Agent 中提取简略的客户端信息（浏览器、操作系统、设备类型）
 */
public class GcsClientInfoUtils {

    private GcsClientInfoUtils() {}

    /**
     * 解析 User-Agent，返回格式如：
     * Chrome/120.0.6099 (Windows 10)
     * Safari/17.2 (macOS)
     * Chrome/120.0 (Android 13, Mobile)
     */
    public static String parseBrief(String userAgentString) {
        if (userAgentString == null || userAgentString.isEmpty()) {
            return "";
        }
        try {
            UserAgent ua = UserAgentUtil.parse(userAgentString);
            StringBuilder sb = new StringBuilder();
            sb.append(ua.getBrowser().getName());
            String version = ua.getVersion();
            if (version != null && !version.isEmpty()) {
                sb.append('/').append(version);
            }
            sb.append(" (");
            sb.append(ua.getOs().getName());
            if (ua.isMobile()) {
                sb.append(", Mobile");
            }
            sb.append(')');
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
