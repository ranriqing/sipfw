package gcs.base.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.Validation;
import javax.validation.Validator;

@Configuration
public class ValidatorConfig {

    /**
     * 配置JSR-380 Bean Validation规范的校验器
     * 使用Hibernate Validator作为默认实现，提供对象属性校验功能
     * 支持@NotNull、@Size、@Pattern等标准校验注解
     */
    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
