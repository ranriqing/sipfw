package gcs.app.ugrp.beans;

import lombok.Data;

@Data
public class GcsLoginForm {

    /** 登录信息 */
    private String identifier;
    private String password;

    /** 防止爆破 */
    private String uuid;
    private String captcha;

}
