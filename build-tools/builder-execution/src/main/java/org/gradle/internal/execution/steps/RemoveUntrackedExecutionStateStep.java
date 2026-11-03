package org.gradle.internal.execution.steps;

import org.gradle.internal.execution.InternalUnitOfWork;

public class RemoveUntrackedExecutionStateStep<C extends WorkspaceContext, R extends AfterExecutionResult> implements Step<C, R> {
    private final Step<? super C, ? extends R> delegate;

    public RemoveUntrackedExecutionStateStep(
            Step<? super C, ? extends R> delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    public R execute(InternalUnitOfWork work, C context) {
        R result = delegate.execute(work, context);
        context.getHistory()
                .ifPresent(history -> {
                    if (!result.getAfterExecutionState().isPresent()) {
                        history.remove(context.getIdentity().getUniqueId());
                    }
                });
        return result;
    }
}
