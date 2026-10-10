package gcs.base.core.condition.convertors;

public class IntegerRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{

    @Override
    Object convertValueType(Object value){
        String valueStr = (value!=null?value.toString():"").trim();
        try{
            return "MIN".equals(valueStr)?Integer.MIN_VALUE:"MAX".equals(valueStr)?Integer.MAX_VALUE:Integer.parseInt(valueStr);
        }catch (Exception e){
            return 0;
        }
    }
}
