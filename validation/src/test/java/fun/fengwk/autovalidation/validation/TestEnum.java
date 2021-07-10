package fun.fengwk.autovalidation.validation;

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
