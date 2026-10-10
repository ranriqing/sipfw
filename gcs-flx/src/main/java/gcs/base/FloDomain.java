package gcs.base;

import lombok.Data;

import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Date;

@Data
@MappedSuperclass
public abstract class FloDomain extends BaseDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SHEET_IDENT = "sheetIdent";
    public static final String APPLY_DESCRIPTION = "applyDescription";

    public static final String FLOW_DEF_KEY = "flowDefKey";
    public static final String FLOW_INST_ID = "flowInstId";
    public static final String FLOW_STATUS = "flowStatus";
    public static final String FLOW_SUBMIT_TIME = "flowSubmitTime";
    public static final String FLOW_UPDATE_TIME = "flowUpdateTime";
    public static final String FLOW_FINISH_TIME = "flowFinishTime";

    /** 基本信息 */
    private String sheetIdent;
    private String applyDescription;

    /** 流程信息 */
    private String flowDefKey;
    private String flowInstId;
    private Integer flowStatus;      // 流程状态
    private Date flowSubmitTime;
    private Date flowUpdateTime;
    private Date flowFinishTime;

    /** 申请信息 */
    private String seqNum;
}
