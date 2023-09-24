package fun.fengwk.autovalidation.validator;

import javax.validation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * @author fengwk
 */
public class GlobalValidator {
    
    private static volatile Validator instance;

    private static final ConcurrentMap<Method, ValidationProxy> PROXY_CACHE = new ConcurrentHashMap<>();

    static {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        instance = validatorFactory.getValidator();
        validatorFactory.close();
    }
    
    private GlobalValidator() {}
    
    public static Validator getInstance() {
        return instance;
    }
    
    public static void setInstance(Validator newInstance) {
        instance = newInstance;
    }
    
    public static <T> void checkConstructorParameters(Class<T> clazz, Class<?>[] parameterTypes, Object[] parameterValues, Class<?>[] groups) {
        Constructor<T> constructor;
        try {
            constructor = clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
        
        Set<ConstraintViolation<T>> cvSet = getInstance().forExecutables().validateConstructorParameters(constructor, parameterValues, groups);
        if (!cvSet.isEmpty()) {
            throw new ConstraintViolationException(cvSet);
        }
    }
    
    public static <T> void checkMethodParameters(Class<T> clazz, String methodName, Class<?>[] parameterTypes, T object, Object[] parameterValues, Class<?>... groups) {
        Method method;
        try {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
        
        Set<ConstraintViolation<T>> cvSet = getInstance().forExecutables().validateParameters(object, method, parameterValues, groups);
        if (!cvSet.isEmpty()) {
            throw new ConstraintViolationException(cvSet);
        }
    }

    public static <T> void checkStaticMethodParameters(Class<T> clazz, String methodName, Class<?>[] parameterTypes, Object[] parameterValues, Class<?>... groups) {
        Method method;
        try {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }

        ValidationProxy proxy = PROXY_CACHE.computeIfAbsent(method, ValidationProxy::new);
        proxy.check(parameterValues, groups);
    }

    static class ValidationProxy {

        final Method method;
        final Object instance;

        public ValidationProxy(Method rawMethod) {
            Class<?> validationClass = JavassistSupport.genValidationClass(rawMethod);
            try {
                this.method = validationClass.getDeclaredMethod(rawMethod.getName(), rawMethod.getParameterTypes());
                this.instance = validationClass.newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public void check(Object[] parameterValues, Class<?>... groups) {
            Set<ConstraintViolation<Object>> cvSet = getInstance().forExecutables().validateParameters(
                    instance, method, parameterValues, groups);
            if (!cvSet.isEmpty()) {
                throw new ConstraintViolationException(cvSet);
            }
        }

    }

}
