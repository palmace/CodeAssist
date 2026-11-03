package org.gradle.api.tasks;

import java.io.File;

/**
 * Bridge interface for legacy Gradle API compatibility
 */
public interface InputFileDetails {
    File getFile();
    boolean isAdded();
    boolean isModified();
    boolean isRemoved();
}
