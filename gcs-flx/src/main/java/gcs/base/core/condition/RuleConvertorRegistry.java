package gcs.base.core.condition;

import gcs.app.vm.constants.VmConstants;
import gcs.base.core.condition.convertors.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查询条件
 * 内容的转换规则定义
 */
class RuleConvertorRegistry {

    // 转换规则注册中心
    static Map<String,RuleConvertor> ruleConvertorMap = new ConcurrentHashMap<String, RuleConvertor>();
    static {
        ruleConvertorMap.put(VmConstants.FIELD_TYPE_STRING,new StringRuleConvertor());
        ruleConvertorMap.put(VmConstants.FIELD_TYPE_BOOLEAN,new BooleanRuleConvertor());
        ruleConvertorMap.put(VmConstants.FIELD_TYPE_INTEGER,new IntegerRuleConvertor());
        ruleConvertorMap.put(VmConstants.FIELD_TYPE_LONG,new LongRuleConvertor());
        ruleConvertorMap.put(VmConstants.FIELD_TYPE_BIG_DECIMAL,new BigDecimalRuleConvertor());
        ruleConvertorMap.put(VmConstants.FIELD_TYPE_DATE,new DateRuleConvertor());
        ruleConvertorMap.put("Double",new DoubleRuleConvertor());
        ruleConvertorMap.put("Float",new FloatRuleConvertor());
        ruleConvertorMap.put("Object",new ObjectRuleConvertor());
        // ruleConvertorMap.put(VmConstants.FIELD_TYPE_MANY2ONE, "无需配置");
        ruleConvertorMap.put(VmConstants.FIELD_TYPE_ONE2MANY,new OneToManyRuleConvertor());
        ruleConvertorMap.put(VmConstants.FIELD_TYPE_LINK_USER,new LinkUserRuleConvertor());
    }

    /**
     * 解析转换规则
     * @param convertorType
     * @return
     */
    static RuleConvertor resolveConvertor(String convertorType){
        if(convertorType!=null){
            convertorType = StringUtils.capitalize(convertorType);
            return ruleConvertorMap.get(convertorType);
        } else {
            return null;
        }
    }


}
