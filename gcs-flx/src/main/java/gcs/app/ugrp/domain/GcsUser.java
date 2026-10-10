package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gcs.base.core.validators.group.InsertGroup;
import gcs.base.core.validators.group.UpdateGroup;
import gcs.base.shiro.ShiroConstants;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Entity
@TableName("gcs_user")
@KeySequence(value = "seq_gcsuser_id")
public class GcsUser extends BaseDomain {
	private static final long serialVersionUID = 1L;

	public static int STATE_ACTIVE =  1;       // 已启用
	public static int STATE_DISABLED =  0;     // 已禁用

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsuser_id")
	@SequenceGenerator(name = "seq_gcsuser_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	/** 基本信息 */
	@NotBlank(message="用户名不能为空", groups = {InsertGroup.class, UpdateGroup.class})
	private String username;
	@NotBlank(message="密码不能为空", groups = InsertGroup.class)
	@Column(columnDefinition = "TEXT")
	private String passwordHash;
	private String mobile;               // 手 机
	private String email;                // 邮 箱
	private String nickname;             // 昵 称
	private String avatar;               // 头 像
	private String salt;

	/** 其它信息 */
	private Boolean isUnsafe;        // 需改密码
	private Boolean isActive;        // 是否启用  1:启用  0:禁用
	private Date authcCheckDate;     // 此日期前Token必须重新登陆(修改密码)

	/** 关联关系 */
	private String wxOpenId;         //WxOpenId

	@Transient
	@NotNull(message="角色不能为空")
	private Long gcsRoleId;             // 角色Id
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private GcsRole gcsRole;


	@Transient
	@NotNull(message="部门不能为空")
	private Long gcsGroupId;             // 部门Id
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private GcsGroup gcsGroup;

	/** 其它信息 */
	@Column(columnDefinition = "TEXT")
	private String remark;

	/** 瞬态字段 */
	@Transient
	@TableField(exist = false)
	private String password;

	public Boolean isSuperAdmin(){
		return ShiroConstants.SUPER_ADMIN_ROLE_ID.equals(gcsRoleId);
	}
}
