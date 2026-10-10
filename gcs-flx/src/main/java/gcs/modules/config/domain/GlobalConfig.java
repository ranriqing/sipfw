package gcs.modules.config.domain;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@TableName("global_config")
@KeySequence(value = "seq_globalconfig_id")
public class GlobalConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_globalconfig_id")
    @SequenceGenerator(name = "seq_globalconfig_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    @Version
    private Long version=1L;

    /** 基本信息 */
    @NotNull(message="Key不能为空")
    private String key;
    private String value;

    /** 其它信息 */
    private String remark;

    /** 创建更新 */
    @TableField(fill = FieldFill.INSERT)
    private Long createrId;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updaterId;
    @TableField(fill = FieldFill.INSERT)
    private Date dateCreated;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date lastUpdated;
}
