package gcs.app.sipfw.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

/**
 * SIP 前缀定义配置。
 */
@Data
@Entity
@Table(name = "sip_prefix_definition")
@TableName("sip_prefix_definition")
@KeySequence(value = "seq_sipprefixdefinition_id")
public class SipPrefixDefinition extends BaseDomain {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_sipprefixdefinition_id")
    @SequenceGenerator(name = "seq_sipprefixdefinition_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    @NotBlank(message = "prefix不能为空")
    private String prefix;

    @NotBlank(message = "displayName不能为空")
    private String displayName;

    /**
     * INBOUND / OUTBOUND
     */
    @NotBlank(message = "callDirection不能为空")
    private String callDirection = "INBOUND";

    /**
     * 比例类型开关。
     */
    private Boolean ratioTypeEnabled = Boolean.FALSE;

    /**
     * 特殊 SIP 编码映射 JSON。
     * 例如：{"603":"blacklist","503":"intercept"}
     */
    private String specialSipCodeJson = "{}";

    /**
     * 是否开启质检抽检。
     */
    private Boolean qaEnabled = Boolean.FALSE;

    /**
     * 质检话术映射 JSON。
     * 例如：{"begin":"开场白","end":"结束语"}
     */
    private String qaScriptJson = "{}";
}
