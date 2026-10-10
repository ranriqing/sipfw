package gcs.modules.wechat;

import gcs.app.ugrp.service.WechatUserService;
import gcs.base.core.GcsHolder;
import gcs.modules.wechat.handler.LocationHandler;
import gcs.modules.wechat.handler.LogHandler;
import gcs.modules.wechat.handler.MenuHandler;
import gcs.modules.wechat.handler.MsgHandler;
import gcs.modules.wechat.handler.ScanHandler;
import gcs.modules.wechat.handler.SubscribeHandler;
import gcs.modules.wechat.handler.UnsubscribeHandler;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpUserService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.kefu.result.WxMpKfOnlineList;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.result.WxMpUserList;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GcsWechatService extends WxMpServiceImpl {

    @Autowired
    private WechatConfig wechatConfig;

    @Autowired
    private LogHandler logHandler;
    @Autowired
    private MenuHandler menuHandler;
    @Autowired
    private SubscribeHandler subscribeHandler;
    @Autowired
    private UnsubscribeHandler unsubscribeHandler;
    @Autowired
    private ScanHandler scanHandler;
    @Autowired
    private LocationHandler locationHandler;
    @Autowired
    private MsgHandler msgHandler;

    @Autowired
    private WechatUserService wechatUserService;

    private WxMpMessageRouter router;

    @PostConstruct
    public void init() {
        // 未配置 wechat.appid 时跳过初始化：微信相关功能（登录/消息）不可用，但应用照常启动。
        String appid = wechatConfig.getAppid();
        if (appid == null || appid.trim().isEmpty()) {
            log.warn("未配置 wechat.appid，跳过微信初始化（微信相关功能不可用）");
            return;
        }
        // 配置公众号
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(appid);
        config.setSecret(wechatConfig.getSecret());
        config.setToken(wechatConfig.getToken());
        config.setAesKey(wechatConfig.getAeskey());
        super.setWxMpConfigStorage(config);

        // 配置消息路由
        WxMpMessageRouter newRouter = new WxMpMessageRouter(this);
        this.router = newRouter;

        // 记录日志
        newRouter.rule().handler(logHandler).next();

        // 菜单点击 & 菜单查看
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT).event(WxConsts.EventType.CLICK).handler(menuHandler).end();
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT).event(WxConsts.EventType.VIEW).handler(menuHandler).end();

        // 关注事件 & 取消关注事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT).event(WxConsts.EventType.SUBSCRIBE).handler(subscribeHandler).end();
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT).event(WxConsts.EventType.UNSUBSCRIBE).handler(unsubscribeHandler).end();

        // 扫码事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT).event(WxConsts.EventType.SCAN).handler(scanHandler).end();

        // 上报地理位置事件 & 接收地理位置消息
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT).event(WxConsts.EventType.LOCATION).handler(locationHandler).end();
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.LOCATION).handler(locationHandler).end();

        // 默认文本消息
        newRouter.rule().async(false).handler(msgHandler).end();
    }

    /**
     * 获取 access_token：生产环境直接返还固定值
     */
    @Override
    public String getAccessToken(boolean forceRefresh) throws WxErrorException {
        if (GcsHolder.getYmlConfig().isDevelopment()) {
            return super.getAccessToken(forceRefresh);
        }else{
            // String wxTokenJsonStr = stringRedisTemplate.opsForValue().get("weixinToken");
            // String redisAccessToken = wxTokenJson['access_token'];
            return super.getAccessToken(forceRefresh);
        }
    }

    /**
     * 微信OAuth2：通过code获取accessToken
     */
    public WxOAuth2AccessToken oauth2getAccessToken(String code) throws WxErrorException {
        return super.getOAuth2Service().getAccessToken(code);
    }

    /**
     * 微信OAuth2：获取用户信息
     */
    public WxOAuth2UserInfo oauth2getUserInfo(WxOAuth2AccessToken accessToken, String lang) throws WxErrorException {
        return super.getOAuth2Service().getUserInfo(accessToken, lang);
    }

    /**
     * 发送模板消息
     */
    public String sendTemplateMsg(WxMpTemplateMessage templateMessage) {
        try {
            if (GcsHolder.getYmlConfig().isDevelopment()) {
                log.warn("开发模式：{} | {}", templateMessage.getToUser(), templateMessage.getUrl());
                return null;
            }
            return super.getTemplateMsgService().sendTemplateMsg(templateMessage);
        } catch (Exception e) {
            log.error("发送模板消息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 收到消息路由
     */
    public WxMpXmlOutMessage route(WxMpXmlMessage message) {
        try {
            return router.route(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查客服状态
     */
    public boolean hasKefuOnline() {
        try {
            WxMpKfOnlineList kfOnlineList = super.getKefuService().kfOnlineList();
            return kfOnlineList != null && kfOnlineList.getKfOnlineList() != null && kfOnlineList.getKfOnlineList().size() > 0;
        } catch (Exception e) {
            log.error("获取客服在线状态异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 刷新全部粉丝信息
     */
    public void refreshAll() {
        try {
            WxMpUserService wxMpUserService = super.getUserService();
            String nextOpenid = null;
            WxMpUserList wxMpUserIdsList = wxMpUserService.userList(nextOpenid);
            while (wxMpUserIdsList != null && wxMpUserIdsList.getOpenids() != null && !wxMpUserIdsList.getOpenids().isEmpty()) {
                List<String> openids = wxMpUserIdsList.getOpenids();
                List<String> stepOpenIds = new ArrayList<>();
                for (int i = 0; i < openids.size(); i++) {
                    stepOpenIds.add(openids.get(i));
                    // 一次最多获取100条，或处理最后一批
                    if (stepOpenIds.size() % 100 == 0 || i == openids.size() - 1) {
                        List<WxMpUser> wxMpUserList = wxMpUserService.userInfoList(stepOpenIds);
                        if (wxMpUserList != null) {
                            for (WxMpUser wxMpUser : wxMpUserList) {
                                wechatUserService.refreshWxUser(wxMpUser);
                            }
                        }
                        stepOpenIds.clear();
                    }
                }
                nextOpenid = wxMpUserIdsList.getNextOpenid();
                if (nextOpenid == null || nextOpenid.isEmpty()) {
                    break;
                }
                wxMpUserIdsList = wxMpUserService.userList(nextOpenid);
            }
        } catch (Exception e) {
            log.error("刷新粉丝信息失败: {}", e.getMessage(), e);
        }
    }
}
