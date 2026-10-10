package gcs.base.shiro.realm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.mapper.GcsUserMapper;
import gcs.base.core.GcsCoreService;
import gcs.base.shiro.bean.GcsFormToken;
import org.apache.shiro.authc.*;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GcsFormRealm extends AuthenticatingRealm {

    @Autowired
    private GcsUserMapper gcsUserMapper;

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    private HashedCredentialsMatcher credentialMatcher;

    @Autowired
    public GcsFormRealm(CredentialsMatcher credentialMatcher) {
        super(credentialMatcher);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof GcsFormToken;
    }

    /**
     * 认证(用户密码登录)
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        GcsFormToken authToken = (GcsFormToken)token;
        // 获取登录用户
        GcsUser gcsUser = gcsUserMapper.selectOne(
                Wrappers.lambdaQuery(GcsUser.class)
                        .apply("LOWER(username) = LOWER({0})", authToken.getUsername())
                        .or().eq(GcsUser::getMobile, authToken.getUsername())
        );
        if (gcsUser == null) {
            throw new UnknownAccountException("[GcsFormRealm]用户不存在");
        }else if(!gcsUser.getIsActive()){
            throw new LockedAccountException("[GcsFormRealm]账号已禁用");
        }
        // 校验登录用户
        String principal = gcsCoreService.getReferenceIdentifier(gcsUser);
        // 未启用Salt参数： ByteSource.Util.bytes(gcsUser.getSalt())
        AuthenticationInfo account = new SimpleAccount(principal, gcsUser.getPasswordHash(), getName());
        if(!credentialMatcher.doCredentialsMatch(token, account)){
            throw new IncorrectCredentialsException("[GcsFormRealm]密码有误");
        }
        return account;
    }
    
    @Override
    protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
        // 已在doGetAuthenticationInfo中校验，覆盖super方法不做凭证匹配
    }

}
