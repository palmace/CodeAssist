package org.gradle.api.internal.tasks;

/**
 * An object that has task dependencies associated with it.
 */
public interface TaskDependencyContainer {
    TaskDependencyContainer EMPTY = context -> {
    };

    /**
     * Adds the dependencies from this container to the given context.
     */
    void visitDependencies(TaskDependencyResolveContext context);
}
