package gcs.app.ugrp.service;


import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.code.kaptcha.Producer;
import gcs.app.ugrp.domain.GcsCaptcha;
import gcs.app.ugrp.mapper.GcsCaptchaMapper;
import gcs.base.core.exception.GcsException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class GcsCaptchaService extends ServiceImpl<GcsCaptchaMapper, GcsCaptcha> {

    @Autowired
    private Producer producer;

    /**
     * 获取图片验证码
     */
    public BufferedImage genCaptcha(String uuid) {
        if(StringUtils.isBlank(uuid)){
            throw new GcsException("uuid不能为空");
        }
        String code = producer.createText();
        // 保存验证码
        GcsCaptcha gcsCaptcha = new GcsCaptcha();
        gcsCaptcha.setUuid(uuid);
        gcsCaptcha.setCode(code);
        gcsCaptcha.setExpireTime(DateUtil.date().offset(DateField.MINUTE, 5)); // 5分钟后过期
        this.save(gcsCaptcha);
        return producer.createImage(code);
    }

    /**
     * 验证码效验
     * @param uuid  uuid
     * @param code  验证码
     * @return  true：成功  false：失败
     */
    public boolean validate(String uuid, String code) {
        GcsCaptcha gcsCaptcha = this.getById(uuid);
        if(gcsCaptcha == null){
            return false;
        }
        this.removeById(uuid);  // 使用则失效
        if(gcsCaptcha.getCode().equalsIgnoreCase(code)
                && gcsCaptcha.getExpireTime().getTime() >= System.currentTimeMillis()){
            return true;
        }
        return false;
    }
}
