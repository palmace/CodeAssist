package org.gradle.api.tasks.incremental;

import org.gradle.api.Action;
import org.gradle.api.tasks.InputFileDetails;

/**
 * Bridge interface for legacy Gradle API compatibility
 */
public interface IncrementalTaskInputs {
    boolean isIncremental();
    void outOfDate(Action<? super InputFileDetails> action);
    void removed(Action<? super InputFileDetails> action);
}
