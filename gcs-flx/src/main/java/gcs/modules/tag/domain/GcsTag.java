package gcs.modules.tag.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@TableName("gcs_tag")
@KeySequence(value = "seq_gcstag_id")
public class GcsTag extends BaseDomain {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcstag_id")
    @SequenceGenerator(name = "seq_gcstag_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    /** 基本信息 */
    private Integer seqOrder = 100;
    private String name;
    private String color;

    /** 使用数（瞬态，被多少条记录引用） */
    @Transient
    @TableField(exist = false)
    private Long usageCount;

}
