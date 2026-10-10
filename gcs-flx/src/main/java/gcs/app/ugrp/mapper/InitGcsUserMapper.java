package gcs.app.ugrp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.ugrp.domain.InitGcsRole;
import gcs.app.ugrp.domain.InitGcsUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InitGcsUserMapper extends BaseMapper<InitGcsUser> {

}
