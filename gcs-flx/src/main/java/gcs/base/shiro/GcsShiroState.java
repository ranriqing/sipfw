package gcs.base.shiro;

import gcs.base.core.bean.GcsState;

/**
 * [调用状态]
 */
public class GcsShiroState {

    // 授权失败
    public static final GcsState AUTHORIZATION_FAILED = new GcsState("201", "授权失败");
    public static final GcsState AUTHORIZATION_FORBIDDEN = new GcsState("202", "没有权限");
    public static final GcsState AUTHORIZATION_TOKEN_EXPIRE = new GcsState("203", "TOKEN授权超时");
    public static final GcsState AUTHORIZATION_AUTHC_REQUIRED = new GcsState("204", "需要重新认证");

}
