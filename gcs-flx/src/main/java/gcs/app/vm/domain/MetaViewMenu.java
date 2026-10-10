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
@TableName("meta_view_menu")
@KeySequence(value = "seq_metaviewmenu_id")
public class MetaViewMenu extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metaviewmenu_id")
	@SequenceGenerator(name = "seq_metaviewmenu_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	@NotNull(message="标题不能为空")
	private String title;
	private String icon;
	private Integer seqOrder = 100;
	private String bindHref;

	/** 关联关系 */
	@Transient
	private Long metaViewId;
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaView metaView;

	@Transient
	private Long parentId;
	@JsonIgnore
	@ManyToOne
	@TableField(exist = false)
	private MetaViewMenu parent;

	/** 其它信息 */
	@Column(columnDefinition = "TEXT")
	private String remark;
}
