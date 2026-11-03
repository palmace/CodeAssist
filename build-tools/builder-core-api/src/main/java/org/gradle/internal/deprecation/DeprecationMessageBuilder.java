package org.gradle.internal.deprecation;

import com.google.common.base.Joiner;
import org.gradle.util.GradleVersion;

import java.util.List;
import java.util.function.Function;

import javax.annotation.CheckReturnValue;

@CheckReturnValue
public class DeprecationMessageBuilder<T extends DeprecationMessageBuilder<T>> {

    private static final GradleVersion GRADLE8 = GradleVersion.version("8.0");

    private String summary;
    private DeprecationTimeline deprecationTimeline;
    private String context;
    private String advice;
    private Documentation documentation = Documentation.NO_DOCUMENTATION;
    private DeprecatedFeatureUsage.Type usageType = DeprecatedFeatureUsage.Type.USER_CODE_DIRECT;

    DeprecationMessageBuilder() {
    }

    @SuppressWarnings("unchecked")
    public T withContext(String context) {
        this.context = context;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withAdvice(String advice) {
        this.advice = advice;
        return (T) this;
    }

    public WithDeprecationTimeline willBeRemovedInGradle8() {
        this.deprecationTimeline = DeprecationTimeline.willBeRemovedInVersion(GRADLE8);
        return new WithDeprecationTimeline(this);
    }

    public WithDeprecationTimeline willBecomeAnErrorInGradle8() {
        this.deprecationTimeline = DeprecationTimeline.willBecomeAnErrorInVersion(GRADLE8);
        return new WithDeprecationTimeline(this);
    }

    void setIndirectUsage() {
        this.usageType = DeprecatedFeatureUsage.Type.USER_CODE_INDIRECT;
    }

    void setSummary(String summary) {
        this.summary = summary;
    }

    void setAdvice(String advice) {
        this.advice = advice;
    }

    void setDeprecationTimeline(DeprecationTimeline deprecationTimeline) {
        this.deprecationTimeline = deprecationTimeline;
    }

    void setDocumentation(Documentation documentation) {
        this.documentation = documentation;
    }

    DeprecationMessage build() {
        return new DeprecationMessage(summary, deprecationTimeline.toString(), advice, context,
                documentation, usageType);
    }

    public static class WithDeprecationTimeline {
        private final DeprecationMessageBuilder<?> builder;

        public WithDeprecationTimeline(DeprecationMessageBuilder<?> builder) {
            this.builder = builder;
        }

        public WithDocumentation undocumented() {
            return new WithDocumentation(builder);
        }

        public WithDocumentation withUserManual(String documentationId) {
            builder.setDocumentation(Documentation.userManual(documentationId));
            return new WithDocumentation(builder);
        }

        public WithDocumentation withUserManual(String documentationId, String section) {
            builder.setDocumentation(Documentation.userManual(documentationId, section));
            return new WithDocumentation(builder);
        }

        public WithDocumentation withDslReference(Class<?> targetClass, String property) {
            builder.setDocumentation(Documentation.dslReference(targetClass, property));
            return new WithDocumentation(builder);
        }

        public WithDocumentation withUpgradeGuideSection(int majorVersion,
                                                         String upgradeGuideSection) {
            builder.setDocumentation(Documentation.upgradeGuide(majorVersion, upgradeGuideSection));
            return new WithDocumentation(builder);
        }
    }

    public static class WithDocumentation {
        private final DeprecationMessageBuilder<?> builder;

        WithDocumentation(DeprecationMessageBuilder<?> builder) {
            this.builder = builder;
        }

        public void nagUser() {
            DeprecationLogger.nagUserWith(builder, WithDocumentation.class);
        }
    }

    public static abstract class WithReplacement<T, SELF extends WithReplacement<T, SELF>> extends DeprecationMessageBuilder<SELF> {
        private final String subject;
        private T replacement;

        WithReplacement(String subject) {
            this.subject = subject;
        }

        @SuppressWarnings("unchecked")
        public SELF replaceWith(T replacement) {
            this.replacement = replacement;
            return (SELF) this;
        }

        String formatSubject() {
            return subject;
        }

        abstract String formatSummary(String subject);

        abstract String formatAdvice(T replacement);

        @Override
        DeprecationMessage build() {
            setSummary(formatSummary(formatSubject()));
            if (replacement != null) {
                setAdvice(formatAdvice(replacement));
            }
            return super.build();
        }
    }

    public static class DeprecateBehaviour extends DeprecationMessageBuilder<DeprecateBehaviour> {
        private final String behaviour;

        public DeprecateBehaviour(String behaviour) {
            this.behaviour = behaviour;
        }

        @Override
        public WithDeprecationTimeline willBeRemovedInGradle8() {
            setDeprecationTimeline(DeprecationTimeline.behaviourWillBeRemovedInVersion(GRADLE8));
            return new WithDeprecationTimeline(this);
        }

        @Override
        DeprecationMessage build() {
            setSummary(behaviour);
            return super.build();
        }
    }

    public static class DeprecateConfiguration extends WithReplacement<List<String>,
            DeprecateConfiguration> {
        private final ConfigurationDeprecationType deprecationType;

        DeprecateConfiguration(String configuration, ConfigurationDeprecationType deprecationType) {
            super(configuration);
            this.deprecationType = deprecationType;
            if (!deprecationType.inUserCode) {
                setIndirectUsage();
            }
        }

        @Override
        String formatSummary(String configuration) {
            return String.format("The %s configuration has been deprecated for %s.", configuration,
                    deprecationType.displayName());
        }

        @Override
        String formatAdvice(List<String> replacements) {
            return String.format("Please %s the %s configuration instead.", deprecationType.usage,
                    Joiner.on(" or ").join(replacements));
        }
    }
}
