package gcs.modules.attachment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.attachment.domain.GcsAttachment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GcsAttachmentMapper extends BaseMapper<GcsAttachment> {

}
