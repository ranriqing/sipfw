package gcs.app.vm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.vm.domain.MetaDomain;
import gcs.app.vm.domain.MetaView;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetaViewMapper extends BaseMapper<MetaView> {

}
