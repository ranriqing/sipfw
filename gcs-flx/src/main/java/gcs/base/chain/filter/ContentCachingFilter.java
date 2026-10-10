package gcs.base.chain.filter;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ContentCachingFilter extends OncePerRequestFilter {

    private static Boolean cacheEnabled = true;

    public static boolean isCachePayload(HttpServletRequest request) {
        String contentType = request.getContentType();
        // application/json || application/x-www-form-urlencoded
        return cacheEnabled && (contentType != null && contentType.contains("application/json"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if(isCachePayload(request)){
            ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(requestWrapper, responseWrapper);
            responseWrapper.copyBodyToResponse();
        }else{
            filterChain.doFilter(request, response);
        }
    }
}