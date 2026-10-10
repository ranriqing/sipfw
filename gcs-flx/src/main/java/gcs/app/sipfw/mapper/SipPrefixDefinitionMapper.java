package gcs.app.sipfw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.app.sipfw.domain.SipPrefixDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SipPrefixDefinitionMapper extends BaseMapper<SipPrefixDefinition> {
}
