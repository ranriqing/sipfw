package gcs.modules.config.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.base.core.GcsConstants;
import gcs.modules.config.domain.ScConfig;
import org.springframework.stereotype.Service;

@Service
public class ScConfigService {

    public ScConfig getScConfigByKey(String scKey) {
        return Db.getOne(
                Wrappers.lambdaQuery(ScConfig.class).eq(ScConfig::getKey, scKey)
                        .orderByDesc(ScConfig::getId).last(GcsConstants.LIMIT_1)
        );
    }
}
