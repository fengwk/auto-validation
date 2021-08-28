package fun.fengwk.autovalidation.validator;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author fengwk
 */
public class JavassistSupportTest {

    private void write(CtClass cc, String dir) throws IOException, CannotCompileException {
        byte[] bytes = cc.toBytecode();
        try (FileOutputStream fos = new FileOutputStream(new File(dir + cc.getSimpleName() + ".class"))) {
            fos.write(bytes);
        }
    }

    @SuppressWarnings("unused")
    private void testMethod(@TestAnnotation String a, @TestAnnotation("1") String b) {}

    @Test
    public void test2() throws NotFoundException, NoSuchMethodException {
        Method rawMethod = JavassistSupportTest.class.getDeclaredMethod("testMethod", String.class, String.class);
        List<String> parameterNames = JavassistSupport.getParameterNames(rawMethod);
        System.out.println(parameterNames);
    }

    @Test
    public void test() throws NoSuchMethodException, NotFoundException, CannotCompileException,
            InvocationTargetException, IllegalAccessException, IOException, BadBytecode {
        Method rawMethod = JavassistSupportTest.class.getDeclaredMethod("testMethod", String.class, String.class);
        CtClass cc = JavassistSupport.genValidationCtClass(rawMethod);
        write(cc, "d:/");
    }

}
