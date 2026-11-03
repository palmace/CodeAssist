package org.gradle.internal.execution.steps;

import org.gradle.cache.Cache;
import org.gradle.internal.Try;
import org.gradle.internal.execution.DeferredExecutionHandler;
import org.gradle.internal.execution.InternalUnitOfWork;
import org.gradle.internal.execution.InternalUnitOfWork.Identity;

public interface DeferredExecutionAwareStep<C extends Context, R extends Result> extends Step<C, R> {
    <T, O> T executeDeferred(InternalUnitOfWork work, C context, Cache<Identity, Try<O>> cache, DeferredExecutionHandler<O, T> handler);
}