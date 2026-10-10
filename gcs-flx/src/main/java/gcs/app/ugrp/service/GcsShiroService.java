package gcs.app.ugrp.service;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.WechatUser;
import gcs.app.ugrp.mapper.GcsUserMapper;
import gcs.base.core.GcsCoreService;
import gcs.base.shiro.ShiroConstants;
import gcs.base.shiro.utils.CookieHelper;
import gcs.base.shiro.spi.SpiUser;
import gcs.modules.wechat.WechatConfig;
import io.jsonwebtoken.*;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Service
public class GcsShiroService {

    // 缓存条目
    private static SecretKey _secertKey;

    @Lazy
    @Autowired
    private GcsUserMapper gcsUserMapper;

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    private WechatConfig wechatConfig;

    /**
     * 执行登录
     * @param authToken
     */
    public void executeLogin(AuthenticationToken authToken) {
        SecurityUtils.getSubject().login(authToken);
    }

    /**
     * 当前用户
     * @return
     */
    public SpiUser currentUser(){
        String principal = (String)SecurityUtils.getSubject().getPrincipal();
        return this.principalUser(principal);
    }

    /**
     * 通过principal读取抽象用户
     * @param principal
     * @return
     */
    public SpiUser principalUser(String principal){
        Object principalUser = gcsCoreService.getReference(principal);
        // 兼容旧版本Gcs
        if(principalUser == null){
            principalUser = gcsUserMapper.selectOne(
                    Wrappers.lambdaQuery(GcsUser.class)
                            .apply("LOWER(username) = LOWER({0})", principal)
                            .or().eq(GcsUser::getMobile, principal)
            );
        }
        SpiUser spiUser = new SpiUser();
        spiUser.setPrincipal(principal);
        if(principalUser instanceof GcsUser){
            GcsUser realUser = (GcsUser)principalUser;
            // 认证标识
            spiUser.setType(SpiUser.TYPE_GCS);
            spiUser.setPrincipal(principal);
            // 原始用户
            spiUser.setId(realUser.getId());
            spiUser.setGcsUser(realUser);
            // 用户信息
            spiUser.setGroupId(realUser.getGcsGroupId());
            spiUser.setRoleId(realUser.getGcsRoleId());
            spiUser.setUsername(realUser.getUsername());
            spiUser.setMobile(realUser.getMobile());
            spiUser.setEmail(realUser.getEmail());
            spiUser.setAvatar(realUser.getAvatar());
            spiUser.setDisplayName(realUser.getNickname());
            spiUser.setAuthcCheckDate(realUser.getAuthcCheckDate());
            return spiUser;
        }else if(principalUser instanceof WechatUser){
            WechatUser realUser = (WechatUser)principalUser;
            // 认证标识
            spiUser.setType(SpiUser.TYPE_WECHAT);
            spiUser.setPrincipal(principal);
            // 原始用户
            spiUser.setId(realUser.getId());
            spiUser.setWechatUser(realUser);
            // 用户信息
            spiUser.setGroupId(null);
            spiUser.setRoleId(null);
            spiUser.setUsername(realUser.getWxOpenId());
            spiUser.setMobile(null);
            spiUser.setEmail(null);
            spiUser.setDisplayName(realUser.getWxNickname());
            spiUser.setAuthcCheckDate(realUser.getAuthcCheckDate());
            return spiUser;
        }
        throw new RuntimeException("Not supported principal:" + principal);
    }

    /**
     * 获取GcsUser权限
     * @param gcsUserId
     * @return
     */
    public Set<String> getGcsUserPermissions(long gcsUserId) {
        // 获取用户权限
        List<String> permissionList;
        if(gcsUserId == ShiroConstants.SUPER_ADMIN_ROLE_ID){
            permissionList = gcsUserMapper.listPermissions(null);
        }else{
            permissionList = gcsUserMapper.listPermissions(gcsUserId);
        }
        // 整理用户权限
        Set<String> permissionSet = new HashSet<>();
        for(String permission : permissionList){
            if(StringUtils.isBlank(permission)){
                continue;
            }
            permissionSet.addAll(Arrays.asList(permission.trim().split(",")));
        }
        return permissionSet;
    }

    /**
     * 获取WechatUser权限
     * @param wechatUserId
     * @return
     */
    public Set<String> getWechatUserPermissions(long wechatUserId) {
        // 配置固定授权，仅允许访问配置中声明的权限标识
        return new HashSet<>(wechatConfig.getPermissions());
    }

    /**
     * 生成JwtToken
     * @param subject
     * @param isRefreshToken
     * @param extraClaims
     * @return
     */
    public String generateJwtToken(String subject, String tokenType, Boolean isRefreshToken, Map<String, Object> extraClaims){
        Assert.notNull(subject, "Subject不能为空");
        if(extraClaims == null){ extraClaims = new HashMap<>(); }
        // 当前时间 & 过期时间
        Date nowDate = new Date();
        Integer expireSeconds = isRefreshToken?ShiroConstants.REFRESH_EXPIRE_SECONDS:ShiroConstants.EXPIRE_SECONDS;
        Date expireDate = DateUtil.date().offset(DateField.SECOND, expireSeconds);
        // 执行生成 JwtToken
        JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.setIssuer(ShiroConstants.JWT_ISSUER).setSubject(subject)
                .setIssuedAt(nowDate).setExpiration(expireDate);                 // 签发时间 & 过期时间
        if(isRefreshToken){
            jwtBuilder.setAudience(ShiroConstants.JWT_EXTRA_REFRESH);            // 刷新识别标记
            extraClaims.put(ShiroConstants.JWT_EXTRA_REXP_SECS, expireSeconds);  // 刷新过期时长
        }
        extraClaims.put(ShiroConstants.JWT_EXTRA_TYPE, tokenType);
        extraClaims.forEach(jwtBuilder::claim);
        return jwtBuilder.signWith(SignatureAlgorithm.HS512, getSecretKey()).compact();
    }

    /**
     * 通过请求解析Jws<Claims>
     * @return
     */
    public Jws<Claims> parseJwtClaims(HttpServletRequest request) {
        // 读取accessToken
        String accessToken = request.getHeader(ShiroConstants.AUTHORIZATION_HEADER);
        if(StringUtils.isBlank(accessToken)){
            Cookie uid = CookieHelper.findCookieByName(request, ShiroConstants.JWT_COOKIE_NAME);
            accessToken = uid!=null?uid.getValue():null;
        }
        return parseJwtClaims(accessToken);
    }

    public static Jws<Claims> parseJwtClaims(String accessToken) {
        // 解析Token信息
        return Jwts.parser().setSigningKey(getSecretKey()).parseClaimsJws(accessToken);
    }

    /**
     * 执行登出操作, 清除Cookie
     * @param request
     * @param response
     * @return
     */
    public Boolean executeLogout(HttpServletRequest request, HttpServletResponse response) {
        return CookieHelper.clearCookieByName(request, response, ShiroConstants.JWT_COOKIE_NAME);
    }

    /** ----- 工具方法 ----- */
    private static SecretKey getSecretKey(){
        if(_secertKey == null){
            // JWT 签名密钥：优先取系统属性 -Dgcs.jwt.secret，其次环境变量 GCS_JWT_SECRET。
            // 两者都未配置时使用占位符，生产环境必须显式配置。
            String secret = System.getProperty("gcs.jwt.secret");
            if(StringUtils.isBlank(secret)){
                secret = System.getenv("GCS_JWT_SECRET");
            }
            if(StringUtils.isBlank(secret)){
                secret = "change-me";
            }
            _secertKey = new SecretKeySpec(secret.getBytes(), "AES");
        }
        return _secertKey;
    }
}
