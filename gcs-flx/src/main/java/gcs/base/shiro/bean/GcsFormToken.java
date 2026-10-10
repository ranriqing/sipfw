package gcs.base.shiro.bean;


import org.apache.shiro.authc.UsernamePasswordToken;

public class GcsFormToken extends UsernamePasswordToken {

    public GcsFormToken(String username, String password){
        super(username, password);
    }

}
