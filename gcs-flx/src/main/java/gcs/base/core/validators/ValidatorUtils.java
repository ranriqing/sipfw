package gcs.base.core.validators;

import gcs.base.core.bean.ParamViolation;
import gcs.base.core.exception.GcsException;
import gcs.base.core.exception.GcsParamException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * hibernate-validator校验工具类
 * 参考文档：http://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/
 */
public class ValidatorUtils {
    private static volatile Validator _validator;

    private static Validator getValidator(){
        // 第一次检查
        if (_validator == null) {
            synchronized (ValidatorUtils.class) {
                // 第二次检查
                if (_validator == null) {
                    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                    _validator = factory.getValidator();
                }
            }
        }
        return _validator;
    }

    /**
     * 校验对象
     * @param object         待校验对象
     * @param groups         待校验的组
     * @throws GcsException  校验不通过，则报GcsException异常
     */
    public static void validateObj(Object object, Class<?>... groups) throws GcsException {
        validateByValidator(getValidator(), object, groups);
    }


    /**
     * 校验对象
     * @param validator      校验器
     * @param object         待校验对象
     * @param groups         待校验的组
     * @throws GcsException  校验不通过，则报GcsException异常
     */
    public static void validateByValidator(Validator validator, Object object, Class<?>... groups) throws GcsException {
        Set<ConstraintViolation<Object>> violations = validator.validate(object, groups);
        if (!violations.isEmpty()) {
            List<ParamViolation> paramViolations = violations.stream().map(item ->
                    new ParamViolation(item.getPropertyPath().toString(), item.getInvalidValue(), item.getMessage())
            ).collect(toList());
            throw new GcsParamException("校验失败", paramViolations);
        }
    }
}
