package gcs.app.vm.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import gcs.base.BaseDomain;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@TableName("meta_view")
@KeySequence(value = "seq_metaview_id")
public class MetaView extends BaseDomain {
	private static final long serialVersionUID = 1L;

	public static Integer TYPE_OPEN = 0;
	public static Integer TYPE_ADMIN = 1;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metaview_id")
	@SequenceGenerator(name = "seq_metaview_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	@NotNull(message="名称不能为空")
	private String name;
	private Integer type = TYPE_ADMIN;            // 视图类型
	private String icon;
	private Integer seqOrder = 100;
	private Boolean active = true;

	/** 其它信息 */
	@Column(columnDefinition = "TEXT")
	private String remark;

}
