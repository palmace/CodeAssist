package com.tyron.completion.java.patterns;

import com.tyron.completion.java.compiler.ReflectionUtil;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.patterns.ElementPattern;
import org.jetbrains.kotlin.com.intellij.patterns.InitialPatternCondition;
import org.jetbrains.kotlin.com.intellij.patterns.PatternCondition;
import org.jetbrains.kotlin.com.intellij.util.ProcessingContext;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

public class ExpressionTreePattern<T extends ExpressionTree, Self extends ExpressionTreePattern<T, Self>> extends JavacTreePattern<T, Self> {
    protected ExpressionTreePattern(@NonNull InitialPatternCondition<T> condition) {
        super(condition);
    }

    protected ExpressionTreePattern(Class<T> aClass) {
        super(aClass);
    }

    public MethodInvocationTreePattern methodCall(final ElementPattern<? extends MethodInvocationTree> method) {
        JavacTreeNamePatternCondition nameCondition = null;
        for (PatternCondition<?> condition : method.getCondition().getConditions()) {
            if (condition instanceof JavacTreeNamePatternCondition) {
                nameCondition = (JavacTreeNamePatternCondition) condition;
                break;
            }
        }

        return new MethodInvocationTreePattern().and(this).with(new PatternCondition<MethodInvocationTree>("methodCall") {
            @Override
            public boolean accepts(@NotNull MethodInvocationTree t, ProcessingContext processingContext) {
                return false;
            }
        });
    }

    public static class Capture<T extends ExpressionTree> extends ExpressionTreePattern<T, Capture<T>> {

        protected Capture(@NonNull InitialPatternCondition<T> condition) {
            super(condition);
        }

        protected Capture(Class<T> aClass) {
            super(aClass);
        }
    }
}
