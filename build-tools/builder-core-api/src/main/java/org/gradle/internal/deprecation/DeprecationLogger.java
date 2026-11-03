package org.gradle.internal.deprecation;

import javax.annotation.CheckReturnValue;

public class DeprecationLogger {

    @CheckReturnValue
    public static DeprecationMessageBuilder<?> deprecate(final String feature) {
        return new GenericDeprecationMessageBuilder(feature);
    }

    @CheckReturnValue
    public static DeprecationMessageBuilder.DeprecateBehaviour deprecateBehaviour(String behaviour) {
        return new DeprecationMessageBuilder.DeprecateBehaviour(behaviour);
    }

    public static void nagUserWith(DeprecationMessageBuilder<?> builder, Class<?> calledFrom) {
        System.out.println("DEPRECATION: " + builder.build().toDeprecatedFeatureUsage().formattedMessage());
    }

    private static class GenericDeprecationMessageBuilder extends DeprecationMessageBuilder<GenericDeprecationMessageBuilder> {
        private final String feature;

        GenericDeprecationMessageBuilder(String feature) {
            this.feature = feature;
        }

        @Override
        DeprecationMessage build() {
            setSummary(feature + " has been deprecated.");
            return super.build();
        }
    }
}
