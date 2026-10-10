package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.*;
import gcs.app.vm.domain.MetaView;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@TableName("gcs_role")
@KeySequence(value = "seq_gcsrole_id")
public class GcsRole extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsrole_id")
	@SequenceGenerator(name = "seq_gcsrole_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	/** 基本信息 */
	@NotNull(message="角色名称不能为空")
	private String name;
	private Integer type = 1;            // 0 开放角色  1 超管角色

	/** 其它信息 */
	private String remark;

	/** 关联关系 */
	@Transient
	private Long viewId;
	@ManyToOne
	@TableField(exist = false)
	private MetaView view;

}
