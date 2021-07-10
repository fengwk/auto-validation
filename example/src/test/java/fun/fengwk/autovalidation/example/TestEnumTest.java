package fun.fengwk.autovalidation.example;

import org.junit.Test;

import javax.validation.ConstraintViolationException;

/**
 * @author fengwk
 */
public class TestEnumTest {

    @Test
    public void test1() {
        TestEnum.testStatic(null);
    }

    @Test(expected = ConstraintViolationException.class)
    public void test2() {
        TestEnum.E.test(null);
    }

}
