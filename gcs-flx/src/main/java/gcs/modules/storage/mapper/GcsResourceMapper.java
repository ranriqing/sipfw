package gcs.modules.storage.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.storage.domain.GcsResource;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GcsResourceMapper extends BaseMapper<GcsResource> {
	
}
