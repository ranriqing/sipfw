package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.*;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@TableName("gcs_group")
public class InitGcsGroup extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	/** 基本信息 */
	@NotNull(message="部门名不能为空")
	private String name;
	private String icon;
	private Integer sequence = 100;

	/** 关联关系 */
	@Transient
	private Long parentId;
}
