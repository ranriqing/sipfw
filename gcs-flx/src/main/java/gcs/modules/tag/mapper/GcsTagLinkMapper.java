package gcs.modules.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.modules.tag.domain.GcsTag;
import gcs.modules.tag.domain.GcsTagLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface GcsTagLinkMapper extends BaseMapper<GcsTagLink> {

    List<GcsTagLink> selectListByReference(@Param("referenceId") Long referenceId,  @Param("referenceClass") String referenceClass, @Param("tagName") String tagName);

    List<Map<String, Object>> countByTagIds(@Param("tagIds") List<Long> tagIds);
}
