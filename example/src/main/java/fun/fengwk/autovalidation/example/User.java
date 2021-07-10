package fun.fengwk.autovalidation.example;

import fun.fengwk.autovalidation.annotation.AutoValidation;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

/**
 * 
 * @author fengwk
 */
@AutoValidation
public class User {

    private String username;
    private int age;
    
    public User(@NotEmpty String username, @Min(0) int age) {
        this.username = username;
        this.age = age;
    }
    
    public void setUsername(@NotEmpty String username) {
        this.username = username;
    }
    
    public void setAge(@Min(0) int age) {
        this.age = age;
    }

    public String getUsername() {
        return username;
    }

    public int getAge() {
        return age;
    }

    public static void staticSetUsername(@NotEmpty String username) {

    }

}
