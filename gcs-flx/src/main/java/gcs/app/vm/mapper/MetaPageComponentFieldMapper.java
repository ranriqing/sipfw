package gcs.app.vm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.vm.domain.MetaPageComponent;
import gcs.app.vm.domain.MetaPageComponentField;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetaPageComponentFieldMapper extends BaseMapper<MetaPageComponentField> {

}
