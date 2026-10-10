package gcs.app.vm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@TableName("vm_field_change")
@KeySequence(value = "seq_vmfieldchange_id")
public class VmFieldChange implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_vmfieldchange_id")
    @SequenceGenerator(name = "seq_vmfieldchange_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    private String domainName;      // 域标识
    private Long recordId;          // 记录 id
    private String changeBatchId;   // UUID，同一次操作的字段变更归为一组
    private String operationType;   // CREATE / UPDATE / DELETE
    private String fieldName;       // 逻辑字段名
    private String fieldTitle;      // 字段标题
    private String fieldType;       // 字段类型（前端渲染用）
    @Column(columnDefinition = "TEXT")
    private String oldValue;        // 旧展示值（可空）
    @Column(columnDefinition = "TEXT")
    private String newValue;        // 新展示值（可空）
    private Long operatorId;        // 操作人 id
    private String operatorName;    // 操作人显示名
    private Date dateCreated;
}
