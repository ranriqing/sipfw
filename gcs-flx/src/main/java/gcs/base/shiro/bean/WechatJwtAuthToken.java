package gcs.base.shiro.bean;


import org.apache.shiro.authc.AuthenticationToken;

public class WechatJwtAuthToken implements AuthenticationToken {
    private String accessToken;

    public WechatJwtAuthToken(String accessToken){
        this.accessToken = accessToken;
    }

    @Override
    public String getPrincipal() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return accessToken;
    }
}
