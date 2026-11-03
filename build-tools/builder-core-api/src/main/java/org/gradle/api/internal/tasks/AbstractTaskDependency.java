package org.gradle.api.internal.tasks;

import org.gradle.api.Task;
import javax.annotation.Nullable;
import java.util.Set;

public abstract class AbstractTaskDependency implements TaskDependencyInternal {
    @Override
    public Set<? extends Task> getDependencies(@Nullable Task task) {
        CachingTaskDependencyResolveContext<Task> context = new CachingTaskDependencyResolveContext<>(
                WorkDependencyResolver.TASK_AS_TASK
        );
        return context.getDependencies(task, this);
    }
}
