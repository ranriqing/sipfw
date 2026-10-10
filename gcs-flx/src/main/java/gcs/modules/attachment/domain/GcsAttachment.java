package gcs.modules.attachment.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@TableName("gcs_attachment")
@KeySequence(value = "seq_gcsattachment_id")
public class GcsAttachment extends BaseDomain {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsattachment_id")
    @SequenceGenerator(name = "seq_gcsattachment_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    private Long version=1L;

    /** 关联方 */
    private String referenceClass;
    private Long referenceId;

    /** 关联资源 */
    // 通过GcsRelationService来实现
    private String type;  // 附件类型

    /** 操作人 && 操作组 */
    private Long actorId;
    private String actorCaption;
    private Long groupId;
    private String groupCaption;

}
