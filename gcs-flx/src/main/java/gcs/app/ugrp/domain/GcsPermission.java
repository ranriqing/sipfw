package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.*;
import gcs.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@Entity
@TableName("gcs_permission")
@KeySequence(value = "seq_gcspermission_id")
public class GcsPermission extends BaseDomain {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcspermission_id")
    @SequenceGenerator(name = "seq_gcspermission_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    /** 基本信息 */
    String permission;   // 权限key(原 name)
    String value;        // 权限值(原 permission)

    /** 其它信息 */
    // 暂无

    /** 关联关系 */
    private Long roleId;             // 角色Id

}
