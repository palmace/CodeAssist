package org.gradle.api.internal.provider;

import com.google.common.collect.ImmutableList;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.internal.DisplayName;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ValueSupplier {
    ValueProducer getProducer();

    boolean calculatePresence(ValueConsumer consumer);

    enum ValueConsumer {
        DisallowUnsafeRead, IgnoreUnsafeRead
    }

    interface ValueProducer {
        NoProducer NO_PRODUCER = new NoProducer();
        UnknownProducer UNKNOWN_PRODUCER = new UnknownProducer();

        default boolean isKnown() {
            return true;
        }

        boolean isProducesDifferentValueOverTime();

        void visitProducerTasks(Action<? super Task> visitor);

        default void visitContentProducerTasks(Action<? super Task> visitor) {
            visitProducerTasks(visitor);
        }

        default ValueProducer plus(ValueProducer producer) {
            if (this == NO_PRODUCER) {
                return producer;
            }
            if (producer == NO_PRODUCER) {
                return this;
            }
            if (producer == this) {
                return this;
            }
            return new PlusProducer(this, producer);
        }

        static ValueProducer noProducer() {
            return NO_PRODUCER;
        }

        static ValueProducer unknown() {
            return UNKNOWN_PRODUCER;
        }

        static ValueProducer externalValue() {
            return new ExternalValueProducer();
        }

        static ValueProducer task(Task task) {
            return new TaskProducer(task, true);
        }

        static ValueProducer taskState(Task task) {
            return new TaskProducer(task, false);
        }
    }

    class ExternalValueProducer implements ValueProducer {
        @Override
        public boolean isProducesDifferentValueOverTime() {
            return true;
        }

        @Override
        public void visitProducerTasks(Action<? super Task> visitor) {
        }
    }

    class TaskProducer implements ValueProducer {
        private final Task task;
        private boolean content;

        public TaskProducer(Task task, boolean content) {
            this.task = task;
            this.content = content;
        }

        @Override
        public boolean isProducesDifferentValueOverTime() {
            return false;
        }

        @Override
        public void visitProducerTasks(Action<? super Task> visitor) {
            visitor.execute(task);
        }

        @Override
        public void visitContentProducerTasks(Action<? super Task> visitor) {
            if (content) {
                visitor.execute(task);
            }
        }
    }

    class PlusProducer implements ValueProducer {
        private final ValueProducer left;
        private final ValueProducer right;

        public PlusProducer(ValueProducer left, ValueProducer right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isKnown() {
            return left.isKnown() || right.isKnown();
        }

        @Override
        public boolean isProducesDifferentValueOverTime() {
            return left.isProducesDifferentValueOverTime() || right.isProducesDifferentValueOverTime();
        }

        @Override
        public void visitProducerTasks(Action<? super Task> visitor) {
            left.visitProducerTasks(visitor);
            right.visitProducerTasks(visitor);
        }
    }

    class UnknownProducer implements ValueProducer {
        @Override
        public boolean isKnown() {
            return false;
        }

        @Override
        public boolean isProducesDifferentValueOverTime() {
            return false;
        }

        @Override
        public void visitProducerTasks(Action<? super Task> visitor) {
        }
    }

    class NoProducer implements ValueProducer {
        @Override
        public boolean isProducesDifferentValueOverTime() {
            return false;
        }

        @Override
        public void visitProducerTasks(Action<? super Task> visitor) {
        }
    }

    interface Value<T> {
        Value<Object> MISSING = new Missing<>();
        Value<Void> SUCCESS = new Present<>(null);

        static <T> Value<T> ofNullable(@Nullable T value) {
            if (value == null) {
                return MISSING.asType();
            }
            return new Present<>(value);
        }

        static <T> Value<T> missing() {
            return MISSING.asType();
        }

        static <T> Value<T> of(T value) {
            if (value == null) {
                throw new IllegalArgumentException();
            }
            return new Present<>(value);
        }

        static Value<Void> present() {
            return SUCCESS;
        }

        T get() throws IllegalStateException;

        @Nullable
        T orNull();

        <S> S orElse(S defaultValue);

        List<DisplayName> getPathToOrigin();

        boolean isMissing();

        <S> Value<S> asType();

        Value<T> pushWhenMissing(@Nullable DisplayName displayName);

        Value<T> addPathsFrom(Value<?> rightValue);
    }

    class Present<T> implements Value<T> {
        private final T result;

        private Present(T result) {
            this.result = result;
        }

        @Override
        public boolean isMissing() {
            return false;
        }

        @Override
        public T get() throws IllegalStateException {
            return result;
        }

        @Override
        public T orNull() {
            return result;
        }

        @Override
        public <S> S orElse(S defaultValue) {
            return (S) result;
        }

        @Override
        public Value<T> pushWhenMissing(@Nullable DisplayName displayName) {
            return this;
        }

        @Override
        public <S> Value<S> asType() {
            throw new IllegalStateException();
        }

        @Override
        public List<DisplayName> getPathToOrigin() {
            throw new IllegalStateException();
        }

        @Override
        public Value<T> addPathsFrom(Value<?> rightValue) {
            throw new IllegalStateException();
        }
    }

    class Missing<T> implements Value<T> {
        private final List<DisplayName> path;

        private Missing() {
            this.path = ImmutableList.of();
        }

        private Missing(List<DisplayName> path) {
            this.path = path;
        }

        @Override
        public boolean isMissing() {
            return true;
        }

        @Override
        public T get() throws IllegalStateException {
            throw new IllegalStateException();
        }

        @Override
        public T orNull() {
            return null;
        }

        @Override
        public <S> S orElse(S defaultValue) {
            return defaultValue;
        }

        @Override
        public List<DisplayName> getPathToOrigin() {
            return path;
        }

        @Override
        public <S> Value<S> asType() {
            return (Value<S>) this;
        }

        @Override
        public Value<T> pushWhenMissing(@Nullable DisplayName displayName) {
            if (displayName == null) {
                return this;
            }
            ImmutableList.Builder<DisplayName> builder = ImmutableList.builderWithExpectedSize(path.size() + 1);
            builder.add(displayName);
            builder.addAll(path);
            return new Missing<>(builder.build());
        }

        @Override
        public Value<T> addPathsFrom(Value<?> rightValue) {
            if (path.isEmpty()) {
                return rightValue.asType();
            }
            Missing<?> other = (Missing<?>) rightValue;
            if (other.path.isEmpty()) {
                return this;
            }
            ImmutableList.Builder<DisplayName> builder = ImmutableList.builderWithExpectedSize(path.size() + other.path.size());
            builder.addAll(path);
            builder.addAll(other.path);
            return new Missing<>(builder.build());
        }
    }

    abstract class ExecutionTimeValue<T> {
        private static final MissingExecutionTimeValue MISSING = new MissingExecutionTimeValue();

        public boolean isMissing() {
            return false;
        }

        public boolean isFixedValue() {
            return false;
        }

        public boolean isChangingValue() {
            return false;
        }

        public boolean hasChangingContent() {
            return false;
        }

        public T getFixedValue() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public ProviderInternal<T> getChangingValue() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public Value<T> toValue() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public abstract ProviderInternal<T> toProvider();

        public abstract ExecutionTimeValue<T> withChangingContent();

        public static <T> ExecutionTimeValue<T> missing() {
            return ((ExecutionTimeValue<T>) MISSING);
        }

        public static <T> ExecutionTimeValue<T> fixedValue(T value) {
            assert value != null;
            return new FixedExecutionTimeValue<>(value, false);
        }

        public static <T> ExecutionTimeValue<T> ofNullable(@Nullable T value) {
            if (value == null) {
                return missing();
            } else {
                return fixedValue(value);
            }
        }

        public static <T> ExecutionTimeValue<T> value(Value<T> value) {
            if (value.isMissing()) {
                return missing();
            } else {
                return fixedValue(value.get());
            }
        }

        public static <T> ExecutionTimeValue<T> changingValue(ProviderInternal<T> provider) {
            return new ChangingExecutionTimeValue<>(provider);
        }
    }

    class MissingExecutionTimeValue extends ExecutionTimeValue<Object> {
        @Override
        public String toString() {
            return "missing";
        }

        @Override
        public boolean isMissing() {
            return true;
        }

        @Override
        public ProviderInternal<Object> toProvider() {
            return Providers.notDefined();
        }

        @Override
        public ExecutionTimeValue<Object> withChangingContent() {
            return this;
        }

        @Override
        public Value<Object> toValue() {
            return Value.missing();
        }
    }

    class FixedExecutionTimeValue<T> extends ExecutionTimeValue<T> {
        private final T value;
        private final boolean changingContent;

        private FixedExecutionTimeValue(T value, boolean changingContent) {
            this.value = value;
            this.changingContent = changingContent;
        }

        @Override
        public String toString() {
            return String.format("fixed(%s)", value);
        }

        @Override
        public boolean isFixedValue() {
            return true;
        }

        @Override
        public boolean hasChangingContent() {
            return changingContent;
        }

        @Override
        public T getFixedValue() {
            return value;
        }

        @Override
        public Value<T> toValue() {
            return Value.of(value);
        }

        @Override
        public ProviderInternal<T> toProvider() {
            if (changingContent) {
                return new Providers.FixedValueWithChangingContentProvider<>(value);
            }
            return Providers.of(value);
        }

        @Override
        public ExecutionTimeValue<T> withChangingContent() {
            return new FixedExecutionTimeValue<>(value, true);
        }
    }

    class ChangingExecutionTimeValue<T> extends ExecutionTimeValue<T> {
        private final ProviderInternal<T> provider;

        private ChangingExecutionTimeValue(ProviderInternal<T> provider) {
            this.provider = provider;
        }

        @Override
        public String toString() {
            return String.format("changing(%s)", provider);
        }

        @Override
        public boolean isChangingValue() {
            return true;
        }

        @Override
        public boolean hasChangingContent() {
            return true;
        }

        @Override
        public ProviderInternal<T> getChangingValue() {
            return provider;
        }

        @Override
        public ProviderInternal<T> toProvider() {
            return provider;
        }

        @Override
        public ExecutionTimeValue<T> withChangingContent() {
            return this;
        }
    }
}
