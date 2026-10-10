package gcs.app.vm.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@TableName("meta_domain")
@KeySequence(value = "seq_metadomain_id")
public class MetaDomain extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metadomain_id")
	@SequenceGenerator(name = "seq_metadomain_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	@NotNull(message="标题不能为空")
	private String title;
	@Pattern(regexp = "^([a-zA-Z])([a-zA-Z0-9_])+$", message="数据表名称不合规")
	private String domainName;
	private Integer seqOrder = 100;
	private Long sciDomainCategory;
	private Boolean visible = true;
	@Column(columnDefinition = "TEXT")
	private String extendJson;

	/** 其它信息 */
	@Column(columnDefinition = "TEXT")
	private String remark;

}
