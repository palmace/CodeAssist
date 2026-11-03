package org.gradle.api.internal.provider;

import org.gradle.api.Transformer;
import org.gradle.api.internal.tasks.TaskDependencyContainer;
import org.gradle.api.provider.Provider;
import org.gradle.internal.DisplayName;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

public interface ProviderInternal<T> extends Provider<T>, ValueSupplier, TaskDependencyContainer {
    @Nullable
    Class<T> getType();

    @Override
    <S> ProviderInternal<S> map(Transformer<? extends S, ? super T> transformer);

    ValueSupplier.Value<? extends T> calculateValue(ValueConsumer consumer);

    ProviderInternal<T> asSupplier(DisplayName owner, Class<? super T> targetType, ValueSanitizer<? super T> sanitizer);

    ProviderInternal<T> withFinalValue(ValueConsumer consumer);

    ExecutionTimeValue<? extends T> calculateExecutionTimeValue();

    @Override
    default <U, R> Provider<R> zip(Provider<U> right, BiFunction<? super T, ? super U, ? extends R> combiner) {
        throw new UnsupportedOperationException();
    }
}
