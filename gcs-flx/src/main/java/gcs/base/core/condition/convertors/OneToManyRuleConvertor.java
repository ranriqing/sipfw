package gcs.base.core.condition.convertors;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.base.CaseFormat;
import gcs.app.ugrp.domain.GcsUserLink;
import gcs.app.ugrp.mapper.GcsUserLinkMapper;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.component.fields.OneToManyClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.core.GcsConstants;
import gcs.base.core.GcsHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OneToManyRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{

    @Override
    public void convertEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        VmClassField vmClassField = context!=null?(VmClassField)context.get(VmConstants.VM_CLASS_FIELD):null;
        JSONObject jsonObject = vmClassField.getExtendMap();
        String relDomainName = jsonObject.getStr(OneToManyClassField.REL_DOMAIN_NAME);
        String relDomainFieldName = jsonObject.getStr(OneToManyClassField.REL_DOMAIN_FIELD_NAME);
        String relTableName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, relDomainName);
        String relTableFieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, relDomainFieldName + OneToManyClassField.ID_SUFFIX);
        Long relDomainId = (Long)this.convertValueType(value);
        wrapper.apply(String.format(
            "id IN (SELECT %s FROM %s WHERE id = %s)",
            relTableFieldName, relTableName, relDomainId
        ));
    }

    @Override
    Object convertValueType(Object value){
        return Convert.toLong(value, 0L);
    }
}
