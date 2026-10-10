package gcs.base.core.condition.convertors;

import java.math.BigDecimal;

public class BigDecimalRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{
    @Override
    Object convertValueType(Object value){
        String valueStr = (value!=null?value.toString():"").trim();
        try{
            return "MIN".equals(valueStr)?new BigDecimal(Integer.MIN_VALUE):"MAX".equals(valueStr)?new BigDecimal(Integer.MAX_VALUE):new BigDecimal(valueStr);
        }catch (Exception e){
            return new BigDecimal(0);
        }
    }
}
