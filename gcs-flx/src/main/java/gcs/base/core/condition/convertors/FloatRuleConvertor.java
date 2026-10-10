package gcs.base.core.condition.convertors;

public class FloatRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{

    @Override
    Object convertValueType(Object value){
        String valueStr = (value!=null?value.toString():"").trim();
        try{
            return "MIN".equals(valueStr)?Float.MIN_VALUE:"MAX".equals(valueStr)?Float.MAX_VALUE:Float.parseFloat(valueStr);
        }catch (Exception e){
            return 0.0f;
        }
    }

}
