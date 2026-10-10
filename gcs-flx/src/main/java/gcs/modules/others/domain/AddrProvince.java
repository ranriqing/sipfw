package gcs.modules.others.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@TableName("addr_province")
public class AddrProvince extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	private Long id;

	/** 基本信息 */
	private String name;
	private Integer seqOrder = 100;
}
