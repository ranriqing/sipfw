package gcs.app.vm.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gcs.app.ugrp.domain.GcsRole;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@TableName("meta_permission")
@KeySequence(value = "seq_metapermission_id")
public class MetaPermission extends BaseDomain {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_metapermission_id")
    @SequenceGenerator(name = "seq_metapermission_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    /** 基本信息 */
    @NotNull(message="标题不能为空")
    private String title;
    private String icon;
    private Integer seqOrder = 100;
    private String type;                   // 权限类型：panel、select、check
    private String permission;             // 权限标识(用于角色权限key)

    @Column(columnDefinition = "TEXT")
    private String extend;                 // eg: {"cols":24}

    /** 关联关系 */
    @Transient
    private Long gcsRoleId;               // 如果关联GcsRole(角色特有权限)，不关联(通用权限)
    @JsonIgnore
    @ManyToOne
    @TableField(exist = false)
    private GcsRole gcsRole;
}
