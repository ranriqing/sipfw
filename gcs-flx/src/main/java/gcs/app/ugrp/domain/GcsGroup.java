package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@TableName("gcs_group")
@KeySequence(value = "seq_gcsgroup_id")
public class GcsGroup extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsgroup_id")
	@SequenceGenerator(name = "seq_gcsgroup_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	/** 基本信息 */
	@NotNull(message="部门名不能为空")
	private String name;
	private String icon;
	private Integer sequence = 100;

	/** 其它信息 */
	String remark;

	/** 关联关系 */
	@Transient
	private Long parentId;
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private GcsGroup parent;

}
