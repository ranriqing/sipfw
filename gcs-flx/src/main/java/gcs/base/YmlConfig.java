package gcs.base;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;


@Data
@Component
// @ConfigurationProperties(prefix = "gcs")
public class YmlConfig {

    @Autowired
    private DevConfig devConfig;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.mail.fromUser}")
    private String mailFromUser;

    /** 基础目录 */
    @Value("${gcs.rootBase}")
    private String rootBase;

    /** 记录日志 */
    @Value("${gcs.logFilter.logPostParams}")
    private Boolean logFilterLogPostParams;

    @Value("${gcs.fileStorage.rootDir}")
    private String fileStorageRootDir;

    @Value("${gcs.tempStorage.rootDir}")
    private String tempStorageRootDir;

    /** 头像上传 */
    @Value("${gcs.toolsAvatar.rootDir}")
    private String toolsAvatarRootDir;

    @Value("${gcs.toolsAvatar.randStrategy:fdimgs}")
    private String toolsAvatarRandStrategy;

    public boolean isDevelopment() {
        return "dev".equals(activeProfile);
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "gcs-dev")
    public class DevConfig {
        private List<String> loginUsernameList;
        private List<String> saleUsernameList;
    }
}
