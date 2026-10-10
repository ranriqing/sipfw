package gcs.base.shiro.exception;

import org.apache.shiro.authc.AuthenticationException;

/**
 * Jwt获取或解析失败
 */
public class JwtParseException extends AuthenticationException{

    public JwtParseException(String message){
        super(message);
    }

}
