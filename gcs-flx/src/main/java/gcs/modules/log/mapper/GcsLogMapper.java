package gcs.modules.log.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.log.domain.GcsLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GcsLogMapper extends BaseMapper<GcsLog> {
	
}
