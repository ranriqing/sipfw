package gcs.app.ugrp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.domain.GcsPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GcsPermissionMapper extends BaseMapper<GcsPermission> {

}
