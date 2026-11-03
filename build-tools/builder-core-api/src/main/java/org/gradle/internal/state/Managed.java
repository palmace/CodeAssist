package org.gradle.internal.state;

import javax.annotation.Nullable;

/**
 * Implemented by types whose state is fully managed by Gradle.
 */
public interface Managed {
    @Nullable
    Object unpackState();

    boolean isImmutable();

    Class<?> publicType();

    int getFactoryId();
}
