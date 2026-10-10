package gcs.base.core.exception;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.bean.ParamViolation;
import gcs.base.shiro.GcsShiroState;
import gcs.base.shiro.exception.AuthcRequiredException;
import gcs.base.shiro.exception.JwtParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;

import javax.servlet.ServletException;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	/** Gcs自定义异常 */
	@ExceptionHandler(GcsException.class)
	public GcsResult handleException(GcsException e){
		GcsResult gcsResult = GcsResult.failedResult("Gcs错误");
		if(e instanceof GcsParamException){
			GcsParamException paramException = (GcsParamException) e;
			List<ParamViolation> violations =  paramException.getViolations();
			gcsResult = GcsResult.paramInvalidResult().put("total", violations.size()).put("errors", violations);
		}else{
			if(!StringUtils.isBlank(e.getCode())){
				gcsResult.setCode(e.getCode());
			}
			gcsResult.setMessage(e.getMessage());
			stackTraceLogger(e).warn(e.getMessage(), e);
		}
		return gcsResult;
	}

	/** Shiro相关异常 */
	// 认证相关
	@ExceptionHandler(AuthenticationException.class)
	public static GcsResult handleAuthenticationException(AuthenticationException e){
		GcsResult gcsResult = GcsResult.stateResult(GcsShiroState.AUTHORIZATION_FAILED, "认证错误");
		if(e instanceof JwtParseException){
			gcsResult = GcsResult.stateResult(GcsShiroState.AUTHORIZATION_FAILED, "认证信息有误");
		}else if(e instanceof ExpiredCredentialsException){
			gcsResult = GcsResult.stateResult(GcsShiroState.AUTHORIZATION_TOKEN_EXPIRE, "认证已过期");
		}else if(e instanceof UnknownAccountException){
			gcsResult = GcsResult.stateResult(GcsShiroState.AUTHORIZATION_AUTHC_REQUIRED, "认证账号不存在");
		}else if(e instanceof LockedAccountException){
			gcsResult = GcsResult.stateResult(GcsShiroState.AUTHORIZATION_FAILED, "认证账号已禁用");
		}else if(e instanceof AuthcRequiredException){
			gcsResult = GcsResult.stateResult(GcsShiroState.AUTHORIZATION_AUTHC_REQUIRED, "需要重新认证");
		}
		stackTraceLogger(e).warn(e.getMessage(), e);
		return gcsResult;
	}
	// 授权相关
	@ExceptionHandler(AuthorizationException.class)
	public GcsResult handleAuthorizationException(AuthorizationException e){
		stackTraceLogger(e).warn(e.getMessage(), e);
		return GcsResult.failedResult("没有权限，请联系管理员授权");
	}

	/** Spring相关异常 */
	@ExceptionHandler(ServletException.class)
	public GcsResult handleServletException(ServletException e) {
		GcsResult gcsResult = GcsResult.failedResult("请求处理有误");
		if(e instanceof HttpRequestMethodNotSupportedException){
			HttpRequestMethodNotSupportedException exp = (HttpRequestMethodNotSupportedException) e;
			gcsResult.setMessage("不支持请求的方法" + exp.getMethod());
		}else if(e instanceof NoHandlerFoundException){
			NoHandlerFoundException exp = (NoHandlerFoundException) e;
			gcsResult.setMessage("路径不存在:" + exp.getRequestURL());
		}
		stackTraceLogger(e).error(e.getMessage(), e);
		return gcsResult;
	}

	/**
	 * SockJs相关异常
	 * DefaultSockJsService XhrStreamingTransportHandler
	 * 刷新页面：
	 * 客户端端开、服务器下刷失败、Tomcat转到/error
	 * BasicErrorController、HttpMessageNotWritableException|HttpMediaTypeNotAcceptableException
	 * GlobalExceptionHandler、HttpMessageNotWritableException|HttpMediaTypeNotAcceptableException
	 */
	@ExceptionHandler(HttpMessageNotWritableException.class)
	public void handleNestedRuntimeException(HttpMessageNotWritableException e){
		stackTraceLogger(e).warn("SockJs Broken /error " + e.getMessage());
	}

	/** MybatisPlus相关异常 */
	@ExceptionHandler(MybatisPlusException.class)
	public GcsResult handleMybatisPlusException(MybatisPlusException e){
		stackTraceLogger(e).error(e.getMessage(), e);
		return GcsResult.failedResult("数据库调用异常");
	}

	/** 捕获其它异常 */
	@ExceptionHandler(Exception.class)
	public GcsResult handleException(Exception e){
		stackTraceLogger(e).error(e.getMessage(), e);
		return GcsResult.failedResult("系统调用异常");
	}

	// 工具方法
	private static Logger stackTraceLogger(Exception e) {
		StackTraceElement[] stackTrace = e.getStackTrace();
		/*if (stackTrace.length > 0) {
			return LoggerFactory.getLogger(stackTrace[0].getClassName());
		}*/
		return LoggerFactory.getLogger(GlobalExceptionHandler.class.getName());
	}
}
