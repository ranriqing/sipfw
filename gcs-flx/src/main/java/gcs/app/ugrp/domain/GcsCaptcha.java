package gcs.app.ugrp.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@TableName("gcs_captcha")
// @KeySequence(value = "seq_gcscaptcha_id")
public class GcsCaptcha implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId(type = IdType.INPUT)
    private String uuid;

    @Version
    private Long version=1L;

    /** 基本信息 */
    private String code;
    private Date expireTime;
}
