package gcs.app.vm.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@TableName("meta_domain_field")
@KeySequence(value = "seq_metadomainfield_id")
public class MetaDomainField extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metadomainfield_id")
	@SequenceGenerator(name = "seq_metadomainfield_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	@NotNull(message="标题不能为空")
	private String title;
	@Pattern(regexp = "^([a-zA-Z])([a-zA-Z0-9_])+$", message="字段名不合规")
	private String fieldName;
	private String fieldType;
	private Integer seqOrder = 100;
	private Boolean visible = true;
	/** 是否启用全局搜索（仅文本类字段可用） */
	private Boolean globalSearch = false;
	@Column(columnDefinition = "TEXT")
	private String extendJson;
	@Column(columnDefinition = "TEXT")
	private String constraintJson;

	/** 关联关系 */
	@Transient
	@NotNull(message="所属数据表不能为空")
	private Long metaDomainId;
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaDomain metaDomain;

}
