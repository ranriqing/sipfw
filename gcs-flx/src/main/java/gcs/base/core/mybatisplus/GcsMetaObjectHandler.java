package gcs.base.core.mybatisplus;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.BaseDomain;
import gcs.base.shiro.spi.SpiUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Component
public class GcsMetaObjectHandler implements MetaObjectHandler {

    @Autowired
    private GcsShiroService shiroService;

    @Override
    public void insertFill(MetaObject metaObject) {
        SpiUser spiUser = null;
        if(ThreadContext.getSubject()!=null && SecurityUtils.getSubject().getPrincipal()!=null){
            spiUser = shiroService.currentUser();
        }
        Long spiUserId = spiUser!=null?spiUser.getId():0L;
        Date nowDate = DateUtil.date();
        this.strictInsertFill(metaObject, BaseDomain.CREATOR_ID, Long.class, spiUserId);
        this.strictUpdateFill(metaObject, BaseDomain.UPDATER_ID, Long.class, spiUserId);
        this.strictInsertFill(metaObject, BaseDomain.DATE_CREATED, Date.class, nowDate);
        this.strictUpdateFill(metaObject, BaseDomain.LAST_UPDATED, Date.class, nowDate);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        SpiUser spiUser = null;
        if(ThreadContext.getSubject()!=null && SecurityUtils.getSubject().getPrincipal()!=null){
            spiUser = shiroService.currentUser();
        }
        Long spiUserId = spiUser!=null?spiUser.getId():0L;
        Date nowDate = DateUtil.date();
        this.strictUpdateFill(metaObject, BaseDomain.UPDATER_ID, Long.class, spiUserId);
        this.strictUpdateFill(metaObject, BaseDomain.LAST_UPDATED, Date.class, nowDate);
    }

    @Override
    public MetaObjectHandler strictFillStrategy(MetaObject metaObject, String fieldName, Supplier<?> fieldVal) {
        Object obj = fieldVal.get();
        metaObject.setValue(fieldName, obj);
        if (Objects.nonNull(obj)) {
            metaObject.setValue(fieldName, obj);
        }
        return this;
    }
}