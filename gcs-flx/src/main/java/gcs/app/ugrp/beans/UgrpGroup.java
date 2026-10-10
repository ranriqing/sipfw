package gcs.app.ugrp.beans;

import lombok.Data;

@Data
public class UgrpGroup {

    /** 基本信息 */
    private Long id;
    private String name;
    private Integer sequence;

    /** 关联关系 */
    private Long parentId;
}
