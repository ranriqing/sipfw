package gcs.base.shiro.realm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.mapper.GcsUserMapper;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.core.GcsCoreService;
import gcs.base.shiro.bean.GcsJwtAuthToken;
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
public class GcsJwtAuthRealm extends AuthenticatingRealm {

    @Autowired
    private GcsUserMapper gcsUserMapper;

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    public GcsJwtAuthRealm(CredentialsMatcher credentialMatcher) {
        super(credentialMatcher);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof GcsJwtAuthToken;
    }

    /**
     * 认证(Jwt登录)
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = (String) token.getCredentials();
        // 校验Token有效性
        Jws<Claims> jwsClaims;
        try {
            jwsClaims = GcsShiroService.parseJwtClaims(accessToken);
        } catch (Exception ex) {
            if(ex instanceof ExpiredJwtException){
                throw new ExpiredCredentialsException("[GcsJwtAuthRealm]认证已过期");
            }else{
                throw new UnknownAccountException("[GcsJwtAuthRealm]JWT解析失败");
            }
        }
        Date issuedAtDate = jwsClaims.getBody().getIssuedAt();
        // 尝试解析用户
        String principal = jwsClaims.getBody().getSubject();
        Long userId = gcsCoreService.getReferenceId(principal);
        GcsUser gcsUser = gcsUserMapper.selectById(userId);
        if(gcsUser == null){
            // 兼容旧版本Gcs
            gcsUser = gcsUserMapper.selectOne(
                    Wrappers.lambdaQuery(GcsUser.class)
                            .apply("LOWER(username) = LOWER({0})", principal)
                            .or().eq(GcsUser::getMobile, principal)
            );
        }
        if (gcsUser == null) {
            throw new UnknownAccountException("[GcsJwtAuthRealm]用户不存在");
        }else if(!gcsUser.getIsActive()){
            throw new LockedAccountException("[GcsJwtAuthRealm]账号已禁用");
        }else if(gcsUser.getAuthcCheckDate()!=null && issuedAtDate.compareTo(gcsUser.getAuthcCheckDate())<0){
            throw new AuthcRequiredException("[GcsJwtAuthRealm]必须重新登录");
        }
        return new SimpleAccount(principal, gcsUser.getPasswordHash(), getName());
    }

    @Override
    protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
        // 已在doGetAuthenticationInfo中校验，覆盖super方法不做凭证匹配
    }
}
