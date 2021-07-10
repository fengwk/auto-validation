package fun.fengwk.autovalidation.processor;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import fun.fengwk.autovalidation.annotation.AutoValidation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * 参考资料：
 * <ul>
 * <li><a href="http://hannesdorfmann.com/annotation-processing/annotationprocessing101/">Annotation Processing</a></li>
 * <li><a href="https://blog.csdn.net/dap769815768/article/details/90448451">java使用AbstractProcessor、编译时注解和JCTree实现自动修改class文件并实现Debug自己的Processor和编译后的代码</a></li>
 * <li><a href="https://blog.csdn.net/A_zhenzhen/article/details/86065063">java注解处理器——在编译期修改语法树</a></li>
 * </ul>
 *
 * @author fengwk
 */
public class AutoValidationProcessor extends AbstractProcessor {

    private Types types;
    private JavacElements elements;
    private Messager messager;
    
    private TreeMaker treeMaker;
    private Names names;

    private Set<ExecutableElement> processedSet = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.types = processingEnv.getTypeUtils();
        this.elements = (JavacElements) processingEnv.getElementUtils();
        this.messager = processingEnv.getMessager();

        // 某些编译器实现会封装JavacProcessingEnvironment，尝试找到被封装的对象
        JavacProcessingEnvironment javacProcessingEnvironment = searchJavacProcessingEnvironment(processingEnv);
        // 如果没有找到JavacProcessingEnvironment则无法支持抽象语法树的修改
        if (javacProcessingEnvironment == null) {
            throw new IllegalStateException(String.format("Auto-Validation not supported current compiling environment '%s'", processingEnv));
        }
        Context context = javacProcessingEnvironment.getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    private JavacProcessingEnvironment searchJavacProcessingEnvironment(Object obj) {
        LinkedList<Object> queue = new LinkedList<>();
        Set<Object> visited = new HashSet<>();
        queue.offer(obj);
        visited.add(obj);
        while (!queue.isEmpty()) {
            Object cur = queue.poll();

            if (cur instanceof JavacProcessingEnvironment) {
                return (JavacProcessingEnvironment) cur;
            }

            for (Object next : listMemberObjects(cur)) {
                if (next != null && !visited.contains(next)) {
                    queue.offer(next);
                    visited.add(next);
                }
            }
        }

        return null;
    }

    private java.util.List<Object> listMemberObjects(Object obj) {
        Class<?> clazz = obj.getClass();
        java.util.List<Object> memberObjects = new ArrayList<>();
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                try {
                    memberObjects.add(f.get(obj));
                } catch (IllegalAccessException ignore) {}
            }
            clazz = clazz.getSuperclass();
        }

        return memberObjects;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotationTypes = new HashSet<>();
        supportedAnnotationTypes.add(AutoValidation.class.getCanonicalName());
        return supportedAnnotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(AutoValidation.class)) {
            try {
                ElementKind kind = annotatedElement.getKind();
                if ((kind == ElementKind.METHOD || kind == ElementKind.CONSTRUCTOR)
                        && (annotatedElement.getEnclosingElement().getKind() == ElementKind.CLASS || annotatedElement.getEnclosingElement().getKind() == ElementKind.ENUM)) {
                    processForMethodOrConstructor((ExecutableElement) annotatedElement);
                } else if (kind == ElementKind.CLASS || kind == ElementKind.ENUM) {
                    processForClass((TypeElement) annotatedElement);
                } else {
                    error(annotatedElement, "Only class(or enum) or class(or enum)'s method or class(or enum)'s constructor can be annotated with @%s, but found on %s",
                            AutoValidation.class.getSimpleName(), annotatedElement.getSimpleName());
                    break;
                }
            } catch (Throwable e) {
                error(annotatedElement, e.toString());
            }
        }

        return true;
    }

    private void processForClass(TypeElement classElement) {
        for (Element element : classElement.getEnclosedElements()) {
            if (element instanceof ExecutableElement) {
                processForMethodOrConstructor((ExecutableElement) element);
            }
        }
    }

    private void processForMethodOrConstructor(ExecutableElement executableElement) {
        if (!processedSet.contains(executableElement)) {
            processedSet.add(executableElement);
            if (containsValidationAnnotation(executableElement)) {
                Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) executableElement.getEnclosingElement();
                doProcessForMethodOrConstructor(classSymbol, executableElement, executableElement.getKind() == ElementKind.CONSTRUCTOR);
            }
        }
    }

    private boolean containsValidationAnnotation(ExecutableElement executableElement) {
        for (VariableElement variableElement : executableElement.getParameters()) {
            for (AnnotationMirror annotationMirror : variableElement.getAnnotationMirrors()) {
                Element annotationElement = annotationMirror.getAnnotationType().asElement();
                if (annotationElement instanceof TypeElement && ((TypeElement) annotationElement).getQualifiedName().toString().equals(javax.validation.Valid.class.getName())) {
                    return true;
                } else {
                    for (AnnotationMirror annotationAnnotationMirror : annotationElement.getAnnotationMirrors()) {
                        Element annotationAnnotationElement = annotationAnnotationMirror.getAnnotationType().asElement();
                        if (annotationAnnotationElement instanceof TypeElement
                                && ((TypeElement) annotationAnnotationElement).getQualifiedName().toString().equals(javax.validation.Constraint.class.getName())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void doProcessForMethodOrConstructor(Symbol.ClassSymbol classSymbol, ExecutableElement executableElement, boolean isConstructor) {
        AutoValidation autoValidation = executableElement.getAnnotation(AutoValidation.class);
        if (autoValidation == null) {
            autoValidation = classSymbol.getAnnotation(AutoValidation.class);
        }

        JCTree.JCExpression[] groups = getGroups(autoValidation);

        JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) elements.getTree(executableElement);
        List<JCTree.JCVariableDecl> parameters = jcMethodDecl.getParameters();

        JCTree.JCExpression[] pClasses = new JCTree.JCExpression[parameters.size()];
        JCTree.JCIdent[] pNames = new JCTree.JCIdent[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            JCTree.JCVariableDecl parameter = parameters.get(i);
            pClasses[i] = treeMaker.ClassLiteral(parameter.getType().getTree().type);
            pNames[i] = treeMaker.Ident(parameter.sym);
        }

        treeMaker.pos = jcMethodDecl.pos;
        JCTree.JCMethodInvocation validateParameters;

        if (isConstructor) {
            validateParameters = treeMaker.Apply(
                    List.nil(),
                    memberAccess( fun.fengwk.autovalidation.validation.GlobalValidator.class.getName() + ".checkConstructorParameters"),
                    List.of(
                            treeMaker.ClassLiteral(classSymbol),
                            treeMaker.NewArray(memberAccess("Class"), List.nil(), List.from(pClasses)),
                            treeMaker.NewArray(memberAccess("Object"), List.nil(), List.from(pNames)),
                            treeMaker.NewArray(memberAccess("Class"), List.nil(), List.from(groups))
                            )
                    );
        } else if (!executableElement.getModifiers().contains(Modifier.STATIC)) {
            validateParameters = treeMaker.Apply(
                    List.nil(),
                    memberAccess(fun.fengwk.autovalidation.validation.GlobalValidator.class.getName() + ".checkMethodParameters"),
                    List.of(
                            treeMaker.ClassLiteral(classSymbol),
                            treeMaker.Literal(executableElement.getSimpleName().toString()),
                            treeMaker.NewArray(memberAccess("Class"), List.nil(), List.from(pClasses)),
                            memberAccess("this"),
                            treeMaker.NewArray(memberAccess("Object"), List.nil(), List.from(pNames)),
                            treeMaker.NewArray(memberAccess("Class"), List.nil(), List.from(groups))
                            )
                    );
        } else {
            validateParameters = treeMaker.Apply(
                    List.nil(),
                    memberAccess(fun.fengwk.autovalidation.validation.GlobalValidator.class.getName() + ".checkStaticMethodParameters"),
                    List.of(
                            treeMaker.ClassLiteral(classSymbol),
                            treeMaker.Literal(executableElement.getSimpleName().toString()),
                            treeMaker.NewArray(memberAccess("Class"), List.nil(), List.from(pClasses)),
                            treeMaker.NewArray(memberAccess("Object"), List.nil(), List.from(pNames)),
                            treeMaker.NewArray(memberAccess("Class"), List.nil(), List.from(groups))
                    )
            );

        }

        jcMethodDecl.body = treeMaker.Block(0, List.of(treeMaker.Exec(validateParameters), jcMethodDecl.body));

        log("Successfully Generate validation code for %s.%s", classSymbol.getSimpleName(), executableElement.getSimpleName());
    }

    private JCTree.JCExpression[] getGroups(AutoValidation autoValidation) {
        JCTree.JCExpression[] groups;
        try {
            Class<?>[] groupClasses = autoValidation.groups();
            groups = new JCTree.JCExpression[groupClasses.length];
            for (int i = 0; i < groupClasses.length; i++) {
                groups[i] = memberAccess(groupClasses[i].getCanonicalName() + ".class");
            }
        } catch (MirroredTypesException e) {
            java.util.List<? extends TypeMirror> typeMirrors = e.getTypeMirrors();
            groups = new JCTree.JCExpression[typeMirrors.size()];
            for (int i = 0; i < typeMirrors.size(); i++) {
                groups[i] = memberAccess(((TypeElement) types.asElement(typeMirrors.get(i))).getQualifiedName().toString() + ".class");
            }
        }
        return groups;
    }

    private Name getNameFromString(String s) { return names.fromString(s); }

    private JCTree.JCExpression memberAccess(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(getNameFromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, getNameFromString(componentArray[i]));
        }
        return expr;
    }

    private void log(String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(String.valueOf(msg), args));
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(String.valueOf(msg), args), e);
    }

}
