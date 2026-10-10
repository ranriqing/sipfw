package gcs.base.core.bean;

import lombok.Data;

/**
 * [参数有误]
 */
@Data
public class ParamViolation {

    private String field;
    private Object rejectedValue;
    private String message;

    public ParamViolation(String field, Object rejectedValue, String message) {
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.message = message;
    }
}
