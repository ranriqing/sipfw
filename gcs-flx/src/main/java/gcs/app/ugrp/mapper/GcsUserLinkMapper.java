package gcs.app.ugrp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.GcsUserLink;
import gcs.modules.tag.domain.GcsTagLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GcsUserLinkMapper extends BaseMapper<GcsUserLink> {
    List<GcsUserLink> selectListByReference(@Param("referenceClass") String referenceClass, @Param("referenceId") Long referenceId, @Param("referenceType") String referenceType);
}
