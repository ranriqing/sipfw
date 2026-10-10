package gcs.modules.log.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@Data
@Entity
@TableName("gcs_log")
@KeySequence(value = "seq_gcslog_id")
public class GcsLog implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@TableId
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcslog_id")
	@SequenceGenerator(name = "seq_gcslog_id", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Version
	private Long version=1L;

	/** 基本信息 */
	private String userType;           // 用户类型
	private Long userId;               // 用户ID
	private String username;           // 用户名
	@Column(length = 512)
	private String operation;          // 用户操作
	@Column(length = 1024)
	private String method;             // 请求方法
	@Column(columnDefinition = "TEXT")
	private String params;             // 请求参数
	private Long cost;                 // 执行时长(毫秒)
	private String ip;                 // IP地址

	/** 其它信息 */
	// @TableField(fill = FieldFill.INSERT)
	private Date dateCreated;
}
