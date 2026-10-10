package gcs.base.shiro.realm;

import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.WechatUser;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.core.GcsCoreService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 权限(鉴权)领域，负责加载用户/微信用户的权限信息。
 */
@Component
public class GcsPermissionRealm extends AuthorizingRealm {

    @Autowired
    protected GcsCoreService gcsCoreService;

    @Autowired
    protected GcsShiroService gcsShiroService;

    public GcsPermissionRealm() {
        super();
    }

    public GcsPermissionRealm(CredentialsMatcher matcher) {
        super(matcher);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return false; // 仅鉴权，不参与认证
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        return null; // 仅鉴权，不参与认证
    }

    /**
     * 鉴权(验证权限调用)
     *   调用链 isPermitted -> getAuthorizationInfo -> doGetAuthorizationInfo调用
     *   缓存支持 AuthorizingRealm extends CachingRealm 默认支持缓存
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String principal = (String) principals.getPrimaryPrincipal();
        Object reference = gcsCoreService.getReference(principal);
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        if (reference instanceof GcsUser) {
            authorizationInfo.setStringPermissions(gcsShiroService.getGcsUserPermissions(((GcsUser) reference).getId()));
        } else if (reference instanceof WechatUser) {
            authorizationInfo.setStringPermissions(gcsShiroService.getWechatUserPermissions(((WechatUser) reference).getId()));
        }
        return authorizationInfo;
    }

    /**
    * 覆盖正常调用逻辑
    */
    @Override
    public boolean isPermitted(PrincipalCollection principals, Permission permission) {
        /* 正常调用逻辑
        isPermitted(principals, permission)
          → getAuthorizationInfo(principals)
          → doGetAuthorizationInfo(principals)   // 子类重写的模板方法
        */
        String principal = (String) principals.getPrimaryPrincipal();
        Object reference = gcsCoreService.getReference(principal);
        if (reference instanceof GcsUser) {
            return true;
        } else if (reference instanceof WechatUser) {
            return super.isPermitted(principals, permission);
        }
        return false;
    }
}
