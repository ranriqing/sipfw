package gcs.base.core.condition.convertors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LongRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{

    // ----------------------------------------------- //
    @Override
    public void  convertIn(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        List<Long> valueList = new ArrayList<>();
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.indexOf(",") > 0) {
                String[] parts = strValue.split(",");
                for (String part : parts) {
                    valueList.add((Long)convertValueType(part));
                }
            } else {
                valueList.add((Long)convertValueType(strValue));
            }
        } else if (value instanceof List) {
            List<?> listValue = (List<?>) value;
            for (Object item : listValue) {
                valueList.add((Long)convertValueType(item));
            }
        }
        wrapper.in(field, valueList);
    }


    @Override
    Object convertValueType(Object value){
        String valueStr = (value!=null?value.toString():"").trim();
        try{
            return "MIN".equals(valueStr)?Long.MIN_VALUE:"MAX".equals(valueStr)?Long.MAX_VALUE:Long.parseLong(valueStr);
        }catch (Exception e){
            return 0L;
        }
    }

}
