package org.gradle.internal.deprecation;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.gradle.internal.featurelifecycle.FeatureUsage;

import javax.annotation.Nullable;

public class DeprecatedFeatureUsage extends FeatureUsage {

    private final String removalDetails;
    private final String advice;
    private final String contextualAdvice;
    private final Documentation documentation;
    private final Type type;

    public DeprecatedFeatureUsage(
        String summary,
        String removalDetails,
        @Nullable String advice,
        @Nullable String contextualAdvice,
        Documentation documentation,
        Type type
    ) {
        this(summary, removalDetails, advice, contextualAdvice, documentation, type, DeprecatedFeatureUsage.class);
    }

    public DeprecatedFeatureUsage(
        String summary,
        String removalDetails,
        @Nullable String advice,
        @Nullable String contextualAdvice,
        Documentation documentation,
        Type type,
        Class<?> calledFrom
    ) {
        super(summary, calledFrom);
        this.removalDetails = Preconditions.checkNotNull(removalDetails);
        this.advice = advice;
        this.contextualAdvice = contextualAdvice;
        this.type = Preconditions.checkNotNull(type);
        this.documentation = Preconditions.checkNotNull(documentation);
    }

    public enum Type {
        USER_CODE_DIRECT,
        USER_CODE_INDIRECT,
        BUILD_INVOCATION
    }

    public String getRemovalDetails() {
        return removalDetails;
    }

    @Nullable
    public String getAdvice() {
        return advice;
    }

    @Nullable
    public String getContextualAdvice() {
        return contextualAdvice;
    }

    @Nullable
    public String getDocumentationUrl() {
        return documentation.documentationUrl();
    }

    public Type getType() {
        return type;
    }

    @Override
    public String formattedMessage() {
        StringBuilder outputBuilder = new StringBuilder(getSummary());
        append(outputBuilder, removalDetails);
        append(outputBuilder, contextualAdvice);
        append(outputBuilder, advice);
        append(outputBuilder, documentation.consultDocumentationMessage());
        return outputBuilder.toString();
    }

    private void append(StringBuilder outputBuilder, String message) {
        if (!StringUtils.isEmpty(message)) {
            outputBuilder.append(" ").append(message);
        }
    }
}
