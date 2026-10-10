package gcs.base;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gcs.base.core.bean.ParamViolation;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@MappedSuperclass
public abstract class BaseDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ID = "id";
    public static final String VERSION = "version";
    public static final String CREATOR_ID = "createrId";  // 使用creater兼容旧版本
    public static final String UPDATER_ID = "updaterId";
    public static final String DATE_CREATED = "dateCreated";
    public static final String LAST_UPDATED = "lastUpdated";

    public static final String SEARCH_KEYWORD = "searchKeyword";
    public static final String ATTACHMENT_LIST = "attachmentList";
    public static final String VIOLATION_LIST = "violationList";

    /** 创建更新 */
    @TableField(fill = FieldFill.INSERT)
    private Long createrId;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updaterId;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateCreated;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdated;

    /** 搜索值 */
    @JsonIgnore
    @Transient
    @TableField(exist = false)
    private String searchKeyword;

    /** 活动列表 */
    @JsonIgnore
    @Transient
    @TableField(exist = false)
    private List<JSONObject> activityList;

    /** 附件列表 */
    @JsonIgnore
    @Transient
    @TableField(exist = false)
    private List<JSONObject> attachmentList;

    /** 标签列表 */
    @JsonIgnore
    @Transient
    @TableField(exist = false)
    private List<String> tagList;

    /** 校验反馈 */
    @JsonIgnore
    @Transient
    @TableField(exist = false)
    private List<ParamViolation> violationList;

    /** 抽象方法 */
    public abstract Long getId();
}
