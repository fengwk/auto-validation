package fun.fengwk.autovalidation.processor;

import javax.validation.constraints.NotEmpty;

import fun.fengwk.autovalidation.annotation.AutoValidation;

/**
 * 
 * @author fengwk
 */
@AutoValidation
public class User {

    private String username;
    
    @AutoValidation
    public User(@NotEmpty String[] username) {

    }
    
    @AutoValidation
    public void setUsername(@NotEmpty String username) {
        this.username = username;
    }

    public void setUsername2(@NotEmpty String username) {
        this.username = username;
    }

    public void setUsername3(String username) {
        this.username = username;
    }
    
}
