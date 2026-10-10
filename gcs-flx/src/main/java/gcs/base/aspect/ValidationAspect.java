package gcs.base.aspect;

import gcs.base.core.validators.ValidatorUtils;
import gcs.base.core.validators.group.InsertGroup;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.Validator;

// 参考文档：http://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/
@Aspect
@Component
public class ValidationAspect {

    @Autowired
    private Validator validator;

    /**
     * 使用ValidatorUtils工具类执行验证，基于分组规则
     * 当参数不符合验证规则时，会抛出GcsParamException异常
     */
    @Before("execution(* gcs.app.*.*(..))")
    public void validateMethodArguments(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                ValidatorUtils.validateByValidator(validator, arg, InsertGroup.class);
            }
        }
    }
}
