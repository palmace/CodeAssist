package com.tyron.completion.java.patterns.elements;

import com.tyron.completion.java.compiler.ReflectionUtil;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.patterns.ElementPattern;
import org.jetbrains.kotlin.com.intellij.patterns.PatternConditionPlus;
import org.jetbrains.kotlin.com.intellij.util.PairProcessor;
import org.jetbrains.kotlin.com.intellij.util.ProcessingContext;
import javax.lang.model.element.Element;

public abstract class JavacElementPatternConditionPlus<Target, Value> extends PatternConditionPlus<Target, Value> implements JavacElementPattern {

    public JavacElementPatternConditionPlus(@NonNls String methodName, ElementPattern valuePattern) {
        super(methodName, valuePattern);
    }


    @Override
    public boolean accepts(@Nullable Object o, ProcessingContext context) {
        if (o instanceof Element) {
            return accepts((Element) o, context);
        }
        return processValues((Target) o, context, (value, context1) -> getValuePattern().accepts(value, context1));
    }

    @Override
    public boolean accepts(Element element, ProcessingContext context) {
        return processValues(element, context, (value, context1) -> getValuePattern().accepts(value, context1));
    }

    public abstract boolean processValues(Element target, ProcessingContext context,
                                          PairProcessor<? super Element, ? super ProcessingContext> processor);

    @Override
    public abstract boolean processValues(Target target, ProcessingContext context,
                                          PairProcessor<? super Value, ? super ProcessingContext> processor);


}
