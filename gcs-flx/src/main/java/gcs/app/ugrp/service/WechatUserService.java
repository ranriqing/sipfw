package gcs.app.ugrp.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.domain.WechatUser;
import gcs.app.ugrp.mapper.WechatUserMapper;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class WechatUserService {

    @Autowired
    private WechatUserMapper wechatUserMapper;

    /**
     * 关注事件：保存或更新粉丝信息
     */
    public boolean subscribeWxUser(WxMpUser wxMpUser) {
        return saveOrUpdateWxUser(wxMpUser);
    }

    /**
     * 取消关注事件：置为非关注状态
     */
    public boolean unsubscribeWxUser(String openId) {
        try {
            WechatUser wechatUser = wechatUserMapper.selectOne(
                    Wrappers.lambdaQuery(WechatUser.class).eq(WechatUser::getWxOpenId, openId));
            if (wechatUser == null) {
                return false;
            }
            wechatUser.setWxSubscribe(false);
            wechatUserMapper.updateById(wechatUser);
            return true;
        } catch (Exception e) {
            log.error("取消关注同步失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 保存或更新粉丝信息
     */
    public boolean saveOrUpdateWxUser(WxMpUser wxMpUser) {
        try {
            WechatUser wechatUser = wechatUserMapper.selectOne(
                    Wrappers.lambdaQuery(WechatUser.class).eq(WechatUser::getWxOpenId, wxMpUser.getOpenId()));
            if (wechatUser == null) {
                wechatUser = new WechatUser();
                wechatUser.setWxOpenId(wxMpUser.getOpenId());
            }
            wechatUser.setWxUnionId(wxMpUser.getUnionId());
            wechatUser.setWxNickname(wxMpUser.getNickname());
            wechatUser.setWxHeadImgUrl(wxMpUser.getHeadImgUrl());
            wechatUser.setWxSubscribe(wxMpUser.getSubscribe());
            if (wxMpUser.getSubscribeTime() != null) {
                wechatUser.setWxSubscribeTime(new Date(wxMpUser.getSubscribeTime() * 1000));
            }
            if (wechatUser.getId() == null) {
                wechatUserMapper.insert(wechatUser);
            } else {
                wechatUserMapper.updateById(wechatUser);
            }
            return true;
        } catch (Exception e) {
            log.error("粉丝信息同步失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 通过OpenId获取微信用户
     */
    public WechatUser getWechatUserByOpenId(String openId) {
        return wechatUserMapper.selectOne(
                Wrappers.lambdaQuery(WechatUser.class).eq(WechatUser::getWxOpenId, openId));
    }

    /**
     * 保存或更新OAuth2用户信息（无subscribe字段，保留原关注状态）
     */
    public boolean saveOrUpdateWxUser(WxOAuth2UserInfo userInfo) {
        try {
            WechatUser wechatUser = getWechatUserByOpenId(userInfo.getOpenid());
            if (wechatUser == null) {
                wechatUser = new WechatUser();
                wechatUser.setWxOpenId(userInfo.getOpenid());
            }
            wechatUser.setWxUnionId(userInfo.getUnionId());
            wechatUser.setWxNickname(userInfo.getNickname());
            wechatUser.setWxHeadImgUrl(userInfo.getHeadImgUrl());
            if (wechatUser.getId() == null) {
                wechatUserMapper.insert(wechatUser);
            } else {
                wechatUserMapper.updateById(wechatUser);
            }
            return true;
        } catch (Exception e) {
            log.error("微信OAuth2用户同步失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 信息刷新回调（全量刷新用）
     */
    public boolean refreshWxUser(WxMpUser wxMpUser) {
        return saveOrUpdateWxUser(wxMpUser);
    }
}
