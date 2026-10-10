package gcs.app.ugrp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import gcs.app.ugrp.domain.GcsUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GcsUserMapper extends BaseMapper<GcsUser> {

	List<String> listPermissions(@Param("userId") Long userId);

}
