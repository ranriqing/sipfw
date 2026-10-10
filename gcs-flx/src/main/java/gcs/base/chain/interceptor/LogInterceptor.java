package gcs.base.chain.interceptor;

import gcs.base.tools.GcsClientInfoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求和响应日志拦截器，用于记录详细的请求和响应信息
 * 包括请求方法、URI、请求头、请求体、响应状态码和响应体
 * @see gcs.base.chain.filter.LogFilter
 */
public class LogInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LogInterceptor.class);
    private static final int MAX_PAYLOAD_LENGTH = 1024; // 限制日志中显示的最大长度

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        logRequestResponse(request, response);
    }

    private void logRequestResponse(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 处理请求
            ContentCachingRequestWrapper requestWrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
            ContentCachingResponseWrapper responseWrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);

            if (requestWrapper != null && responseWrapper != null) {
                // 构建请求信息
                StringBuilder requestInfo = new StringBuilder();
                requestInfo.append(request.getMethod()).append(" ");
                requestInfo.append(request.getRequestURI()).append(" ");

                String clientInfo = GcsClientInfoUtils.parseBrief(request.getHeader("User-Agent"));
                if (!clientInfo.isEmpty()) {
                    requestInfo.append("[").append(clientInfo).append("] ");
                }

                // requestInfo.append(getRequestHeaders(request));

                if (requestWrapper.getContentLength() > 0) {
                    /*requestInfo.append("REQ_BODY=").append(getTruncatedPayload(requestWrapper.getContentAsByteArray(),
                            requestWrapper.getCharacterEncoding()));*/
                }

                // 构建响应信息
                StringBuilder responseInfo = new StringBuilder();
                responseInfo.append("RSP_CODE=").append(responseWrapper.getStatus()).append(" ");
                /*responseInfo.append("RSP_BODY=").append(getTruncatedPayload(responseWrapper.getContentAsByteArray(),
                        responseWrapper.getCharacterEncoding()));*/

                // 合并请求和响应信息
                logger.info("{} {}", requestInfo, responseInfo);

                // 还原响应内容
                responseWrapper.copyBodyToResponse();
            }
        } catch (Exception e) {
            logger.warn("Error logging request/response", e);
        }
    }

    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    private String getTruncatedPayload(byte[] content, String characterEncoding) {
        if (content == null || content.length == 0) {
            return "";
        }

        String charset = characterEncoding != null ? characterEncoding : StandardCharsets.UTF_8.name();
        try {
            String payload = new String(content, charset);
            if (payload.length() > MAX_PAYLOAD_LENGTH) {
                return payload.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)";
            }
            return payload;
        } catch (Exception e) {
            return "[Error decoding payload: " + e.getMessage() + "]";
        }
    }
}