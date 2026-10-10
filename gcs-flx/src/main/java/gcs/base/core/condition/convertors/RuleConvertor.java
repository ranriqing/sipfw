package gcs.base.core.condition.convertors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.Map;

public interface RuleConvertor {
    // ----------------------------------------------- //
    void convertIn(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertIdsubIn(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertBetween(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    // ----------------------------------------------- //
    void convertEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertNotEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertContains(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertBeginsWith(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertEndsWith(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertLess(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertLessOrEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertGreater(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertGreaterOrEqual(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertIsNull(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
    void convertIsNotNull(QueryWrapper<?> wrapper, String field, Object value, Map<String, Object> context);
}
