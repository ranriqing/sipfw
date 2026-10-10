package gcs.modules.activity.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gcs.app.ugrp.domain.GcsUser;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

// GcsActivity专用和GcsUser建立1对n关联
@Data
@Entity
@TableName("gcs_activity_link")
@KeySequence(value = "seq_gcsactivitylink_id")
public class GcsActivityLink implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsactivitylink_id")
    @SequenceGenerator(name = "seq_gcsactivitylink_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    /** 关联关系 */
    @Transient
    private Long gcsUserId;
    @JsonIgnore
    @ManyToOne
    @TableField(exist = false)
    private GcsUser gcsUser;

    /** 关联活动 */
    private Long activityId;

    /** 瞬态字段 */
    @Transient
    @TableField(exist = false)
    private String username;

    @Transient
    @TableField(exist = false)
    private String nickname;
}
