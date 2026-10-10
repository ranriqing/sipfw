package gcs.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.message.domain.GcsMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GcsMessageMapper extends BaseMapper<GcsMessage> {
}
