package gcs.base.shiro.bean;


import org.apache.shiro.authc.AuthenticationToken;

/**
 * 微信 WechatOauth2Token
 */
public class WechatOauth2Token implements AuthenticationToken {
    private String wxOpenId;

    public WechatOauth2Token(String wxOpenId){
        this.wxOpenId = wxOpenId;
    }

    @Override
    public String getPrincipal() {
        return this.wxOpenId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }
}
