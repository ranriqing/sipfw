package gcs.base.chain.filter;

import gcs.base.YmlConfig;
import gcs.base.tools.GcsHttpUtils;
import gcs.base.tools.GcsShiroUtils;
import gcs.base.shiro.spi.SpiUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 记录请求相关内容
 * @see gcs.base.chain.interceptor.LogInterceptor
 */
public class LogFilter implements Filter {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final YmlConfig ymlConfig;

    public LogFilter(YmlConfig ymlConfig) {
        this.ymlConfig = ymlConfig;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String method = httpServletRequest.getMethod();
        String requestURI = httpServletRequest.getRequestURI();
        // 记录日志
        if(!"OPTIONS".equals(method)
            && !requestURI.contains("stomp")
            && !requestURI.contains("selectReadFlagCount")
        ){
            String remoteAddr = GcsHttpUtils.getIpAddr((HttpServletRequest)request);
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append(remoteAddr).append(" ");
            if(GcsShiroUtils.isLogin()){
                SpiUser spiUser = GcsShiroUtils.getCurrentUser();
                logBuilder.append("(").append(spiUser.getPrincipal()).append("|").append(spiUser.getUsername()).append(")").append(" ");
            }
            logBuilder.append(method).append(" ").append(requestURI);
            if(httpServletRequest.getQueryString() != null){
                logBuilder.append("?").append(httpServletRequest.getQueryString());
            }
            if(ymlConfig!=null && ymlConfig.getLogFilterLogPostParams()){
                if(ContentCachingFilter.isCachePayload(httpServletRequest)){
                    ContentCachingRequestWrapper requestWrapper = (ContentCachingRequestWrapper)httpServletRequest;
                    // logBuilder.append(" ").append(CachedBodyHttpServletRequest.payloadToString(httpServletRequest));
                    String requestContent = new String(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding());
                    logBuilder.append(" ").append(requestContent);
                }
            }
            logger.info(logBuilder.toString());
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}