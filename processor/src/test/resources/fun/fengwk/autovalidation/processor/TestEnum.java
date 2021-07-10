package fun.fengwk.autovalidation.processor;

import fun.fengwk.autovalidation.annotation.AutoValidation;

import javax.validation.constraints.NotNull;

/**
 * @author fengwk
 */
@AutoValidation
public enum TestEnum {

    E(1);

    private final int code;

    TestEnum(int code) {
        this.code = code;
    }

    @AutoValidation(groups = Group.class)
    public static void testStatic(@NotNull Integer code) {}

    public void test(@NotNull Integer code) {}

}
