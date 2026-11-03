package org.gradle.api.internal.provider;

import org.gradle.internal.state.ModelObject;
import org.gradle.internal.state.OwnerAware;

public interface PropertyInternal<T> extends ProviderInternal<T>, HasConfigurableValueInternal, OwnerAware {
    void setFromAnyValue(Object object);

    void attachProducer(ModelObject owner);
}
