package org.gradle.internal.state;

import org.gradle.api.Describable;
import org.gradle.api.Task;

import javax.annotation.Nullable;

/**
 * An object that represents some part of a model.
 */
public interface ModelObject {
    /**
     * Returns the display name of this object that indicates its identity, if this is known.
     */
    @Nullable
    Describable getModelIdentityDisplayName();

    /**
     * Does this type provide a useful {@link Object#toString()} implementation?
     */
    boolean hasUsefulDisplayName();

    /**
     * Returns the task that owns this object, if any.
     */
    @Nullable
    Task getTaskThatOwnsThisObject();
}
