package com.tyron.builder.gradle.tasks

import com.android.SdkConstants
import com.tyron.builder.api.variant.BuiltArtifacts
import com.tyron.builder.api.variant.impl.BuiltArtifactsImpl
import com.tyron.builder.api.variant.impl.VariantOutputImpl
import com.tyron.builder.api.variant.impl.dirName
import com.tyron.builder.api.variant.impl.getApiString
import com.tyron.builder.gradle.internal.LoggerWrapper
import com.tyron.builder.gradle.internal.component.AndroidTestCreationConfig
import com.tyron.builder.gradle.internal.component.ComponentCreationConfig
import com.tyron.builder.gradle.internal.component.InstrumentedTestCreationConfig
import com.tyron.builder.gradle.internal.component.TestCreationConfig
import com.tyron.builder.gradle.internal.component.TestVariantCreationConfig
import com.tyron.builder.gradle.internal.publishing.AndroidArtifacts
import com.tyron.builder.gradle.internal.publishing.AndroidArtifacts.ArtifactScope
import com.tyron.builder.gradle.internal.publishing.AndroidArtifacts.ConsumedConfigType
import com.tyron.builder.gradle.internal.scope.InternalArtifactType
import com.tyron.builder.gradle.internal.scope.InternalArtifactType.PACKAGED_MANIFESTS
import com.tyron.builder.gradle.internal.tasks.BuildAnalyzer
import com.tyron.builder.gradle.internal.tasks.factory.VariantTaskCreationAction
import com.tyron.builder.gradle.internal.tasks.manifest.ManifestProviderImpl
import com.tyron.builder.internal.utils.setDisallowChanges
import com.tyron.builder.gradle.tasks.ProcessApplicationManifest.Companion.getArtifactName
import com.tyron.builder.internal.InstrumentedTestManifestGenerator
import com.tyron.builder.internal.UnitTestManifestGenerator
import com.tyron.builder.gradle.internal.tasks.TaskCategory
import com.tyron.builder.compiler.manifest.ManifestMerger2
import com.tyron.builder.compiler.manifest.ManifestProvider
import com.tyron.builder.compiler.manifest.MergingReport
import com.tyron.builder.compiler.manifest.PlaceholderHandler
import com.android.utils.FileUtils
import com.tyron.builder.log.ILogger
import com.google.common.base.Charsets
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.google.common.io.Files
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException

/**
 * A task that processes the manifest for test modules and tests in androidTest.
 * Suturado: Corregidos imports para usar ManifestMerger2 custom y constantes internas.
 */
@DisableCachingByDefault
@BuildAnalyzer(primaryTaskCategory = TaskCategory.MANIFEST)
abstract class ProcessTestManifest : ManifestProcessorTask() {

    @get:OutputDirectory
    abstract val packagedManifestOutputDirectory: DirectoryProperty

    @get:Internal
    abstract val tmpDir: DirectoryProperty

    private var manifests: ArtifactCollection? = null

    @get:Nested
    abstract val apkData: Property<VariantOutputImpl>

    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    var navigationJsons: FileCollection? = null
        private set

    override fun doTaskAction() {
        val dirName = apkData.get().variantOutputConfiguration.dirName()
        val manifestOutputFolder =
            if (Strings.isNullOrEmpty(dirName)) packagedManifestOutputDirectory.get().asFile
            else packagedManifestOutputDirectory.get().file(dirName).asFile
        FileUtils.mkdirs(manifestOutputFolder)
        val manifestOutputFile = File(manifestOutputFolder, SdkConstants.ANDROID_MANIFEST_XML)
        val navJsons = navigationJsons?.files ?: listOf<File>()

        mergeManifestsForTestVariant(
            testApplicationId.get(),
            namespace.get(),
            minSdkVersion.get(),
            targetSdkVersion.get(),
            testedApplicationId.get(),
            instrumentationRunner.get(),
            handleProfiling.orNull,
            functionalTest.orNull,
            testLabel.orNull,
            if (testManifestFile.get().asFile.isFile) testManifestFile.get().asFile else null,
            computeProviders(),
            placeholdersValues.get().mapValues { it.value.toString() },
            navJsons,
            jniLibsUseLegacyPackaging.orNull,
            debuggable.get(),
            manifestOutputFile,
            tmpDir.get().asFile
        )
        BuiltArtifactsImpl(
            BuiltArtifacts.METADATA_FILE_VERSION,
            PACKAGED_MANIFESTS,
            testApplicationId.get(),
            variantName,
            listOf(
                apkData.get().toBuiltArtifact(
                    manifestOutputFile
                )
            )
        )
            .saveToDirectory(packagedManifestOutputDirectory.get().asFile)
    }

    private fun mergeManifestsForTestVariant(
        testApplicationId: String,
        namespace: String,
        minSdkVersion: String,
        targetSdkVersion: String,
        testedApplicationId: String,
        instrumentationRunner: String,
        handleProfiling: Boolean?,
        functionalTest: Boolean?,
        testLabel: String?,
        testManifestFile: File?,
        manifestProviders: List<ManifestProvider?>,
        manifestPlaceholders: Map<String, String>,
        navigationJsons: Collection<File>,
        jniLibsUseLegacyPackaging: Boolean?,
        debuggable: Boolean,
        outManifest: File,
        tmpDir: File
    ) {
        val logger: ILogger = LoggerWrapper(logger)
        var tempFile1: File? = null
        var tempFile2: File? = null
        try {
            FileUtils.mkdirs(tmpDir)
            var generatedTestManifest: File =
                File.createTempFile("tempFile1ProcessTestManifest", ".xml", tmpDir)
                    .also { tempFile1 = it }
            
            if (handleProfiling != null) {
                InstrumentedTestManifestGenerator(
                    generatedTestManifest,
                    testApplicationId,
                    minSdkVersion,
                    if (targetSdkVersion == "-1") null else targetSdkVersion,
                    testedApplicationId,
                    instrumentationRunner,
                    handleProfiling!!,
                    functionalTest ?: false
                ).generate()
            } else {
                UnitTestManifestGenerator(
                    generatedTestManifest,
                    testApplicationId,
                    minSdkVersion,
                    if (targetSdkVersion == "-1") null else targetSdkVersion,
                    testApplicationId,
                    instrumentationRunner
                ).generate()
            }
            if (testManifestFile != null && testManifestFile.exists()) {
                val intermediateInvoker = ManifestMerger2.newMerger(
                    testManifestFile,
                    logger,
                    ManifestMerger2.MergeType.APPLICATION
                )
                    .setPlaceHolderValues(manifestPlaceholders)
                    .addFlavorAndBuildTypeManifests(*manifestOverlays.get().toTypedArray())
                    .addLibraryManifest(generatedTestManifest)
                    .setOverride(ManifestMerger2.SystemProperty.PACKAGE, testApplicationId)
                    .setOverride(ManifestMerger2.SystemProperty.MIN_SDK_VERSION, minSdkVersion)
                    .withFeatures(
                        ManifestMerger2.Invoker.Feature.DISABLE_MINSDKLIBRARY_CHECK,
                        ManifestMerger2.Invoker.Feature.WARN_IF_PACKAGE_IN_SOURCE_MANIFEST
                    )

                instrumentationRunner?.let {
                    intermediateInvoker.setPlaceHolderValue(
                        PlaceholderHandler.INSTRUMENTATION_RUNNER,
                        it)
                }
                if (targetSdkVersion != "-1") {
                    intermediateInvoker.setOverride(
                        ManifestMerger2.SystemProperty.TARGET_SDK_VERSION, targetSdkVersion
                    )
                }
                tempFile2 = File.createTempFile("tempFile2ProcessTestManifest", ".xml", tmpDir)
                handleMergingResult(intermediateInvoker.merge(), tempFile2, logger)
                generatedTestManifest = tempFile2
            }
            val finalInvoker = ManifestMerger2.newMerger(
                generatedTestManifest,
                logger,
                ManifestMerger2.MergeType.APPLICATION
            )
                .withFeatures(
                    ManifestMerger2.Invoker.Feature.REMOVE_TOOLS_DECLARATIONS
                )
                .setOverride(ManifestMerger2.SystemProperty.PACKAGE, testApplicationId)
                .addManifestProviders(manifestProviders as List<ManifestProvider>)
                .setPlaceHolderValues(manifestPlaceholders)
            if (jniLibsUseLegacyPackaging == false) {
                finalInvoker.withFeatures(ManifestMerger2.Invoker.Feature.DO_NOT_EXTRACT_NATIVE_LIBS)
            }
            if (debuggable) {
                finalInvoker.withFeatures(ManifestMerger2.Invoker.Feature.DEBUGGABLE)
            }
            handleMergingResult(finalInvoker.merge(), outManifest, logger)
        } catch (e: IOException) {
            throw RuntimeException("Unable to create the temporary file", e)
        } catch (e: ManifestMerger2.MergeFailureException) {
            throw RuntimeException("Manifest merging exception", e)
        } finally {
            try {
                if (tempFile1 != null) {
                    FileUtils.delete(tempFile1!!)
                }
                if (tempFile2 != null) {
                    FileUtils.delete(tempFile2!!)
                }
            } catch (e: IOException) {
                logger.error(e, "Unable to clean up the temporary files.")
            }
        }
    }

    @Throws(IOException::class)
    private fun handleMergingResult(
        mergingReport: MergingReport, outFile: File, logger: ILogger
    ) {
        outputMergeBlameContents(
            mergingReport,
            mergeBlameFile.get().asFile
        )
        if (mergingReport.result == MergingReport.Result.ERROR) {
            mergingReport.log(logger)
            throw RuntimeException(mergingReport.reportString)
        }
        if (mergingReport.result == MergingReport.Result.WARNING) {
            mergingReport.log(logger)
        }

        val finalMergedDocument =
            mergingReport.getMergedDocument().orNull()?.prettyPrint()
                ?: throw RuntimeException("No result from manifest merger")
        try {
            Files.asCharSink(outFile, Charsets.UTF_8).write(finalMergedDocument)
        } catch (e: IOException) {
            logger.error(e, "Cannot write resulting xml")
            throw RuntimeException(e)
        }
        logger.verbose("Merged manifest saved to $outFile")
    }

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val testManifestFile: RegularFileProperty

    @get:Input
    abstract val testApplicationId: Property<String>

    @get:Input
    abstract val testedApplicationId: Property<String>

    @get:Input
    abstract val namespace: Property<String>

    @get:Input
    abstract val minSdkVersion: Property<String>

    @get:Input
    abstract val targetSdkVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val instrumentationRunner: Property<String>

    @get:Input
    @get:Optional
    abstract val handleProfiling: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val functionalTest: Property<Boolean>

    @get:Input
    abstract val componentType: Property<String>

    @get:Optional
    @get:Input
    abstract val testLabel: Property<String?>

    @get:Input
    abstract val placeholdersValues: MapProperty<String, Any>

    @get:Optional
    @get:Input
    abstract val jniLibsUseLegacyPackaging: Property<Boolean>

    @get:Input
    abstract val debuggable: Property<Boolean>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val manifestOverlays: ListProperty<File>

    private fun computeProviders(): List<ManifestProvider?> {
        return manifests?.artifacts?.map { ManifestProviderImpl(it.file, getArtifactName(it)) } ?: emptyList()
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getManifests(): FileCollection? {
        return manifests?.artifactFiles
    }

    class CreationAction(
        creationConfig: TestCreationConfig
    ) : VariantTaskCreationAction<ProcessTestManifest, TestCreationConfig>(creationConfig) {
        override val name = computeTaskName("process", "Manifest")
        override val type = ProcessTestManifest::class.java

        override fun preConfigure(taskName: String) {
            super.preConfigure(taskName)
            creationConfig
                .artifacts
                .republish(
                    PACKAGED_MANIFESTS,
                    InternalArtifactType.MANIFEST_METADATA
                )
        }

        override fun handleProvider(
            taskProvider: TaskProvider<ProcessTestManifest>
        ) {
            super.handleProvider(taskProvider)
            creationConfig.taskContainer.processManifestTask = taskProvider
            creationConfig.artifacts.setInitialProvider(
                taskProvider,
                ProcessTestManifest::packagedManifestOutputDirectory
            ).on(PACKAGED_MANIFESTS)
            creationConfig.artifacts.setInitialProvider(
                taskProvider,
                ProcessTestManifest::mergeBlameFile
            ).withName("manifest-merger-blame-" + creationConfig.baseName + "-report.txt")
                .on(InternalArtifactType.MANIFEST_MERGE_BLAME_FILE)
        }

        override fun configure(
            task: ProcessTestManifest
        ) {
            super.configure(task)
            val project = task.project
            val variantSources = creationConfig.variantSources
            task.testManifestFile
                .fileProvider(project.provider(variantSources::mainManifestFilePath))
            
            creationConfig.manifestPlaceholdersCreationConfig?.placeholders?.let {
                task.placeholdersValues.putAll(it)
            }
            task.placeholdersValues.disallowChanges()
        }
    }
}
