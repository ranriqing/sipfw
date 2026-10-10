package gcs.modules.storage.domain;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import gcs.base.BaseDomain;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "gcs_resource")
@TableName("gcs_resource")
@KeySequence(value = "seq_gcsresource_id")
public class GcsResource extends BaseDomain {

    @Id
    @TableId
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gcsresource_id")
    @SequenceGenerator(name = "seq_gcsresource_id", initialValue = 1001, allocationSize = 1)
    private Long id;

    private Long version=1L;

    // 基本信息
    private String fileName;            // 文件名称
    private String contentType;         // 文件ContentType
    private String uploadType;          // 上传类型
    private String fileUuid;            // 文件唯一标识(用于下载文件，防止暴漏可推测的路径)

    // 扩展信息
    private String identifier;          // 资源库中文件标准
    private Long length;                // 未加密时文件长度
    private String hash;                // 文件更新时自动更新的Hash标记
    private String resourceUri;         // 文件存储路径


}
