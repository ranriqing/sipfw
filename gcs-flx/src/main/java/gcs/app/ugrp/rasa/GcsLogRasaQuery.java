package gcs.app.ugrp.rasa;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import gcs.modules.log.domain.GcsLog;
import gcs.base.shiro.spi.SpiUser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("GcsLog_query")
public class GcsLogRasaQuery implements IRasaQuery<GcsLog>{

    public void wrapCondition(QueryWrapper<GcsLog> queryWrapper, SpiUser spiUser, Map<String, Object> context){
        LambdaQueryWrapper<GcsLog> lambdaQueryWrapper = queryWrapper.lambda();
        if(spiUser.getId() == 1){
            // do query1
        }else if(spiUser.getId() == 2){
            // do query2
        }
        lambdaQueryWrapper.eq(GcsLog::getOperation, "测试");
    }
}
