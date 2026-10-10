package gcs.modules.log.annotation;

import gcs.modules.log.aspect.GcsLogAspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @see GcsLogAspect
 * @see gcs.modules.log.domain.GcsLog
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GLog {

	// 用户操作 operation
	String value() default "";
}
