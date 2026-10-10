package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gcs.modules.tag.domain.GcsTag;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@TableName("gcs_user_link")
@KeySequence(value = "seq_gcsuserlink_id")
public class GcsUserLink implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsuserlink_id")
    @SequenceGenerator(name = "seq_gcsuserlink_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    /** 关联关系 */
    @Transient
    private Long gcsUserId;
    @JsonIgnore
    @ManyToOne
    @TableField(exist = false)
    private GcsUser gcsUser;

    /** 关联实体 */
    private String referenceClass;
    private Long referenceId;
    private String referenceType;

    /** 瞬态字段 */
    @Transient
    @TableField(exist = false)
    private String username;

    @Transient
    @TableField(exist = false)
    private String nickname;
}
