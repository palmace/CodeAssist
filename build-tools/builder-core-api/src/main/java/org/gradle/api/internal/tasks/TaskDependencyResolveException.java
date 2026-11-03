package org.gradle.api.internal.tasks;

public class TaskDependencyResolveException extends RuntimeException {
    public TaskDependencyResolveException(String message, Throwable cause) {
        super(message, cause);
    }
}
