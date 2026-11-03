package org.gradle.internal.resource;

import org.gradle.api.Describable;

/**
 * Represents some resource that may have content.
 */
public interface Resource extends Describable {
    /**
     * Returns a display name for this resource. This can be used in log and error messages.
     *
     * @return the display name
     */
    @Override
    String getDisplayName();
}
