package fun.fengwk.autovalidation.example;

import fun.fengwk.autovalidation.annotation.AutoValidation;

import javax.validation.Valid;
import java.util.Date;

/**
 * 
 * @author fengwk
 */
@AutoValidation
public class UserManager {

    public void save(@Valid User user, Date date) {}
    
}
