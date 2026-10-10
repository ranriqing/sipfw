package gcs.modules.others.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Data
@Entity
@TableName("addr_area")
public class AddrArea extends BaseDomain {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	private Long id;

	/** 基本信息 */
	private String name;
	private Integer seqOrder = 100;

	/** 关联关系 */
	@Transient
	private Long addrCityId;
	@ManyToOne
	@TableField(exist = false)
	private AddrCity addrCity;

}
