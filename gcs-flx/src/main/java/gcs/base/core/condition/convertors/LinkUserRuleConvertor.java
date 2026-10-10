package gcs.base.core.condition.convertors;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.domain.GcsUserLink;
import gcs.app.ugrp.mapper.GcsUserLinkMapper;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.GcsConstants;
import gcs.base.core.GcsHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LinkUserRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{

    @Override
    public void convertEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        VmClassField vmClassField = context!=null?(VmClassField)context.get(VmConstants.VM_CLASS_FIELD):null;
        String vmDomainName = vmClassField.getVmClass().getDomainName();
        Long userId = (Long)this.convertValueType(value);
        boolean useSql = field!=null;
        if(useSql){
            wrapper.apply(
            "id IN (SELECT reference_id FROM gcs_user_link WHERE gcs_user_id = {0} AND reference_class = {1} AND reference_type = {2})",
            userId, vmDomainName, field
            );
        }else{
            GcsUserLinkMapper gcsUserLinkMapper = GcsHolder.getBean(GcsUserLinkMapper.class);
            List<GcsUserLink> userLinks = gcsUserLinkMapper.selectList(
                Wrappers.lambdaQuery(GcsUserLink.class)
                    .eq(GcsUserLink::getGcsUserId, userId)
                    .eq(GcsUserLink::getReferenceClass, vmDomainName)
                    .eq(GcsUserLink::getReferenceType, field)
            );
            List<Long> referenceIds = userLinks.stream().map(GcsUserLink::getReferenceId).collect(Collectors.toList());
            referenceIds.add(0L);
            wrapper.in(BaseDomain.ID, referenceIds);
        }
    }

    @Override
    Object convertValueType(Object value){
        return Convert.toLong(value, 0L);
    }
}
