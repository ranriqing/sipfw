package gcs.base.chain.filter;

import gcs.base.chain.filter.request.CachedBodyHttpServletRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 解决request.getInputStream只能读取一次问题
 */
@Deprecated
public class CachedBodyFilter extends OncePerRequestFilter {

    private static Boolean cacheEnabled = true;

    public static boolean isCachePayload(HttpServletRequest request) {
        String contentType = request.getContentType();
        // application/json || application/x-www-form-urlencoded
        return cacheEnabled && (contentType != null && contentType.contains("application/json"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(isCachePayload(request)){
            CachedBodyHttpServletRequest cachedBodyRequest = new CachedBodyHttpServletRequest(request);
            filterChain.doFilter(cachedBodyRequest, response);
        }else{
            filterChain.doFilter(request, response);
        }
    }
}
