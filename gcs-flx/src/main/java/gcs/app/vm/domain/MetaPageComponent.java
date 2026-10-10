package gcs.app.vm.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@TableName("meta_page_component")
@KeySequence(value = "seq_metapagecomponent_id")
public class MetaPageComponent extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metapagecomponent_id")
	@SequenceGenerator(name = "seq_metapagecomponent_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	@NotNull(message="标题不能为空")
	private String title;
	private String ident;
	@JsonProperty("pIdent")
	private String pIdent;
	private String type;                   // 标识页面组件的不同部分，eg：PART_QUERY、PART_TABLE
	private String component;              // 标识前端使用的具体组件，eg：PartQuery、PartTable
	private Integer seqOrder = 100;
	private Boolean initialized = false;
	@Column(columnDefinition = "TEXT")
	private String extend;

	/** 关联关系 */
	@Transient
	private Long metaDomainId;             // 如果关联MetaDomain(Domain页面组件)，不关联(纯页面组件)
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaDomain metaDomain;

}
