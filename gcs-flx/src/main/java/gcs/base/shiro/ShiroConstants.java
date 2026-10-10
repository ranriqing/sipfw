package gcs.base.shiro;

public class ShiroConstants {

    /** 超级管理员ID */
    public static final Long SUPER_ADMIN_ROLE_ID = -1L;

    /**
     * 请求Header认证字段
     * 请求Header刷新Token字段
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REFRESH_TOKEN = "RefreshToken";

    /** Jwt的相关参数 */
    public static final String JWT_ISSUER = "Gcs";
    public static final String JWT_EXTRA_TYPE = "token_type";          // JwtToken类型
    public static final String JWT_EXTRA_REFRESH = "refresh";          // Jwt刷新标记
    public static final String JWT_EXTRA_REXP_SECS = "rexp_secs";      // 刷新过期时间Key
    public static final String JWT_EXTRA_ORIGIN_USER = "origin_user";  // 超管切换用户自身用户名
    public static final String JWT_COOKIE_NAME = "uid";
    public static final String JWT_COOKIE_PATH = "/";

    /** Token相关信息 */
    public static Integer EXPIRE_SECONDS = 3600*2;                // 过期2小时
    public static Integer REFRESH_EXPIRE_SECONDS = 3600*24*7;     // 刷新时间7天
}
