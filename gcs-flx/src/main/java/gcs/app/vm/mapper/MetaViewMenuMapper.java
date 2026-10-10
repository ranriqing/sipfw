package gcs.app.vm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.vm.domain.MetaView;
import gcs.app.vm.domain.MetaViewMenu;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetaViewMenuMapper extends BaseMapper<MetaViewMenu> {

}
