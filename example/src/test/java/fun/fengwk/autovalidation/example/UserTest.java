package fun.fengwk.autovalidation.example;

import javax.validation.ConstraintViolationException;

import org.junit.Test;

/**
 * 
 * @author fengwk
 */
public class UserTest {

    @Test(expected = ConstraintViolationException.class)
    public void test() {
        new User("", -1);
    }

    @Test(expected = ConstraintViolationException.class)
    public void test2() {
        User.staticSetUsername("");
    }
    
}
