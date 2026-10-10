package gcs.base.core.condition.convertors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractRuleConvertor implements RuleConvertor{

    // ----------------------------------------------- //
    @Override
    public void convertIn(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {

        List<Object> valueList = new ArrayList<>();
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.indexOf(",") > 0) {
                String[] parts = strValue.split(",");
                for (String part : parts) {
                    valueList.add(convertValueType(part));
                }
            } else {
                valueList.add(convertValueType(strValue));
            }
        } else if (value instanceof List) {
            List<?> listValue = (List<?>) value;
            for (Object item : listValue) {
                valueList.add(convertValueType(item));
            }
        }
        wrapper.in(field, valueList);
    }

    @Override
    public void convertIdsubIn(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
    }

    @Override
    public void convertBetween(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        List<Object> valueList = new ArrayList<>(Arrays.asList("MIN", "MAX"));
        List<Object> tmpList = new ArrayList<>();
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.contains(",")) {
                tmpList = Arrays.asList(strValue.split(","));
            } else {
                tmpList.add(strValue);
            }
        } else if (value instanceof List) {
            tmpList = (List<Object>) value;
        }
        if (tmpList.size() > 1) {
            valueList.set(0, tmpList.get(0) != null ? tmpList.get(0) : valueList.get(0));
            valueList.set(1, tmpList.get(1) != null ? tmpList.get(1) : valueList.get(1));
        } else if (tmpList.size() > 0) {
            valueList.set(0, tmpList.get(0) != null ? tmpList.get(0) : valueList.get(0));
        }
        for (int i = 0; i < valueList.size(); i++) {
            valueList.set(i, convertValueType(valueList.get(i)));
        }
        wrapper.between(field, valueList.get(0), valueList.get(1));
    }

    // ----------------------------------------------- //

    @Override
    public void convertEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        if (value instanceof String && value.toString().contains(",")) {
            wrapper.in(field, Arrays.stream(((String) value).split(",")).map(this::convertValueType).collect(Collectors.toList()));
        }else{
            wrapper.eq(field, convertValueType(value));
        }
    }

    @Override
    public void convertNotEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.ne(field, convertValueType(value));
    }

    @Override
    public void convertContains(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
    }

    @Override
    public void convertBeginsWith(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
    }

    @Override
    public void convertEndsWith(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
    }

    @Override
    public void convertLess(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.lt(field, convertValueType(value));
    }

    @Override
    public void convertLessOrEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.le(field, convertValueType(value));
    }

    @Override
    public void convertGreater(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.gt(field, convertValueType(value));
    }

    @Override
    public void convertGreaterOrEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.ge(field, convertValueType(value));
    }

    @Override
    public void convertIsNull(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.isNull(field);
    }

    @Override
    public void convertIsNotNull(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        wrapper.isNotNull(field);
    }

    abstract Object convertValueType(Object value);
}
