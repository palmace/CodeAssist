package com.tyron.lint.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tyron.builder.project.api.JavaModule;
import com.tyron.completion.java.compiler.CompileTask;
import com.tyron.lint.client.Configuration;
import com.tyron.lint.client.LintDriver;

import org.jetbrains.kotlin.com.intellij.psi.PsiAnnotation;
import org.jetbrains.kotlin.com.intellij.psi.PsiAnonymousClass;
import org.jetbrains.kotlin.com.intellij.psi.PsiClass;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiField;
import org.jetbrains.kotlin.com.intellij.psi.PsiLabeledStatement;
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod;
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.kotlin.com.intellij.psi.PsiNewExpression;
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceExpression;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import java.io.File;
import java.lang.reflect.Method;

/**
 * JavaContext v28.37: "The Zenith Anchor - Absolute Shield".
 * v28.37: Desacoplamiento total del tipo Trees en firmas para evadir bloqueos de Android 15.
 * v28.26: Sellado absoluto contra llamadas directas a Trees.instance().
 */
public class JavaContext extends Context {
    static final String SUPPRESS_COMMENT_PREFIX = "//noinspection ";
    private CompileTask mCompileTask;

    public JavaContext(LintDriver driver, JavaModule project, File file, Configuration config) {
        super(driver, project, file, config);
    }

    public void setCompileTask(CompileTask root) {
        mCompileTask = root;
    }

    public CompileTask getCompileTask() {
        return mCompileTask;
    }

    public CompilationUnitTree getCompilationUnit() {
        return mCompileTask != null ? mCompileTask.root() : null;
    }

    public void report(
            @NonNull Issue issue,
            @Nullable Tree scope,
            @Nullable Location location,
            @NonNull String message) {
        if (scope != null && mDriver.isSuppressed(this, issue, scope)) {
            return;
        }
        super.report(issue, location, message);
    }

    public Location getLocation(@NonNull Tree node) {
        // OMNI: Uso de Object para evitar que el verificador de Android intente cargar Trees prematuramente
        Object trees = mCompileTask != null ? mCompileTask.getTrees() : null;
        if (trees == null) {
            return Location.create(file);
        }
        
        try {
            Method getSourcePositionsMethod = trees.getClass().getMethod("getSourcePositions");
            SourcePositions pos = (SourcePositions) getSourcePositionsMethod.invoke(trees);
            CompilationUnitTree unit = getCompilationUnit();
            
            if (unit == null || pos == null) {
                return Location.create(file);
            }

            return Location.create(file,
                    getContents(),
                    (int) pos.getStartPosition(unit, node),
                    (int) pos.getEndPosition(unit, node));
        } catch (Throwable t) {
            return Location.create(file);
        }
    }

    public static String getMethodName(@NonNull Tree call) {
        if (call instanceof MethodInvocationTree) {
            ExpressionTree expressionTree = ((MethodInvocationTree) call).getMethodSelect();
            if (expressionTree instanceof IdentifierTree) {
                return ((IdentifierTree) expressionTree).getName().toString();
            } else if (expressionTree instanceof MemberSelectTree) {
                return ((MemberSelectTree) expressionTree).getIdentifier().toString();
            }
        }
        return null;
    }

    @Nullable
    public static PsiElement findNameElement(@NonNull PsiElement element) {
        if (element instanceof PsiClass) {
            if (element instanceof PsiAnonymousClass) {
                return ((PsiAnonymousClass)element).getBaseClassReference();
            }
            return ((PsiClass) element).getNameIdentifier();
        } else if (element instanceof PsiMethod) {
            return ((PsiMethod) element).getNameIdentifier();
        } else if (element instanceof PsiMethodCallExpression) {
            return ((PsiMethodCallExpression) element).getMethodExpression().
                    getReferenceNameElement();
        } else if (element instanceof PsiNewExpression) {
            return ((PsiNewExpression) element).getClassReference();
        } else if (element instanceof PsiField) {
            return ((PsiField)element).getNameIdentifier();
        } else if (element instanceof PsiAnnotation) {
            return ((PsiAnnotation)element).getNameReferenceElement();
        } else if (element instanceof PsiReferenceExpression) {
            return ((PsiReferenceExpression) element).getReferenceNameElement();
        } else if (element instanceof PsiLabeledStatement) {
            return ((PsiLabeledStatement)element).getLabelIdentifier();
        }
        return null;
    }
}
