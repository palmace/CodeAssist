package org.gradle.internal.execution;

import com.google.common.collect.ImmutableSortedMap;
import org.gradle.api.Describable;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.execution.OutputSnapshotter.OutputFileSnapshottingException;
import org.gradle.internal.execution.caching.CachingDisabledReason;
import org.gradle.internal.execution.caching.CachingState;
import org.gradle.internal.execution.fingerprint.InputFingerprinter;
import org.gradle.internal.execution.fingerprint.InputFingerprinter.InputFileFingerprintingException;
import org.gradle.internal.execution.fingerprint.InputFingerprinter.InputVisitor;
import org.gradle.internal.execution.history.OverlappingOutputs;
import org.gradle.internal.execution.history.changes.InputChangesInternal;
import org.gradle.internal.execution.workspace.WorkspaceProvider;
import org.gradle.internal.file.TreeType;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.snapshot.FileSystemSnapshot;
import org.gradle.internal.snapshot.ValueSnapshot;
import org.gradle.internal.snapshot.impl.ImplementationSnapshot;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface InternalUnitOfWork extends Describable {
    Identity identify(Map<String, ValueSnapshot> identityInputs, Map<String, CurrentFileCollectionFingerprint> identityFileInputs);

    interface Identity {
        String getUniqueId();
    }

    WorkOutput execute(ExecutionRequest executionRequest);

    interface ExecutionRequest {
        File getWorkspace();
        Optional<InputChangesInternal> getInputChanges();
        Optional<ImmutableSortedMap<String, FileSystemSnapshot>> getPreviouslyProducedOutputs();
    }

    interface WorkOutput {
        WorkResult getDidWork();
        Object getOutput();
    }

    enum WorkResult {
        DID_WORK,
        DID_NO_WORK
    }

    default Object loadRestoredOutput(File workspace) {
        throw new UnsupportedOperationException();
    }

    WorkspaceProvider getWorkspaceProvider();

    default Optional<Duration> getTimeout() {
        return Optional.empty();
    }

    default InputChangeTrackingStrategy getInputChangeTrackingStrategy() {
        return InputChangeTrackingStrategy.NONE;
    }

    default void visitImplementations(ImplementationVisitor visitor) {
        visitor.visitImplementation(getClass());
    }

    interface ImplementationVisitor {
        void visitImplementation(Class<?> implementation);
        void visitImplementation(ImplementationSnapshot implementation);
    }

    InputFingerprinter getInputFingerprinter();

    default void visitIdentityInputs(InputVisitor visitor) {}
    default void visitRegularInputs(InputVisitor visitor) {}

    void visitOutputs(File workspace, OutputVisitor visitor);

    interface OutputVisitor {
        default void visitOutputProperty(String propertyName, TreeType type, File root, FileCollection contents) {}
        default void visitLocalState(File localStateRoot) {}
        default void visitDestroyable(File destroyableRoot) {}
    }

    default void handleUnreadableInputs(InputFileFingerprintingException ex) {
        throw ex;
    }

    default void handleUnreadableOutputs(OutputFileSnapshottingException ex) {
        throw ex;
    }

    default void validate(WorkValidationContext validationContext) {}

    default Optional<CachingDisabledReason> shouldDisableCaching(@Nullable OverlappingOutputs detectedOverlappingOutputs) {
        return Optional.empty();
    }

    default boolean isAllowedToLoadFromCache() {
        return true;
    }

    default OverlappingOutputHandling getOverlappingOutputHandling() {
        return OverlappingOutputHandling.IGNORE_OVERLAPS;
    }

    enum OverlappingOutputHandling {
        /**
         * Overlapping outputs are detected and handled.
         */
        DETECT_OVERLAPS,

        /**
         * Overlapping outputs are not detected.
         */
        IGNORE_OVERLAPS
    }

    default boolean shouldCleanupOutputsOnNonIncrementalExecution() {
        return true;
    }

    enum InputChangeTrackingStrategy {
        NONE(false),
        /**
         * Only the incremental parameters should be tracked for input changes.
         */
        INCREMENTAL_PARAMETERS(true),
        /**
         * All parameters are considered incremental.
         *
         * @deprecated Only used for {@code IncrementalTaskInputs}. Should be removed once {@code IncrementalTaskInputs} is gone.
         */
        @Deprecated
        ALL_PARAMETERS(true);

        private final boolean requiresInputChanges;
        InputChangeTrackingStrategy(boolean requiresInputChanges) {
            this.requiresInputChanges = requiresInputChanges;
        }
        public boolean requiresInputChanges() {
            return requiresInputChanges;
        }
    }

    default void markLegacySnapshottingInputsStarted() {}
    default void markLegacySnapshottingInputsFinished(CachingState cachingState) {}
    default void ensureLegacySnapshottingInputsClosed() {}

    default WorkValidationContext.TypeOriginInspector getTypeOriginInspector() {
        return WorkValidationContext.TypeOriginInspector.NO_OP;
    }
}