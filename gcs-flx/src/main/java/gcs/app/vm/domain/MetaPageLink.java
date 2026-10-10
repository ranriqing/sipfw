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
@TableName("meta_page_link")
@KeySequence(value = "seq_metapagelink_id")
public class MetaPageLink extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metapagelink_id")
	@SequenceGenerator(name = "seq_metapagelink_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	private Integer seqOrder = 100;

	/** 关联关系 */
	@Transient
	private Long metaPageId;
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaPage metaPage;

	@Transient
	private Long metaPageComponentId;
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaPageComponent metaPageComponent;

}
