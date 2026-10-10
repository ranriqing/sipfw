package gcs.base.shiro.utils;

import gcs.base.shiro.ShiroConstants;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Cookie操作工具类
 */
public class CookieHelper {

    /**
     * 解决 servlet 3.0 以下版本不支持 HttpOnly
     * @param response HttpServletResponse类型的响应
     * @param cookie   要设置httpOnly的cookie对象
     */
    public static void addHttpOnlyCookie(HttpServletResponse response, Cookie cookie) {
        if (cookie == null) {
            return;
        }
        /**
         * 依次取得cookie中的名称、值、 最大生存时间、路径、域和是否为安全协议信息
         */
        String cookieName = cookie.getName();
        String cookieValue = cookie.getValue();
        int maxAge = cookie.getMaxAge();
        String path = cookie.getPath();
        String domain = cookie.getDomain();
        boolean isSecure = cookie.getSecure();

        StringBuilder sf = new StringBuilder();
        sf.append(cookieName).append("=").append(cookieValue).append(";");

        if (maxAge >= 0) {
            sf.append("Max-Age=").append(cookie.getMaxAge()).append(";");
        }
        if (domain != null) {
            sf.append("domain=").append(domain).append(";");
        }
        if (path != null) {
            sf.append("path=").append(path).append(";");
        }
        if (isSecure) {
            sf.append("secure;HTTPOnly;");
        } else {
            sf.append("HTTPOnly;");
        }

        response.addHeader("Set-Cookie", sf.toString());
    }

    /**
     * <p>
     * 根据cookieName清除指定Cookie
     * </p>
     *
     * @param request
     * @param response
     * @param cookieName cookie名称
     * @return Boolean
     */
    public static Boolean clearCookieByName(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        Boolean result = false;
        Cookie cookie = findCookieByName(request, cookieName);

        if (cookie != null) {
            try {
                Cookie clearCookie = new Cookie(cookieName, null);
                clearCookie.setMaxAge(0);  // 立即删除
                clearCookie.setPath(ShiroConstants.JWT_COOKIE_PATH);
                response.addCookie(clearCookie);
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
                // Ignore
                result = false;
            }
        }
        return result;
    }

    /**
     * 根据cookieName获取Cookie
     * @param request
     * @param cookieName Cookie名称
     * @return Cookie
     */
    public static Cookie findCookieByName(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                return cookie;
            }
        }
        return null;
    }
}