package gcs.modules.config.domain;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Entity
@TableName("sc_element")
@KeySequence(value = "seq_scelement_id")
public class ScElement implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_scelement_id")
    @SequenceGenerator(name = "seq_scelement_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    /** 基本信息 */
    @NotNull(message="选项不能为空")
    private String title;
    private Integer seqOrder = 100;    // 排序
    private Boolean dfault = false;    // 是否默认

    /** 其它信息 */
    private String tagStyle;           // 标签样式
    private String description;        // 描述信息
    private String remark;

    /** 关联关系 */
    @Transient
    private Long scConfigId;            // 配置Id
    @ManyToOne
    @TableField(exist = false)
    private ScConfig scConfig;

    @Transient
    private Long parentId;              // 父选项
    @ManyToOne
    @TableField(exist = false)
    private ScElement parent;
    @JsonIgnore
    @Transient
    @TableField(exist = false)
    private List<ScElement> children;

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
