package gcs.modules.config.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.config.domain.ScConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScConfigMapper extends BaseMapper<ScConfig> {
	
}
