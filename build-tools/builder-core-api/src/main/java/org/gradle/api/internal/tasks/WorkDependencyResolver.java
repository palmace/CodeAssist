package org.gradle.api.internal.tasks;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskDependency;

public interface WorkDependencyResolver<T> {
    boolean resolve(Task task, Object node, Action<? super T> resolveAction);

    WorkDependencyResolver<Task> TASK_AS_TASK = new WorkDependencyResolver<Task>() {
        @Override
        public boolean resolve(Task originalTask, Object node, Action<? super Task> resolveAction) {
            if (node instanceof TaskDependency) {
                TaskDependency taskDependency = (TaskDependency) node;
                for (Task dependencyTask : taskDependency.getDependencies(originalTask)) {
                    resolveAction.execute(dependencyTask);
                }
                return true;
            }
            if (node instanceof Task) {
                resolveAction.execute((Task) node);
                return true;
            }
            return false;
        }
    };
}
