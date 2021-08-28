package fun.fengwk.autovalidation.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author fengwk
 */
@Documented
@Target({ PARAMETER })
@Retention(RUNTIME)
public @interface TestAnnotation {

    String[] value() default {};

}
