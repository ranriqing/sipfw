package gcs.base.shiro;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.exception.GlobalExceptionHandler;
import gcs.base.shiro.bean.GcsJwtAuthToken;
import gcs.base.shiro.bean.WechatJwtAuthToken;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.GcsHttpUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OAuth2Filter extends AuthenticatingFilter {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        return isAccessAllowed(request, response, mappedValue) || onAccessDenied(request, response, mappedValue);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        return ((HttpServletRequest) request).getMethod().equals(RequestMethod.OPTIONS.name());
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        // 获取请求token，如果token不存在直接返回401
        String token = getRequestToken((HttpServletRequest) request);
        if(StringUtils.isBlank(token)){
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            logFailureRequest((HttpServletRequest)request, httpResponse);
            // 返回Json结果
            String jsonStr = new Gson().toJson(GcsResult.stateResult(GcsShiroState.AUTHORIZATION_FAILED, "需要包含认证标识"));
            httpResponse.getWriter().print(jsonStr);
            return false;
        }
        return super.executeLogin(request, response);
    }

    // 在 super.executeLogin 中调用
    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        // 获取请求token
        String token = this.getRequestToken((HttpServletRequest) request);
        if(StringUtils.isBlank(token)){
            return null;
        }
        // 根据token类型路由到对应Realm
        String tokenType = null;
        try {
            Jws<Claims> jwsClaims = GcsShiroService.parseJwtClaims(token);
            tokenType = (String) jwsClaims.getBody().get(ShiroConstants.JWT_EXTRA_TYPE);
        } catch (Exception e) {
            // 传递给GcsJwtAuthRealm来处理失败
            tokenType = SpiUser.TYPE_GCS;
        }
        if(SpiUser.TYPE_WECHAT.equals(tokenType)){
            return new WechatJwtAuthToken(token);
        }
        return new GcsJwtAuthToken(token);
    }

    // 在 super.executeLogin 中调用
    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        logFailureRequest((HttpServletRequest)request, httpResponse);
        // 返回Json结果
        try {
            GcsResult gcsResult = GlobalExceptionHandler.handleAuthenticationException(e);
            httpResponse.getWriter().print(gcsResult.toJson());
        } catch (IOException e1) {
            // ignore
        }
        return false;
    }

    // 获取请求的token
    private String getRequestToken(HttpServletRequest httpRequest){
        // Header中获取token
        String jwtToken = httpRequest.getHeader(ShiroConstants.AUTHORIZATION_HEADER);
        // 如果Header中不存在token，则从参数中获取token
        if(StringUtils.isBlank(jwtToken)){
            jwtToken = httpRequest.getParameter(ShiroConstants.AUTHORIZATION_HEADER);
        }
        return jwtToken;
    }

    private void logFailureRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse){
        // 设定返回头
        httpResponse.setContentType(ContentType.APPLICATION_JSON.toString());
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Allow-Origin", GcsHttpUtils.getOrigin());
        // 记录调用日志
        String method = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();
        String remoteAddr = GcsHttpUtils.getIpAddr(httpRequest);
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append(remoteAddr).append(" ");
        logBuilder.append(method).append(" ").append(requestURI);
        if(httpRequest.getQueryString() != null){
            logBuilder.append("?").append(httpRequest.getQueryString());
        }
        logger.warn(logBuilder.toString());
    }
}
