package gcs.base.shiro.realm;

import gcs.app.ugrp.domain.WechatUser;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.core.GcsCoreService;
import gcs.base.shiro.bean.WechatJwtAuthToken;
import gcs.base.shiro.exception.AuthcRequiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import org.apache.shiro.authc.*;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class WechatJwtAuthRealm extends AuthenticatingRealm {

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    public WechatJwtAuthRealm(CredentialsMatcher credentialMatcher) {
        super(credentialMatcher);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof WechatJwtAuthToken;
    }

    /**
     * 认证(微信Jwt登录)
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = (String) token.getCredentials();
        // 校验Token有效性
        Jws<Claims> jwsClaims;
        try {
            jwsClaims = GcsShiroService.parseJwtClaims(accessToken);
        } catch (Exception ex) {
            if (ex instanceof ExpiredJwtException) {
                throw new ExpiredCredentialsException("[WechatJwtAuthRealm]认证已过期");
            } else {
                throw new UnknownAccountException("[WechatJwtAuthRealm]JWT解析失败");
            }
        }
        Date issuedAtDate = jwsClaims.getBody().getIssuedAt();
        // 尝试解析用户
        String principal = jwsClaims.getBody().getSubject();
        Object reference = gcsCoreService.getReference(principal);
        if (reference instanceof WechatUser) {
            WechatUser wechatUser = (WechatUser) reference;
            if (wechatUser.getAuthcCheckDate() != null && issuedAtDate.compareTo(wechatUser.getAuthcCheckDate()) < 0) {
                throw new AuthcRequiredException("[WechatJwtAuthRealm]必须重新登录");
            }
        } else {
            throw new UnknownAccountException("[WechatJwtAuthRealm]用户不存在");
        }
        return new SimpleAccount(principal, accessToken, getName());
    }

    @Override
    protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
        // 已在doGetAuthenticationInfo中校验，覆盖super方法不做凭证匹配
    }
}
