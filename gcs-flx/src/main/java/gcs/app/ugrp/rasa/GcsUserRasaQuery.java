package gcs.app.ugrp.rasa;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import gcs.app.ugrp.domain.GcsUser;
import gcs.base.shiro.spi.SpiUser;
import gcs.modules.log.domain.GcsLog;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("GcsUser_query")
public class GcsUserRasaQuery implements IRasaQuery<GcsUser>{

    public void wrapCondition(QueryWrapper<GcsUser> queryWrapper, SpiUser spiUser, Map<String, Object> context){
        LambdaQueryWrapper<GcsUser> lambdaQueryWrapper = queryWrapper.lambda();
        lambdaQueryWrapper.eq(GcsUser::getIsActive, true);
    }
}
