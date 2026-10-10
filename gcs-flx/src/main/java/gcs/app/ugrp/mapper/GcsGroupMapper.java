package gcs.app.ugrp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.domain.GcsUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GcsGroupMapper extends BaseMapper<GcsGroup> {

}
