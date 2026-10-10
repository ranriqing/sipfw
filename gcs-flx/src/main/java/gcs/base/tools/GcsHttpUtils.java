package gcs.base.tools;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

public class GcsHttpUtils {

	public static HttpServletRequest getHttpServletRequest() {
		return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	}

	public static String getDomain(){
		HttpServletRequest request = getHttpServletRequest();
		StringBuffer requestURL = request.getRequestURL();
		return requestURL.delete(requestURL.length() - request.getRequestURI().length(), requestURL.length()).toString();
	}

	public static String getOrigin(){
		HttpServletRequest request = getHttpServletRequest();
		return request.getHeader("Origin");
	}

	/**
	 * 获取IP地址
	 * 如果使用了反向代理，尝试从不同请求头获取真实IP地址
	 */
	public static String getIpAddr(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		if("127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr)){
			String remoteIp;
			List<String> headerList = Arrays.asList("X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR");
			for(String header: headerList){
				remoteIp = request.getHeader(header);
				if(!StringUtils.isBlank(remoteIp) && !"unknown".equalsIgnoreCase(remoteIp)){
					remoteAddr = remoteIp;
					break;
				}
			}
		}
		return remoteAddr;
	}
}
