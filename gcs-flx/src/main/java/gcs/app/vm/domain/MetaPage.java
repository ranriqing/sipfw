package gcs.app.vm.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gcs.app.ugrp.domain.GcsRole;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@TableName("meta_page")
@KeySequence(value = "seq_metapage_id")
public class MetaPage extends BaseDomain {
	private static final long serialVersionUID = 1L;

	public static Integer TYPE_OPEN = 0;
	public static Integer TYPE_ADMIN = 1;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metapage_id")
	@SequenceGenerator(name = "seq_metapage_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	@NotNull(message="标识不能为空")
	private String pdent;                         // 页面来源标识，用于跳转
	private String ident;
	private String title;
	private String component;
	private Integer seqOrder = 100;
	private Boolean initialized = false;
	@Column(columnDefinition = "TEXT")
	private String extend;

	/** 关联关系 */
	@Transient
	private Long metaViewId;
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaView metaView;

	@Transient
	private Long metaViewMenuId;
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaViewMenu metaViewMenu;

	/** 其它信息 */
	@Column(columnDefinition = "TEXT")
	private String remark;
}
