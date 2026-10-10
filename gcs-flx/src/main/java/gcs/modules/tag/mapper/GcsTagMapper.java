package gcs.modules.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.tag.domain.GcsTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GcsTagMapper extends BaseMapper<GcsTag> {

    List<String> listNamesByReferenceClass(@Param("referenceClass") String referenceClass);
}
