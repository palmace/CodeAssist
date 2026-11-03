package org.gradle.api.internal.provider;

import com.google.common.base.Objects;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.internal.Cast;
import org.gradle.internal.state.ManagedFactory;

import javax.annotation.Nullable;
import java.util.Map;

public class ManagedFactories {
    public static class ProviderManagedFactory implements ManagedFactory {
        public static final int FACTORY_ID = 1001;

        @Nullable
        @Override
        public <T> T fromState(Class<T> type, Object state) {
            if (!type.isAssignableFrom(Provider.class)) {
                return null;
            }
            return type.cast(Providers.ofNullable(state));
        }

        @Override
        public int getId() {
            return FACTORY_ID;
        }
    }

    public static class PropertyManagedFactory implements ManagedFactory {
        public static final int FACTORY_ID = Objects.hashCode(DefaultProperty.class.getName());

        @Nullable
        @Override
        public <S> S fromState(Class<S> type, Object state) {
            return null;
        }

        @Override
        public int getId() {
            return FACTORY_ID;
        }
    }

    public static class ListPropertyManagedFactory implements ManagedFactory {
        public static final int FACTORY_ID = Objects.hashCode(DefaultListProperty.class.getName());

        @Nullable
        @Override
        public <S> S fromState(Class<S> type, Object state) {
            return null;
        }

        @Override
        public int getId() {
            return FACTORY_ID;
        }
    }

    public static class SetPropertyManagedFactory implements ManagedFactory {
        public static final int FACTORY_ID = Objects.hashCode(DefaultSetProperty.class.getName());

        @Nullable
        @Override
        public <T> T fromState(Class<T> type, Object state) {
            return null;
        }

        @Override
        public int getId() {
            return FACTORY_ID;
        }
    }

    public static class MapPropertyManagedFactory implements ManagedFactory {
        public static final int FACTORY_ID = Objects.hashCode(DefaultMapProperty.class.getName());

        @Nullable
        @Override
        public <S> S fromState(Class<S> type, Object state) {
            return null;
        }

        @Override
        public int getId() {
            return FACTORY_ID;
        }
    }
}
