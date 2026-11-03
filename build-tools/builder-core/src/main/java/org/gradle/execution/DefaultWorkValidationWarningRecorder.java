package org.gradle.execution;

import com.google.common.collect.ImmutableSet;
import org.gradle.internal.deprecation.DeprecationLogger;
import org.gradle.internal.execution.UnitOfWork;
import org.gradle.internal.execution.steps.ValidateStep;
import org.gradle.internal.reflect.validation.TypeValidationProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.gradle.internal.reflect.validation.TypeValidationProblemRenderer.convertToSingleLine;
import static org.gradle.internal.reflect.validation.TypeValidationProblemRenderer.renderMinimalInformationAbout;

public class DefaultWorkValidationWarningRecorder implements ValidateStep.ValidationWarningRecorder, WorkValidationWarningReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkValidationWarningRecorder.class);

    private final AtomicInteger workWithFailuresCount = new AtomicInteger();

    // Ajustado para coincidir con la firma de ValidateStep.ValidationWarningRecorder en Gradle 8.x
    @Override
    public void recordValidationWarnings(UnitOfWork work, Collection<? extends org.gradle.internal.reflect.validation.TypeValidationProblem> warnings) {
        workWithFailuresCount.incrementAndGet();

        // Lógica de reporte adaptada
        warnings.forEach(warning -> {
            String message = convertToSingleLine(renderMinimalInformationAbout(warning, false, false));
            LOGGER.warn("Validation warning for {}: {}", work.getDisplayName(), message);
            
            DeprecationLogger.deprecateBehaviour(message)
                .withContext("Execution optimizations are disabled to ensure correctness.")
                .willBecomeAnErrorInGradle8()
                .nagUser();
        });
    }

    @Override
    public void reportWorkValidationWarningsAtEndOfBuild() {
        int workWithFailures = workWithFailuresCount.getAndSet(0);
        if (workWithFailures > 0) {
            LOGGER.warn("\nExecution optimizations have been disabled for {} invalid unit(s) of work.", workWithFailures);
        }
    }
}