package gcs.modules.relation.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@TableName("gcs_relation")
@KeySequence(value = "seq_gcsrelation_id")
public class GcsRelation extends BaseDomain {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsrelation_id")
    @SequenceGenerator(name = "seq_gcsrelation_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    private Long version=1L;

    /** 主关联方 */
    private String aReferenceClass;
    private Long aReferenceId;

    /** 被关联方 */
    private String bReferenceClass;
    private Long bReferenceId;

    /** 扩展关联方 */
    private String extendReferenceClass;
    private Long extendReferenceId;

    /** 其它信息 */
    @Column(columnDefinition = "TEXT")
    private String remark;
}
