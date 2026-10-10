package gcs.app.ugrp.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import gcs.app.ugrp.rasa.GcsDftRasaQuery;
import gcs.app.ugrp.rasa.IRasaQuery;
import gcs.base.shiro.spi.SpiUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GcsRasaService {

    // 实现IRasaQuery接口的所有Bean实例
    private final Map<String, IRasaQuery> rasaQueryMap;

    @Autowired
    private GcsShiroService gcsShiroService;

    @Autowired
    private GcsDftRasaQuery gcsDftRasaQuery;

    @Autowired
    public GcsRasaService(ApplicationContext applicationContext) {
        this.rasaQueryMap = applicationContext.getBeansOfType(IRasaQuery.class);
    }

    public <T> QueryWrapper<T> query(QueryWrapper<T> queryWrapper, String permission, Map<String, Object> context) {
        IRasaQuery rasaQuery = rasaQueryMap.get(permission + IRasaQuery.QUERY_SUFFIX);
        if(rasaQuery == null){ rasaQuery = gcsDftRasaQuery; }
        SpiUser spiUser = gcsShiroService.currentUser();
        rasaQuery.wrapCondition(queryWrapper, spiUser, context);
        return queryWrapper;
    }
}
