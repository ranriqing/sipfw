package gcs.modules.message.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import gcs.base.BaseDomain;
import gcs.base.tools.excel.Excel;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@TableName("gcs_message")
@KeySequence(value = "seq_gcsmessage_id")
public class GcsMessage extends BaseDomain {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsmessage_id")
    @SequenceGenerator(name = "seq_gcsmessage_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    /** 发件人 */
    @Excel(name = "发件人")
    private Long sendUserId;

    /** 收件人 */
    @Excel(name = "收件人")
    private Long receiveUserId;

    /** 发件人标题 */
    @Excel(name = "发件人标题")
    @Column(columnDefinition = "TEXT")
    private String title;

    /** 发送内容 */
    @Excel(name = "发送内容")
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发送时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date sendTime;

    /** 是否阅读 */
    @Excel(name = "是否阅读")
    private String ifRead;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("sendUserId", getSendUserId())
            .append("receiveUserId", getReceiveUserId())
            .append("title", getTitle())
            .append("content", getContent())
            .append("sendTime", getSendTime())
            .append("ifRead", getIfRead())
            .toString();
    }
}
