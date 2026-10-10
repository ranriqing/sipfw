package gcs.base.core.condition.convertors;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import gcs.base.core.GcsConstants;
import org.apache.commons.lang3.time.DateUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class DateRuleConvertor extends AbstractRuleConvertor implements RuleConvertor{
    @Override
    public void convertIn(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        
    }

    @Override
    public void convertEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        
    }

    @Override
    public void convertNotEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        
    }

    @Override
    public void convertBetween(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context) {
        // value转为List
        List<Object> originalList = new ArrayList<>();
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.indexOf(',') >= 0) {
                originalList = Arrays.asList(strValue.split(","));
            } else {
                originalList.add(strValue);
            }
        } else if (value instanceof List) {
            originalList = (List<Object>) value;
        }
        // originalList 转为 valueList
        List<Object> valueList = new ArrayList<>(Arrays.asList("MIN", "MAX"));
        if (originalList.size() > 1) {
            valueList.set(0, originalList.get(0) != null ? originalList.get(0) : valueList.get(0));
            valueList.set(1, originalList.get(1) != null ? originalList.get(1) : valueList.get(1));
        } else if (originalList.size() > 0) {
            valueList.set(0, originalList.get(0) != null ? originalList.get(0) : valueList.get(0));
        }
        // 转为日期格式
        for (int i = 0; i < valueList.size(); i++) {
            valueList.set(i, convertValueType(valueList.get(i)));
        }
        // toDate修正查询
        String toDateStr = originalList.get(1).toString();
        Date toDate = (Date)valueList.get(1);
        if (toDateStr.matches("\\d{4}/\\d\\d")) {
            toDate = DateTime.of(toDate).offset(DateField.MONTH, 1).offset(DateField.SECOND, -1);
        }else if (toDateStr.matches("\\d{4}/\\d\\d/\\d\\d")) {
            toDate = DateTime.of(toDate).offset(DateField.DAY_OF_YEAR, 1).offset(DateField.SECOND, -1);
        } else if (toDateStr.matches("\\d{4}/\\d\\d/\\d\\d \\d\\d:\\d\\d")) {
            toDate = DateTime.of(toDate).offset(DateField.MINUTE, 1).offset(DateField.SECOND, -1);
        } else {
            toDate = DateTime.of(toDate).offset(DateField.SECOND, 0);
        }
        wrapper.between(field, valueList.get(0), toDate);
    }

    @Override
    Object convertValueType(Object value){
        if (value == null) {
            return new Date(0);
        }
        String valueStr = value.toString().trim();
        try {
            if ("MIN".equals(valueStr)) {
                return new Date(0);
            } else if ("MAX".equals(valueStr)) {
                long currentTime = System.currentTimeMillis();
                long oneHundredYearsInMillis = TimeUnit.DAYS.toMillis(365 * 10);
                return new Date(currentTime + oneHundredYearsInMillis);
            } else {
                return DateUtils.parseDate(valueStr, GcsConstants.DATETIME_MINSEC_PATTERN, GcsConstants.DATETIME_MIN_PATTERN, GcsConstants.DATE_DAY_PATTERN, GcsConstants.DATE_MONTH_PATTERN);
            }
        } catch (Exception e) {
            // 转换失败，返回默认日期
        }
        return new Date(0);
    }
}
