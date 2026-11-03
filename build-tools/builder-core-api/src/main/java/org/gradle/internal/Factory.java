package org.gradle.internal;

import javax.annotation.Nullable;

public interface Factory<T> {
    @Nullable
    T create();
}
