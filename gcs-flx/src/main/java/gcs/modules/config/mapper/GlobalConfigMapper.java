package gcs.modules.config.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.config.domain.GlobalConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GlobalConfigMapper extends BaseMapper<GlobalConfig> {
	
}
