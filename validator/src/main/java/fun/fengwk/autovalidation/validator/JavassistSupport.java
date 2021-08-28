package fun.fengwk.autovalidation.validator;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengwk
 */
public class JavassistSupport {

    private static final ConcurrentMap<String, AtomicInteger> CLASS_NAME_GEN = new ConcurrentHashMap<>();

    private JavassistSupport() {}

    public static MemberValue createMemberValue(ConstPool constPool, CtClass type, Object value) throws NotFoundException {
        if (type == CtClass.booleanType) {
            return new BooleanMemberValue((boolean) value, constPool);
        }
        else if (type == CtClass.byteType) {
            return new ByteMemberValue((byte) value, constPool);
        }
        else if (type == CtClass.charType) {
            return new CharMemberValue((char) value, constPool);
        }
        else if (type == CtClass.shortType) {
            return new ShortMemberValue((short) value, constPool);
        }
        else if (type == CtClass.intType) {
            return new IntegerMemberValue(constPool, (int) value);
        }
        else if (type == CtClass.longType) {
            return new LongMemberValue((long) value, constPool);
        }
        else if (type == CtClass.floatType) {
            return new FloatMemberValue((float) value, constPool);
        }
        else if (type == CtClass.doubleType) {
            return new DoubleMemberValue((double) value, constPool);
        }
        else if (type.getName().equals("java.lang.Class")) {
            return new ClassMemberValue(((Class<?>) value).getName(), constPool);
        }
        else if (type.getName().equals("java.lang.String")) {
            return new StringMemberValue((String) value, constPool);
        }
        else if (type.isArray()) {
            ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constPool);
            CtClass arrayType = type.getComponentType();
            int len = Array.getLength(value);
            MemberValue[] members = new MemberValue[len];
            for (int i = 0; i < len; i++) {
                Object item = Array.get(value, i);
                members[i] = createMemberValue(constPool, arrayType, item);
            }
            arrayMemberValue.setValue(members);
            return arrayMemberValue;
        }
        else if (type.isInterface()) {
            Annotation info = new Annotation(constPool, type);
            return new AnnotationMemberValue(info, constPool);
        }
        else {
            // treat as enum.  I know this is not typed,
            // but JBoss has an Annotation Compiler for JDK 1.4
            // and I want it to work with that. - Bill Burke
            EnumMemberValue emv = new EnumMemberValue(constPool);
            emv.setType(type.getName());
            emv.setValue(((Enum<?>) value).name());
            return emv;
        }
    }

    public static Annotation fromRawAnnotation(ClassPool pool, ConstPool constPool, java.lang.annotation.Annotation rawAnn)
            throws InvocationTargetException, IllegalAccessException, NotFoundException {
        Class<? extends java.lang.annotation.Annotation> rawAnnType = rawAnn.annotationType();
        Annotation ann = new Annotation(rawAnnType.getName(), constPool);
        for (Method rawAnnMethod : rawAnnType.getDeclaredMethods()) {
            if (!rawAnnMethod.isAccessible()) {
                rawAnnMethod.setAccessible(true);
            }

            CtClass type = pool.get(rawAnnMethod.getReturnType().getName());
            Object value = rawAnnMethod.invoke(rawAnn);
            ann.addMemberValue(rawAnnMethod.getName(),
                    createMemberValue(constPool, type, value));
        }

        return ann;
    }

    public static List<String> getParameterNames(Method rawMethod) throws NotFoundException {
        List<String> paramNames = new ArrayList<>();
        ClassPool pool = ClassPool.getDefault();
        CtClass declaring = pool.getCtClass(rawMethod.getDeclaringClass().getName());
        CtMethod method = declaring.getDeclaredMethod(rawMethod.getName());
        // 使用javassist的反射方法的参数名
        MethodInfo methodInfo = method.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
        if (attr != null) {
            int len = method.getParameterTypes().length;
            // 非静态的成员函数的第一个参数是this
            int pos = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
            for (int i = 0; i < len; i++) {
                String paramName = attr.variableName(i + pos);
                paramNames.add(paramName);
            }
        }
        return paramNames;
    }

    /**
     * 利用原始方法生成校验类。
     *
     * @param rawMethod
     * @return
     * @throws NotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws CannotCompileException
     */
    public static CtClass genValidationCtClass(Method rawMethod) throws NotFoundException, InvocationTargetException,
            IllegalAccessException, CannotCompileException, BadBytecode {
        ClassPool pool = ClassPool.getDefault();

        Class<?>[] rawParameterTypes = rawMethod.getParameterTypes();
        CtClass[] parameterTypes = new CtClass[rawParameterTypes.length];
        for (int i = 0; i < rawParameterTypes.length; i++) {
            parameterTypes[i] = pool.get(rawParameterTypes[i].getName());
        }

        Class<?> rawDeclaring = rawMethod.getDeclaringClass();
        CtClass declaring = pool.makeClass(genClassName(pool, rawDeclaring.getName()));

        ClassFile classFile = declaring.getClassFile();
        ConstPool constPool = classFile.getConstPool();

        CtMethod method = new CtMethod(CtClass.voidType, rawMethod.getName(), parameterTypes, declaring);
        method.setModifiers(Modifier.PUBLIC);
        method.setBody("{}");

        java.lang.annotation.Annotation[][] rawParameterAnnotations = rawMethod.getParameterAnnotations();
        Annotation[][] parameterAnnotations = new Annotation[rawParameterAnnotations.length][];
        for (int i = 0; i < rawParameterAnnotations.length; i++) {
            java.lang.annotation.Annotation[] parameterAnnotation = rawParameterAnnotations[i];
            parameterAnnotations[i] = new Annotation[parameterAnnotation.length];
            for (int j = 0; j < parameterAnnotation.length; j++) {
                parameterAnnotations[i][j] = fromRawAnnotation(pool, constPool, rawParameterAnnotations[i][j]);
            }
        }

        ParameterAnnotationsAttribute parameterAnnAttrs = new ParameterAnnotationsAttribute(
                constPool, ParameterAnnotationsAttribute.visibleTag);
        parameterAnnAttrs.setAnnotations(parameterAnnotations);

        method.getMethodInfo().addAttribute(parameterAnnAttrs);

        declaring.addMethod(method);

        return declaring;
    }

    public static Class<?> genValidationClass(Method rawMethod) {
        try {
            CtClass cc = genValidationCtClass(rawMethod);
            return cc.toClass();
        } catch (NotFoundException | InvocationTargetException | IllegalAccessException | CannotCompileException | BadBytecode e) {
            throw new IllegalStateException(e);
        }
    }

    private static String genClassName(ClassPool pool, String className) {
        AtomicInteger counter = CLASS_NAME_GEN.computeIfAbsent(className, k -> new AtomicInteger());
        className += "$ValidationProxy";

        int c;
        String retClassName;
        while (pool.getOrNull(retClassName = (className + "$" + (c = counter.get()))) != null
                || !counter.compareAndSet(c, c+1)) {}

        return retClassName;
    }

}
