package gcs.base.shiro.realm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.WechatUser;
import gcs.app.ugrp.mapper.GcsUserMapper;
import gcs.app.ugrp.mapper.WechatUserMapper;
import gcs.base.core.GcsCoreService;
import gcs.base.shiro.bean.WechatOauth2Token;
import org.apache.shiro.authc.*;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WechatOauth2Realm extends AuthenticatingRealm {

    @Autowired
    private GcsUserMapper gcsUserMapper;

    @Autowired
    private WechatUserMapper wechatUserMapper;

    @Autowired
    private GcsCoreService gcsCoreService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof WechatOauth2Token;
    }

    /**
     * 认证(微信OAuth2登录)：优先尝试GcsUser，否则WechatUser
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String wxOpenId = (String) token.getPrincipal();

        // 优先绑定到 GcsUser
        GcsUser gcsUser = gcsUserMapper.selectOne(
            Wrappers.lambdaQuery(GcsUser.class).eq(GcsUser::getWxOpenId, wxOpenId)
        );
        if (gcsUser != null) {
            if (!gcsUser.getIsActive()) {
                throw new LockedAccountException("[WechatOauth2Realm]账号已禁用");
            }
            String principal = gcsCoreService.getReferenceIdentifier(gcsUser);
            return new SimpleAccount(principal, null, getName());
        }

        // 否则绑定到 WechatUser
        WechatUser wechatUser = wechatUserMapper.selectOne(
                Wrappers.lambdaQuery(WechatUser.class).eq(WechatUser::getWxOpenId, wxOpenId));
        if (wechatUser == null) {
            throw new UnknownAccountException("[WechatOauth2Realm]微信用户不存在");
        }
        String principal = gcsCoreService.getReferenceIdentifier(wechatUser);
        return new SimpleAccount(principal, null, getName());
    }

    @Override
    protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
        // 已在doGetAuthenticationInfo中校验，覆盖super方法不做凭证匹配
    }
}
