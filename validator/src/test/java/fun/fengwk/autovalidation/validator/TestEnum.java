package fun.fengwk.autovalidation.validator;

import java.util.Arrays;

/**
 * @author fengwk
 */
public enum TestEnum {

    A, B, C;


    public static void main(String[] args) {
        System.out.println(Arrays.toString(TestEnum.class.getDeclaredFields()));


    }

}
