package gcs.modules.config.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.config.domain.InitScElement;
import gcs.modules.config.domain.ScElement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InitScElementMapper extends BaseMapper<InitScElement> {
	
}
