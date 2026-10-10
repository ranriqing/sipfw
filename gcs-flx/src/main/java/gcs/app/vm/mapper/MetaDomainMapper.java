package gcs.app.vm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.ugrp.domain.GcsRole;
import gcs.app.vm.domain.MetaDomain;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetaDomainMapper extends BaseMapper<MetaDomain> {

}
