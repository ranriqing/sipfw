package gcs.base.core.condition;

import lombok.Data;

import java.util.List;

@Data
public class GcsRule {
    // 子规则组
    String condition = BaseCondition.CONDITION_AND;
    List<GcsRule> rules;

    // 查询规则
    String operator;  // 操作符
    String field;     // 字段名
    Object value;     // 字段值
}
