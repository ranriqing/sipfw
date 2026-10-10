package gcs.base.core.condition.convertors;

public class DoubleRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{

    @Override
    Object convertValueType(Object value){
        String valueStr = (value!=null?value.toString():"").trim();
        try{
            return "MIN".equals(valueStr)?Double.MIN_VALUE:"MAX".equals(valueStr)?Double.MAX_VALUE:Double.parseDouble(valueStr);
        }catch (Exception e){
            return 0d;
        }
    }

}
