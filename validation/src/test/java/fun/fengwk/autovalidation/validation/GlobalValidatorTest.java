package fun.fengwk.autovalidation.validation;

import org.junit.Test;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author fengwk
 */
public class GlobalValidatorTest {

    @Test(expected = ConstraintViolationException.class)
    public void test1() {
        new CheckObj(new User(), 16);
    }
    
    @Test
    public void test2() {
        User user = new User();
        user.username = "username";
        new CheckObj(user, 15);
    }
    
    @Test(expected = ConstraintViolationException.class)
    public void test3() {
        new CheckObj2().check(new User(), 16);
    }
    
    @Test
    public void test4() {
        User user = new User();
        user.username = "username";
        new CheckObj2().check(user, 15);
    }
    
    @Test(expected = ConstraintViolationException.class)
    public void test5() {
        new CheckObj3().check(new User(), 16);
    }

    @Test(expected = ConstraintViolationException.class)
    public void test6() {
        CheckObj4.check(new User(), 16);
    }

    @Test(expected = ConstraintViolationException.class)
    public void test7() {
        Bean bean = new Bean();
        bean.name = "123";
        bean.beans = Arrays.asList(bean);
        bean.v = Arrays.asList("a");
        CheckObj5.check(bean);
    }

    @Test(expected = ConstraintViolationException.class)
    public void test8() {
        CheckObj6.check("");
    }

    @Test
    public void test9() {
        CheckObj7.check("123");
        CheckObj7.check("1234", 1);
    }

    @Test(expected = ConstraintViolationException.class)
    public void test10() {
        new PImpl().a(null);
    }

    static class User {

        @NotEmpty
        private String username;
        
    }
    
    static class CheckObj {
        
        CheckObj(@Valid User user, @Max(15) int len) {
            GlobalValidator.checkConstructorParameters(CheckObj.class, new Class<?>[] {User.class, int.class}, new Object[] {user, len}, new Class<?>[0]);
        }
        
    }
    
    static class CheckObj2 {
        
        public void check(@Valid User user, @Max(15) int len) {
            GlobalValidator.checkMethodParameters(CheckObj2.class, "check", new Class<?>[] {User.class, int.class}, this, new Object[] {user, len}, new Class<?>[0]);
        }
        
    }
    
    interface ICheckObj {
        
        public void check(@Valid User user, @Max(15) int len);
        
    }
    
    static class CheckObj3 implements ICheckObj {

        // 根据hibernate-validator规则，子类不能修改继承方法的约束，可以原样写回去，或者什么都不写
        @Override
        public void check(User user, int len) {
            GlobalValidator.checkMethodParameters(CheckObj3.class, "check", new Class<?>[] {User.class, int.class}, this, new Object[] {user, len}, new Class<?>[0]);
        }
        
    }

    static class CheckObj4 {

        public static void check(@Valid User user, @Max(15) int len) {
            GlobalValidator.checkStaticMethodParameters(CheckObj4.class, "check", new Class<?>[] {User.class, int.class}, new Object[] {user, len}, new Class<?>[0]);
        }

    }

    static class Bean {

        @NotEmpty
        private String name;

        @Valid
        private List<Bean> beans;

        private List<@Pattern(regexp = "[0-9]+")String> v;

    }

    static class CheckObj5 {

        public static void check(@Valid Bean bean) {
            GlobalValidator.checkStaticMethodParameters(CheckObj5.class, "check", new Class<?>[] {Bean.class}, new Object[] {bean}, new Class<?>[0]);
        }

    }

    interface Update {}

    static class CheckObj6 {

        public static void check(@NotEmpty(groups = Update.class) String name) {
            GlobalValidator.checkStaticMethodParameters(CheckObj6.class, "check", new Class<?>[] {String.class}, new Object[] {name}, new Class<?>[] { Update.class });
        }

    }

    static class CheckObj7 {

        public static void check(@NotEmpty String name) {
            GlobalValidator.checkStaticMethodParameters(CheckObj7.class, "check", new Class<?>[] {String.class}, new Object[] {name}, new Class<?>[] { Update.class });
        }

        public static void check(@NotEmpty String name, @Min(0) int age) {
            GlobalValidator.checkStaticMethodParameters(CheckObj7.class, "check", new Class<?>[] {String.class, int.class}, new Object[] {name, age}, new Class<?>[] { Update.class });
        }

    }

    interface P<T> {

        void a(@NotNull T t);

    }

    static class PImpl implements P<String> {

        @Override
        public void a(String s) {
            GlobalValidator.checkMethodParameters(PImpl.class, "a", new Class<?>[] {String.class}, this, new Object[] {s}, new Class<?>[0]);
        }
    }

}
