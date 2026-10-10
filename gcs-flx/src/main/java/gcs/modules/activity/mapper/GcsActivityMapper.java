package gcs.modules.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.activity.domain.GcsActivity;
import gcs.modules.attachment.domain.GcsAttachment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GcsActivityMapper extends BaseMapper<GcsActivity> {

}
