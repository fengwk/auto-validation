package fun.fengwk.autovalidation.processor;

import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

/**
 * 
 * @author fengwk
 */
public class AutoMapperProcessorTest {

    @Test
    public void test() {
        Compiler
                .javac()
                .withProcessors(new AutoValidationProcessor())
                .compile(
                        JavaFileObjects.forResource("fun/fengwk/autovalidation/processor/Group.java"),
                        JavaFileObjects.forResource("fun/fengwk/autovalidation/processor/TestEnum.java")
                );
    }

}
