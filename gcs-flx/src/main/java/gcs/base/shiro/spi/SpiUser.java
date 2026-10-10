package gcs.base.shiro.spi;

import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.WechatUser;
import lombok.Data;

import java.util.Date;

@Data
public class SpiUser {

    public static final String TYPE_GCS =  "gcs";            // Gcs用户
    public static final String TYPE_WECHAT =  "wechat";      // Wechat用户

    /** 认证标识 */
    private String type;
    private String principal;

    /** 原始用户 */
    private GcsUser gcsUser;
    private WechatUser wechatUser;

    /** 用户信息 */
    private Long id;
    private String username;
    private String mobile;
    private String email;
    private String avatar;
    private String displayName;
    private Long roleId;
    private Long groupId;
    private Date authcCheckDate;

    /** 工具方法 */
    public Boolean isSuperAdmin() {
        return this.isGcsUser() && gcsUser.isSuperAdmin();
    }

    public Object getRealUser(){
        if(this.isGcsUser()){
            return this.gcsUser;
        }else if(this.isWechatUser()){
            return this.wechatUser;
        }else{
            return null;
        }
    }

    public Boolean isGcsUser(){
        return TYPE_GCS.equals(this.type);
    }

    public Boolean isWechatUser(){
        return TYPE_WECHAT.equals(this.type);
    }

}
