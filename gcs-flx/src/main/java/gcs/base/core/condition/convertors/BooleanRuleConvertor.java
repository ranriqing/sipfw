package gcs.base.core.condition.convertors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import gcs.base.core.convertors.TypeConvertorUtils;

import java.util.Map;

public class BooleanRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{
    @Override
    public void convertIn(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) { }

    @Override
    public void convertBetween(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {

    }

    @Override
    public void convertLess(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {

    }

    @Override
    public void convertLessOrEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {

    }

    @Override
    public void convertGreater(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {

    }

    @Override
    public void convertGreaterOrEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {

    }

    @Override
    Object convertValueType(Object value){
        String valueStr = (value!=null?value.toString():"").trim();
        return TypeConvertorUtils.convert(valueStr, Boolean.class);
    }
}
