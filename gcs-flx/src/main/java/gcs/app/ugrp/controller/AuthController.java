package gcs.app.ugrp.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.mapper.GcsUserMapper;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.app.ugrp.service.WechatUserService;
import gcs.base.core.GcsCoreService;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.GcsResult;
import gcs.app.ugrp.beans.GcsLoginForm;
import gcs.app.ugrp.service.GcsCaptchaService;
import gcs.base.core.bean.GcsState;
import gcs.base.shiro.GcsShiroState;
import gcs.base.shiro.ShiroConstants;
import gcs.base.shiro.bean.GcsFormToken;
import gcs.base.shiro.bean.WechatOauth2Token;
import gcs.base.shiro.exception.AuthcRequiredException;
import gcs.base.shiro.utils.CookieHelper;
import gcs.base.shiro.spi.SpiUser;
import gcs.modules.wechat.GcsWechatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	private GcsUserMapper gcsUserMapper;

	@Autowired
	private GcsCoreService gcsCoreService;

	@Autowired
	private GcsShiroService gcsShiroService;

	@Autowired
	private GcsCaptchaService gcsCaptchaService;

	@Autowired
	private GcsWechatService gcsWechatService;

	@Autowired
	private WechatUserService wechatUserService;

	@GetMapping("/captcha.jpg")
	public void captcha(HttpServletResponse response, @RequestParam String uuid) throws IOException {
		response.setHeader("Cache-Control", "no-store, no-cache");
		response.setContentType("image/jpeg");
		BufferedImage image = gcsCaptchaService.genCaptcha(uuid);
		ServletOutputStream out = response.getOutputStream();
		ImageIO.write(image, "jpg", out);
		IOUtils.closeQuietly(out);
	}

	@PostMapping("/login")
	public GcsResult login(HttpServletResponse response, @RequestBody GcsLoginForm form){
		boolean captcha = gcsCaptchaService.validate(form.getUuid(), form.getCaptcha());
		if(!captcha){
			return GcsResult.failedResult("验证码不正确");
		}
		GcsFormToken gcsFormToken = new GcsFormToken(form.getIdentifier(), form.getPassword());
		gcsShiroService.executeLogin(gcsFormToken);
		SpiUser spiUser = gcsShiroService.currentUser();
		Map<String, Object> extraClaims = new HashMap<>();
		String accessTokenStr = gcsShiroService.generateJwtToken(spiUser.getPrincipal(), spiUser.getType(), false, extraClaims);
		String refreshTokenStr = gcsShiroService.generateJwtToken(spiUser.getPrincipal(), spiUser.getType(), true, extraClaims);
		return generateJwtTokenResult(response, accessTokenStr, refreshTokenStr, spiUser.getType());
	}

	@PostMapping("/clogin")
	public GcsResult clogin(HttpServletResponse response, @RequestBody Map<String, Object> jsonMap){
		String code = (String) jsonMap.get("code");
		if (StringUtils.isBlank(code)) {
			return GcsResult.stateResult(GcsState.PARAM_INVALID, "缺少code参数");
		}
		// 开发模式: dev-code-* 直接以指定用户登录(跳过微信OAuth)
		if (GcsHolder.getYmlConfig().isDevelopment() && code.startsWith("dev-code-")) {
			Map<String, String> devCodeMap = new HashMap<>();
			devCodeMap.put("dev-code-admin", "admin");
			String username = devCodeMap.getOrDefault(code, "admin");
			GcsUser devUser = gcsUserMapper.selectOne(Wrappers.lambdaQuery(GcsUser.class).eq(GcsUser::getUsername, username));
			if (devUser == null) {
				return GcsResult.failedResult("开发用户不存在: " + username);
			}
			String genPrincipal = gcsCoreService.getReferenceIdentifier(devUser);
			Map<String, Object> extraClaims = new HashMap<>();
			String accessTokenStr = gcsShiroService.generateJwtToken(genPrincipal, SpiUser.TYPE_GCS, false, extraClaims);
			String refreshTokenStr = gcsShiroService.generateJwtToken(genPrincipal, SpiUser.TYPE_GCS, true, extraClaims);
			return generateJwtTokenResult(response, accessTokenStr, refreshTokenStr, SpiUser.TYPE_GCS);
		}
		// 微信OAuth2换取用户信息
		WxOAuth2UserInfo wxUserInfo = null;
		try {
			WxOAuth2AccessToken accessToken = gcsWechatService.oauth2getAccessToken(code);
			wxUserInfo = gcsWechatService.oauth2getUserInfo(accessToken, null);
			wechatUserService.saveOrUpdateWxUser(wxUserInfo);
		} catch (Exception e) {
			e.printStackTrace();
			return GcsResult.stateResult(GcsShiroState.AUTHORIZATION_FAILED, e.getMessage());
		}
		WechatOauth2Token wechatOauth2Token = new WechatOauth2Token(wxUserInfo.getOpenid());
		gcsShiroService.executeLogin(wechatOauth2Token);
		// 生成Token
		SpiUser spiUser = gcsShiroService.currentUser();
		Map<String, Object> extraClaims = new HashMap<>();
		String accessTokenStr = gcsShiroService.generateJwtToken(spiUser.getPrincipal(), spiUser.getType(), false, extraClaims);
		String refreshTokenStr = gcsShiroService.generateJwtToken(spiUser.getPrincipal(), spiUser.getType(), true, extraClaims);
		return generateJwtTokenResult(response, accessTokenStr, refreshTokenStr, spiUser.getType());
	}

	/**
	 * 刷新Token
	 */
	@RequestMapping(value = "/refreshToken", method = {RequestMethod.GET, RequestMethod.POST})
	public GcsResult refreshToken(HttpServletRequest request, HttpServletResponse response) {
		// 读取Token
		String oldRefreshTokenStr = request.getHeader(ShiroConstants.REFRESH_TOKEN);
		if (StringUtils.isBlank(oldRefreshTokenStr)) {
			oldRefreshTokenStr = request.getParameter(ShiroConstants.REFRESH_TOKEN);
		}

		// 参数校验
		GcsResult gcsResult;
		if (oldRefreshTokenStr == null || oldRefreshTokenStr.isEmpty()) {
			gcsResult = GcsResult.stateResult(GcsState.PARAM_INVALID, "请求Header中缺少" + ShiroConstants.REFRESH_TOKEN + "参数");
			return gcsResult;
		}

		// 刷新Token
		try {
			Jws<Claims> refreshTokenClaims = GcsShiroService.parseJwtClaims(oldRefreshTokenStr);
			String subject = refreshTokenClaims.getBody().getSubject();
			String audience = refreshTokenClaims.getBody().getAudience();
			String tokenType = (String) refreshTokenClaims.getBody().get(ShiroConstants.JWT_EXTRA_TYPE);
			String originUser = (String) refreshTokenClaims.getBody().get(ShiroConstants.JWT_EXTRA_ORIGIN_USER);
			if (!"refresh".equals(audience)) {
				throw new RuntimeException("AccessToken不能用来刷新");
			}
			// 重新登录判断
			SpiUser spiUser = gcsShiroService.principalUser(subject);
			Date issuedAtDate = refreshTokenClaims.getBody().getIssuedAt();
			Date authcCheckDate = spiUser!=null?spiUser.getAuthcCheckDate():null;
			if (authcCheckDate != null && issuedAtDate.before(authcCheckDate)) {
				throw new AuthcRequiredException("必须重新登录");
			}

			// 生成Token
			Map<String, Object> extraClaims = new HashMap<>();
			if (originUser != null) {
				extraClaims.put(ShiroConstants.JWT_EXTRA_ORIGIN_USER, originUser);
			}
			String accessTokenStr = gcsShiroService.generateJwtToken(subject, tokenType, false, extraClaims);
			String refreshTokenStr = gcsShiroService.generateJwtToken(subject, tokenType, true, extraClaims);
			return generateJwtTokenResult(response, accessTokenStr, refreshTokenStr, tokenType);
		} catch (Exception ex) {
			ex.printStackTrace();
			gcsResult = GcsResult.stateResult(GcsShiroState.AUTHORIZATION_FAILED, "无效的" + ShiroConstants.REFRESH_TOKEN);
		}
		return gcsResult;
	}

	/**
	 * 登出操作
	 * 客户端丢掉accessTokenStr && 服务端来删除Cookie
	 * @return
	 */
	@RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
	public GcsResult logout(HttpServletRequest request, HttpServletResponse response) {
		// 清理Cookie
		gcsShiroService.executeLogout(request, response);
		// 登出结果
		return GcsResult.successResult();
	}

	@PostMapping("/switchToken")
	public GcsResult switchToken(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> jsonMap){
		SpiUser spiUser = gcsShiroService.currentUser();
		String toUsername = (String)jsonMap.get("toUsername");
		Boolean switchTo = !StringUtils.isBlank(toUsername);
		GcsUser genTokenUser;
		if(!spiUser.isGcsUser()){
			return GcsResult.failedResult("切换失败");
		}else if(switchTo){
			// 切换到指定用户
			if(!spiUser.isSuperAdmin()){
				return GcsResult.failedResult("无权[切换到]用户" + toUsername);
			}
			GcsUser toGcsUser = gcsUserMapper.selectOne(Wrappers.lambdaQuery(GcsUser.class).eq(GcsUser::getUsername, toUsername));
			if(toGcsUser == null){
				return GcsResult.failedResult("无法[切换到]用户" + toUsername);
			}else if(toGcsUser.isSuperAdmin()){
				return GcsResult.failedResult("不允许[切换到]超管用户");
			}
			genTokenUser = toGcsUser;
		}else{
			// 切换回管理用户
			Jws<Claims> jwsClaims = gcsShiroService.parseJwtClaims(request);
			String originUsername = (String)jwsClaims.getBody().get(ShiroConstants.JWT_EXTRA_ORIGIN_USER);
			if(originUsername == null) {
				throw new RuntimeException("无权[切换回]原用户");
			}
			GcsUser originGcsUser = gcsUserMapper.selectOne(Wrappers.lambdaQuery(GcsUser.class).eq(GcsUser::getUsername, originUsername));
			if(originGcsUser == null){
				throw new RuntimeException("无法[切换回]原用户" + originUsername);
			}
			genTokenUser = originGcsUser;
		}
		// 生成Token
		String genPrincipal = gcsCoreService.getReferenceIdentifier(genTokenUser);
		Map<String, Object> extraClaims = new HashMap<>();
		if(switchTo){ extraClaims.put(ShiroConstants.JWT_EXTRA_ORIGIN_USER, spiUser.getGcsUser().getUsername()); }      // 切换到的用户，前端区分切换动作
		String accessTokenStr = gcsShiroService.generateJwtToken(genPrincipal, spiUser.getType(), false, extraClaims);
		String refreshTokenStr = gcsShiroService.generateJwtToken(genPrincipal, spiUser.getType(), true, extraClaims);
		return generateJwtTokenResult(response, accessTokenStr, refreshTokenStr, spiUser.getType())
				.putAll(extraClaims);
	}

	/** 工具方法 */
	private GcsResult generateJwtTokenResult(HttpServletResponse response, String accessTokenStr, String refreshTokenStr, String tokenType){
		// 写出Cookie
		Cookie cookie = new Cookie(ShiroConstants.JWT_COOKIE_NAME, accessTokenStr);
		cookie.setPath(ShiroConstants.JWT_COOKIE_PATH);
		cookie.setSecure(false);
		cookie.setMaxAge(ShiroConstants.EXPIRE_SECONDS);      // 单位Seconds
		CookieHelper.addHttpOnlyCookie(response, cookie);
		// 返回GcsResult
		return GcsResult.successResult()
				.put("access_token", accessTokenStr)
				.put("expires_in", ShiroConstants.EXPIRE_SECONDS)
				.put("refresh_token", refreshTokenStr)
				.put("token_type", tokenType); // token_type 仅用来给前端识别类型
	}
}
