package gcs.base.core.condition.convertors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.Map;

public class StringRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{

    @Override
    public void convertContains(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.like(field, convertValueType(value));
    }

    @Override
    public void  convertBeginsWith(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.likeLeft(field, convertValueType(value));
    }

    @Override
    public void  convertEndsWith(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.likeRight(field, convertValueType(value));
    }

    @Override
    Object convertValueType(Object value){
        return (value!=null?value.toString():"").trim();
    }
}
