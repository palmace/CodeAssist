package org.gradle.api.internal.provider;

import org.gradle.api.Transformer;
import org.gradle.internal.Cast;
import org.gradle.internal.DisplayName;
import org.gradle.internal.logging.text.TreeFormatter;
import org.gradle.internal.state.Managed;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.provider.Provider;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiFunction;

public abstract class AbstractMinimalProvider<T> implements ProviderInternal<T>, Managed {
    private static final DisplayName DEFAULT_DISPLAY_NAME = new DisplayName() {
        @Override
        public String getCapitalizedDisplayName() {
            return "this provider".toUpperCase(Locale.ROOT);
        }

        @Override
        public String getDisplayName() {
            return "this provider"  ;
        }
    };

    @Override
    public <S> ProviderInternal<S> map(final Transformer<? extends S, ? super T> transformer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> Provider<S> flatMap(final Transformer<? extends Provider<? extends S>, ? super T> transformer) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    protected DisplayName getDeclaredDisplayName() {
        return null;
    }

    protected DisplayName getDisplayName() {
        DisplayName displayName = getDeclaredDisplayName();
        if (displayName == null) {
            return DEFAULT_DISPLAY_NAME;
        }
        return displayName;
    }

    protected DisplayName getTypedDisplayName() {
        return DEFAULT_DISPLAY_NAME;
    }

    protected abstract ValueSupplier.Value<? extends T> calculateOwnValue(ValueConsumer consumer);

    @Override
    public boolean isPresent() {
        return calculatePresence(ValueConsumer.IgnoreUnsafeRead);
    }

    @Override
    public boolean calculatePresence(ValueConsumer consumer) {
        return !calculateOwnValue(consumer).isMissing();
    }

    @Override
    public T get() {
        Value<? extends T> value = calculateOwnValue(ValueConsumer.IgnoreUnsafeRead);
        if (value.isMissing()) {
            throw new MissingValueException(cannotQueryValueOf(value));
        }
        return value.get();
    }

    @Override
    public T getOrNull() {
        return calculateOwnValue(ValueConsumer.IgnoreUnsafeRead).orNull();
    }

    @Override
    public T getOrElse(T defaultValue) {
        return calculateOwnValue(ValueConsumer.IgnoreUnsafeRead).orElse(defaultValue);
    }

    @Override
    public Value<? extends T> calculateValue(ValueConsumer consumer) {
        return calculateOwnValue(consumer).pushWhenMissing(getDeclaredDisplayName());
    }

    @Override
    public Provider<T> orElse(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Provider<T> orElse(Provider<? extends T> provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Provider<T> forUseAtConfigurationTime() {
        return this;
    }

    @Override
    public void visitDependencies(TaskDependencyResolveContext context) {
        getProducer().visitProducerTasks(context);
    }

    @Override
    public ValueProducer getProducer() {
        return ValueProducer.unknown();
    }

    @Override
    public ExecutionTimeValue<? extends T> calculateExecutionTimeValue() {
        return ExecutionTimeValue.value(calculateOwnValue(ValueConsumer.IgnoreUnsafeRead));
    }

    @Override
    public ProviderInternal<T> asSupplier(DisplayName owner, Class<? super T> targetType, ValueSanitizer<? super T> sanitizer) {
        if (getType() != null && !targetType.isAssignableFrom(getType())) {
            throw new IllegalArgumentException(String.format("Cannot set the value of %s of type %s using a provider of type %s.", owner.getDisplayName(), targetType.getName(), getType().getName()));
        } else {
            return this;
        }
    }

    @Override
    public ProviderInternal<T> withFinalValue(ValueConsumer consumer) {
        return Providers.nullableValue(calculateValue(consumer));
    }

    @Override
    public <U, R> Provider<R> zip(Provider<U> right, BiFunction<? super T, ? super U, ? extends R> combiner) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        Class<?> type = getType();
        return String.format("provider(%s)", type == null ? "?" : type.getName());
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public Class<?> publicType() {
        return Provider.class;
    }

    @Override
    public Object unpackState() {
        return getOrNull();
    }

    @Override
    public int getFactoryId() {
        return 0;
    }

    private String cannotQueryValueOf(Value<? extends T> value) {
        TreeFormatter formatter = new TreeFormatter();
        formatter.node("Cannot query the value of ").append(getDisplayName().getDisplayName()).append(" because it has no value available.");
        if (!value.getPathToOrigin().isEmpty()) {
            formatter.node("The value of ").append(getTypedDisplayName().getDisplayName()).append(" is derived from");
            formatter.startChildren();
            for (DisplayName displayName : value.getPathToOrigin()) {
                formatter.node(displayName.getDisplayName());
            }
            formatter.endChildren();
        }
        return formatter.toString();
    }
}
