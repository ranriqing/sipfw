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
@TableName("meta_page_component_field")
@KeySequence(value = "seq_metapagecomponentfield_id")
public class MetaPageComponentField extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metapagecomponentfield_id")
	@SequenceGenerator(name = "seq_metapagecomponentfield_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	@NotNull(message="标题不能为空")
	private String title;
	private String field;
	private String type;                             // 字段原始类型，eg：MetaDomainField.fieldType
	private String component;                        // 字段映射组件，eg：PartQuery中g-input、PartTable中linkRender
	private Integer seqOrder = 100;
	private String placeholder;
	private String pIdent;                           // 父标识，用于流程弹窗
	@Column(columnDefinition = "TEXT")
	private String extend;

	/** 关联关系 */
	@Transient
	@NotNull(message="所属组件不能为空")
	private Long metaPageComponentId;                // 所属组件，不能为空
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaPageComponent metaPageComponent;

	@Transient
	private Long metaDomainFieldId;                  // 可能关联到MetaDomainField
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaDomainField metaDomainField;

}
