package org.gradle.api.internal.tasks;

import org.gradle.api.Task;
import org.gradle.api.tasks.TaskDependency;

import java.util.Collections;
import java.util.Set;

public interface TaskDependencyInternal extends TaskDependency, TaskDependencyContainer {

    TaskDependencyInternal EMPTY = new TaskDependencyInternal() {
        @Override
        public void visitDependencies(TaskDependencyResolveContext context) {
        }

        @Override
        public Set<? extends Task> getDependencies(Task task) {
            return Collections.emptySet();
        }
    };
}
