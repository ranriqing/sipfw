package gcs.app.ugrp.beans;

import lombok.Data;

@Data
public class UgrpUser {

    /** 用户信息 */
    private Long id;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;

    /** 关联关系 */
    private Long roleId;
    private String roleCaption;
    private Long groupId;
    private String groupCaption;

}
