package gcs.modules.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatConfig {

    private String appid;
    private String secret;
    private String token;
    private String aeskey;

    /** 微信用户允许访问的权限标识列表 */
    private List<String> permissions = new ArrayList<>();

    /** 微信用户默认视图ID */
    private Long viewId;
}
