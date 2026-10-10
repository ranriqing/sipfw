package gcs.modules.activity.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import gcs.app.ugrp.domain.GcsUser;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@TableName("gcs_activity")
@KeySequence(value = "seq_gcsactivity_id")
public class GcsActivity extends BaseDomain {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsactivity_id")
    @SequenceGenerator(name = "seq_gcsactivity_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    /** 关联方 */
    private String referenceClass;
    private Long referenceId;

    /** 通用内容 */
    @Column(columnDefinition = "TEXT")
    private String title;               // 活动标题
    @Column(columnDefinition = "TEXT")
    private String detail;              // 活动详情

    /** 工作计划 */
    private Boolean isActive;           // 计划状态 1:待办  0:已办
    private Long sciScheduleType;       // 计划类型
    private Date dueDate;               // 计划截止日
    private Date finishDate;            // 计划完成时间
    private Date lastNotifyDate;        // 上次提醒时间

    @Column(columnDefinition = "TEXT")
    private String comment;             // 完成备注

    @Transient
    private Long assigneeId;
    @ManyToOne
    @TableField(exist = false)
    private GcsUser assignee;            // 计划指派人

    /** 操作人 && 操作组 */
    private Long actorId;
    private String actorCaption;
    private Long groupId;
    private String groupCaption;

}
