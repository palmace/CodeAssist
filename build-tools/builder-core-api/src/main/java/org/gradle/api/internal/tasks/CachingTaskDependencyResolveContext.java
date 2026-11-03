package org.gradle.api.internal.tasks;

import org.gradle.api.Task;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;

public class CachingTaskDependencyResolveContext<T> implements TaskDependencyResolveContext {
    private final List<WorkDependencyResolver<T>> resolvers;
    private final Set<T> values = new HashSet<>();
    private Task task;

    @SafeVarargs
    public CachingTaskDependencyResolveContext(WorkDependencyResolver<T>... resolvers) {
        this.resolvers = Arrays.asList(resolvers);
    }

    public Set<T> getDependencies(@Nullable Task task, Object dependencies) {
        this.task = task;
        values.clear();
        add(dependencies);
        return values;
    }

    @Override
    public void add(Object dependency) {
        if (dependency instanceof TaskDependencyContainer) {
            ((TaskDependencyContainer) dependency).visitDependencies(this);
        } else {
            for (WorkDependencyResolver<T> resolver : resolvers) {
                if (resolver.resolve(task, dependency, values::add)) {
                    return;
                }
            }
        }
    }

    @Override
    public void visitFailure(Throwable failure) {
    }

    @Override
    @Nullable
    public Task getTask() {
        return task;
    }
}
