package fun.fengwk.autovalidation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link AutoValidation}可以用于类（或枚举）、类（或枚举）构造器、类（或枚举）方法（如果在其它位置使用该注解，例如接口上将会产生编译期错误），
 * 标记了{@link AutoValidation}的类、类构造器、类方法将会在编译期间按需生成入参校验代码，
 * 使用{@link AutoValidation#groups()}可以指定校验所用的分组，类构造器与类方法的注解优先于类注解。
 * 例如在{@code setUsername}类方法上标记了{@link AutoValidation}注解，那么在编译期间就会自动生成如下校验代码：
 * <pre>
 * public void setUsername(@NotEmpty String username) {
 *     // 校验代码将在编译期进行插入
 *     GlobalValidator.checkParameters(User.class, "setUsername", new Class[]{String.class}, this, new Object[]{username}, new Class[0]);
 *     this.username = username;
 * }
 * </pre>
 *
 * @author fengwk
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface AutoValidation {

    Class<?>[] groups() default {};

}
