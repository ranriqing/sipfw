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
@TableName("sc_config")
@KeySequence(value = "seq_scconfig_id")
public class ScConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_scconfig_id")
    @SequenceGenerator(name = "seq_scconfig_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    @Version
    private Long version=1L;

    /** 基本信息 */
    private String title;
    @NotNull(message="Key不能为空")
    private String key;
    private Integer seqOrder = 100;
    private Integer sciScCategory;            // 选项分类
    private Boolean multiLevel = false;       // 多层结构
    private Boolean allowModify = true;       // 允许编辑
    @NotNull(message="不能为空")
    private Boolean freeCreation = true;      // 自由创建

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
