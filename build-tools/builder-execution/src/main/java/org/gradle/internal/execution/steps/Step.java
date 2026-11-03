package org.gradle.internal.execution.steps;

import org.gradle.internal.execution.InternalUnitOfWork;

public interface Step<C extends Context, R extends Result> {
    R execute(InternalUnitOfWork work, C context);
}