package gcs.modules.log.aspect;

import com.google.gson.Gson;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.GcsHttpUtils;
import gcs.modules.log.annotation.GLog;
import gcs.modules.log.domain.GcsLog;
import gcs.modules.log.mapper.GcsLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

@Aspect
@Component
public class GcsLogAspect {
	@Autowired
	private GcsLogMapper gcsLogMapper;

	@Autowired
	private GcsShiroService gcsShiroService;


	@Pointcut("@annotation(gcs.modules.log.annotation.GLog)")
	public void logPointCut() { 
		
	}

	@Around("logPointCut()")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		long beginTime = System.currentTimeMillis();
		Object result = joinPoint.proceed();
		long time = System.currentTimeMillis() - beginTime;
		//保存日志
		saveGcsLog(joinPoint, time);
		return result;
	}

	private void saveGcsLog(ProceedingJoinPoint joinPoint, long costTime) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		// 创建日志实体
		GcsLog gcsLog = new GcsLog();
		GLog gLog = method.getAnnotation(GLog.class);
		if(gLog != null){ // 方法注解上的描述
			gcsLog.setOperation(gLog.value());
		}
		// 请求的方法名
		String className = joinPoint.getTarget().getClass().getSimpleName();
		String methodName = signature.getName();
		gcsLog.setMethod(className + "." + methodName + "()");
		// 请求的参数
		Object[] args = joinPoint.getArgs();
		try{
			String params = new Gson().toJson(args);
			gcsLog.setParams(params);
		}catch (Exception e){
			// ignore
		}
		// 请求的Ip && 用户名
		HttpServletRequest request = GcsHttpUtils.getHttpServletRequest();
		gcsLog.setIp(GcsHttpUtils.getIpAddr(request));
		SpiUser spiUser = gcsShiroService.currentUser();
		gcsLog.setUserType(spiUser.getType());
		gcsLog.setUserId(spiUser.getId());
		gcsLog.setUsername(spiUser.getUsername());
		// 时间相关
		gcsLog.setCost(costTime);
		gcsLog.setDateCreated(new Date());
		// 保存日志
		gcsLogMapper.insert(gcsLog);
	}
}
