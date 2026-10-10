package gcs.base.shiro.exception;

import org.apache.shiro.authc.AuthenticationException;

/**
 * 需要重新认证
 */
public class AuthcRequiredException extends AuthenticationException{

    public AuthcRequiredException(String message){
        super(message);
    }

}
