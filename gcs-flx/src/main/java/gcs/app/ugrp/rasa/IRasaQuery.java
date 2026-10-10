package gcs.app.ugrp.rasa;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import gcs.base.shiro.spi.SpiUser;

import java.util.Map;

public interface IRasaQuery<T> {

    static final String QUERY_SUFFIX = "_query";

    void wrapCondition(QueryWrapper<T> queryWrapper, SpiUser spiUser, Map<String, Object> context);

}
