package org.gradle.internal.featurelifecycle;

public interface FeatureHandler<T extends FeatureUsage> {
    void featureUsed(T usage);
}
