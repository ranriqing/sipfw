package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@Data
@TableName("gcs_role")
public class InitGcsRole extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	/** 基本信息 */
	@NotNull(message="角色名称不能为空")
	private String name;

	/** 其它信息 */
	private String remark;

	/** 关联关系 */
	@Transient
	private Long viewId;

}
