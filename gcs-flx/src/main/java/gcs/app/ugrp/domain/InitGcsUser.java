package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@TableName("gcs_user")
public class InitGcsUser extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	/** 基本信息 */
	private String username;
	private String passwordHash;
	private String mobile;               // 手 机
	private String nickname;             // 昵 称
	private String avatar;               // 头 像
	private String salt;

	/** 其它信息 */
	private Boolean isActive;        // 是否启用  1:启用  0:禁用
	private Date authcCheckDate;     // 此日期前Token必须重新登陆(修改密码)

	/** 关联关系 */
	@Transient
	private Long gcsRoleId;             // 角色Id

	@Transient
	private Long gcsGroupId;             // 部门Id
}
