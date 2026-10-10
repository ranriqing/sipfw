package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import gcs.base.core.validators.group.InsertGroup;
import gcs.base.core.validators.group.UpdateGroup;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
@Entity
@TableName("wechat_user")
@KeySequence(value = "seq_wechatuser_id")
public class WechatUser extends BaseDomain {
	private static final long serialVersionUID = 1L;

	public static int STATE_ACTIVE =  1;       // 已启用
	public static int STATE_DISABLED =  0;     // 已禁用

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_wechatuser_id")
	@SequenceGenerator(name = "seq_wechatuser_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	@NotBlank(message="OpenId不能为空", groups = {InsertGroup.class, UpdateGroup.class})
	String wxOpenId;
	String wxUnionId;
	String wxNickname;
	String wxHeadImgUrl;
	Boolean wxSubscribe;
	Date wxSubscribeTime;

	/** 其它信息 */
	private Integer isActive;        // 是否启用  1:启用  0:禁用
	private Date authcCheckDate;     // 此日期前Token必须重新登陆

}
