package org.gradle.internal.state;

import javax.annotation.Nullable;

public interface ManagedFactory {
    @Nullable
    <T> T fromState(Class<T> type, Object state);
    int getId();
}
