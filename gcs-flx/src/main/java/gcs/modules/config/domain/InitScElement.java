package gcs.modules.config.domain;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sc_element")
public class InitScElement implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
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

    @Transient
    private Long parentId;              // 父选项

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
