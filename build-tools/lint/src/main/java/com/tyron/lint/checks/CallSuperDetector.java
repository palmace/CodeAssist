package com.tyron.lint.checks;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tyron.lint.api.Category;
import com.tyron.lint.api.Detector;
import com.tyron.lint.api.Implementation;
import com.tyron.lint.api.Issue;
import com.tyron.lint.api.JavaContext;
import com.tyron.lint.api.JavaVoidVisitor;
import com.tyron.lint.api.Scope;
import com.tyron.lint.api.Severity;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import java.util.Collections;
import java.util.List;
import java.lang.reflect.Method;

/**
 * CallSuperDetector v28.36: "The Zenith Super Anchor - Deep Signature".
 * v28.34: Implementación de Elements.overrides para validación de firmas.
 * v28.35: Refinamiento de reporte de ubicación y compatibilidad con AndroidX dinámico.
 * v28.36: FIX - Uso de getLocation() compatible con la API de JavaContext actual.
 */
public class CallSuperDetector extends Detector implements Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            CallSuperDetector.class,
            Scope.JAVA_FILE_SCOPE);

    public static final Issue ISSUE = Issue.create(
            "MissingSuperCall", 
            "Missing Super Call",
            "Some methods require that you also call the super implementation.",
            Category.CORRECTNESS,
            9,
            Severity.ERROR,
            IMPLEMENTATION);

    public CallSuperDetector() {
    }

    @Override
    public List<Class<? extends Tree>> getApplicableTypes() {
        return Collections.singletonList(MethodTree.class);
    }

    @Override
    public JavaVoidVisitor getVisitor(JavaContext context) {
        return new JavaVoidVisitor() {
            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                checkCallSuper(context, node);
                return null;
            }
        };
    }

    private static void checkCallSuper(@NonNull JavaContext context, @NonNull MethodTree declaration) {
        ExecutableElement superMethod = getRequiredSuperMethod(context, declaration);
        if (superMethod != null) {
            if (!SuperCallVisitor.callsSuper(declaration, superMethod)) {
                String methodName = declaration.getName().toString();
                // v28.36: Se usa getLocation() ya que getNameLocation() no está definido en JavaContext.java
                context.report(ISSUE, declaration, context.getLocation(declaration), 
                        "Overriding method should call `super." + methodName + "`");
            }
        }
    }

    @Nullable
    private static ExecutableElement getRequiredSuperMethod(@NonNull JavaContext context, @NonNull MethodTree node) {
        if (context.getCompileTask() == null) return null;
        Object trees = context.getCompileTask().getTrees();
        if (trees == null) return null;
        
        try {
            TreePath path = TreePath.getPath(context.getCompilationUnit(), node);
            if (path == null) return null;

            Method getElementMethod = trees.getClass().getMethod("getElement", TreePath.class);
            Element method = (Element) getElementMethod.invoke(trees, path);
            if (!(method instanceof ExecutableElement executableMethod)) return null;
            
            Method getScopeMethod = trees.getClass().getMethod("getScope", TreePath.class);
            Object scope = getScopeMethod.invoke(trees, path);
            if (scope == null) return null;
            
            Method getEnclosingClassMethod = scope.getClass().getMethod("getEnclosingClass");
            Object typeElementObj = getEnclosingClassMethod.invoke(scope);
            
            if (!(typeElementObj instanceof TypeElement typeElement)) return null;
            if (!(typeElement.getSuperclass() instanceof DeclaredType superClass)) return null;
            
            Element superElementBase = superClass.asElement();
            if (!(superElementBase instanceof TypeElement superElement)) return null;

            List<? extends Element> elements = context.getCompileTask().task.getElements().getAllMembers(superElement);
            for (Element element : elements) {
                if (element instanceof ExecutableElement superMethod && 
                    superMethod.getAnnotation(CallSuper.class) != null) {
                    
                    if (context.getCompileTask().task.getElements().overrides(executableMethod, superMethod, typeElement)) {
                        return superMethod;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static class SuperCallVisitor extends JavaVoidVisitor {
        private final ExecutableElement mMethod;
        private boolean mCallsSuper;

        public static boolean callsSuper(@NonNull MethodTree methodDeclaration, @NonNull ExecutableElement method) {
            SuperCallVisitor visitor = new SuperCallVisitor(method);
            methodDeclaration.accept(visitor, null);
            return visitor.mCallsSuper;
        }

        private SuperCallVisitor(@NonNull ExecutableElement method) {
            mMethod = method;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            ExpressionTree expressionTree = node.getMethodSelect();
            if (expressionTree instanceof MemberSelectTree select) {
                if (select.getExpression().toString().equals("super") &&
                    mMethod.getSimpleName().contentEquals(select.getIdentifier().toString())) {
                    mCallsSuper = true;
                }
            }
            return super.visitMethodInvocation(node, unused);
        }
    }
}
