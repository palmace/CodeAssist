package org.gradle.util.internal;

import org.gradle.api.provider.ProviderResolutionStrategy;
import org.gradle.api.provider.Provider;
import org.gradle.internal.Factory;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

import static org.gradle.util.GUtil.uncheckedCall;

public class DeferredUtil {

    @Nullable
    public static Object unpack(@Nullable Object deferred) {
        return unpack(ProviderResolutionStrategy.REQUIRE_PRESENT, deferred);
    }

    @Nullable
    public static Object unpackOrNull(@Nullable Object deferred) {
        return unpack(ProviderResolutionStrategy.ALLOW_ABSENT, deferred);
    }

    @Nullable
    private static Object unpack(ProviderResolutionStrategy providerResolutionStrategy, @Nullable Object deferred) {
        if (deferred == null) {
            return null;
        }
        Object value = unpackNestableDeferred(deferred);
        if (value instanceof Provider) {
            return providerResolutionStrategy.resolve((Provider<?>) value);
        }
        if (value instanceof Factory) {
            return ((Factory<?>) value).create();
        }
        return value;
    }

    public static boolean isDeferred(Object value) {
        return value instanceof Provider
               || value instanceof Factory
               || isNestableDeferred(value);
    }

    public static boolean isNestableDeferred(@Nullable Object value) {
        return value instanceof Callable;
    }

    @Nullable
    public static Object unpackNestableDeferred(@Nullable Object deferred) {
        Object current = deferred;
        while (isNestableDeferred(current)) {
            if (current instanceof Callable) {
                current = uncheckedCall((Callable<?>) current);
            }
        }
        return current;
    }
}
