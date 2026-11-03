package com.tyron.builder.gradle.internal.tasks

import com.android.SdkConstants
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Throwables
import com.tyron.builder.api.artifact.MultipleArtifact
import com.tyron.builder.api.transform.QualifiedContent
import com.tyron.builder.api.variant.impl.getFeatureLevel
import com.tyron.builder.dexing.*
import com.tyron.builder.dexing.DexingType.LEGACY_MULTIDEX
import com.tyron.builder.dexing.DexingType.NATIVE_MULTIDEX
import com.tyron.builder.files.SerializableFileChanges
import com.tyron.builder.gradle.errors.MessageReceiverImpl
import com.tyron.builder.gradle.internal.component.ApkCreationConfig
import com.tyron.builder.gradle.internal.dependency.AndroidAttributes
import com.tyron.builder.gradle.internal.dependency.getDexingArtifactConfiguration
import com.tyron.builder.gradle.internal.publishing.AndroidArtifacts
import com.tyron.builder.gradle.internal.scope.InternalArtifactType
import com.tyron.builder.gradle.internal.scope.InternalMultipleArtifactType
import com.tyron.builder.gradle.internal.tasks.DexMergingAction.*
import com.tyron.builder.gradle.internal.tasks.factory.VariantTaskCreationAction
import com.tyron.builder.gradle.options.BooleanOption
import com.tyron.builder.gradle.options.IntegerOption
import com.tyron.builder.gradle.options.ProjectOptions
import com.tyron.builder.internal.utils.setDisallowChanges
import com.tyron.builder.plugin.options.SyncOptions
import com.tyron.builder.tasks.IncrementalTask
import com.tyron.builder.tasks.toSerializable
import org.gradle.api.GradleException
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.file.*
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import com.tyron.builder.gradle.internal.utils.cleanOutputDir
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import java.nio.file.Path
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Task that merges input dex files into a smaller number of output dex files.
 */
@CacheableTask
abstract class DexMergingTask : IncrementalTask() {

    /**
     * Parameters shared between [DexMergingTask], [DexMergingTaskDelegate], and
     * [DexMergingWorkAction].
     */
    abstract class SharedParams {

        @get:Input
        abstract val dexingType: Property<DexingType>

        @get:Input
        abstract val minSdkVersion: Property<Int>

        @get:Input
        abstract val debuggable: Property<Boolean>

        @get:Internal
        abstract val errorFormatMode: Property<SyncOptions.ErrorFormatMode>

        @get:Nested
        abstract val mainDexListConfig: MainDexListConfig

        /**
         * Parameters for generating main dex list and merging dex files in [LEGACY_MULTIDEX] mode.
         */
        abstract class MainDexListConfig {

            @get:Optional
            @get:InputFile
            @get:PathSensitive(PathSensitivity.NONE)
            abstract val aaptGeneratedRules: RegularFileProperty

            @get:Optional
            @get:InputFiles
            @get:PathSensitive(PathSensitivity.NONE)
            abstract val userMultidexProguardRules: ListProperty<RegularFile>

            @get:Optional
            @get:Input
            abstract val platformMultidexProguardRules: ListProperty<String>

            @get:Optional
            @get:InputFile
            @get:PathSensitive(PathSensitivity.NONE)
            abstract val userMultidexKeepFile: Property<File>

            @get:Optional
            @get:Classpath
            abstract val libraryClasses: ConfigurableFileCollection
        }
    }

    @get:Nested
    abstract val sharedParams: SharedParams

    @get:Input
    abstract val numberOfBuckets: Property<Int>

    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    lateinit var dexDirs: FileCollection
        private set

    @get:Incremental
    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fileDependencyDexDir: DirectoryProperty

    // Fake folder, used as a way to set up dependency
    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val duplicateClassesCheck: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Optional
    @get:OutputFile
    abstract val mainDexListOutput: RegularFileProperty

    override fun doTaskAction(inputChanges: InputChanges) {
        val dexJars = (dexDirs + listOfNotNull(fileDependencyDexDir.orNull?.asFile)).flatMap { dir ->
            check(dir.isDirectory) { "Directory does not exist: ${dir.path}" }
            dir.listFiles()!!.filter { isJarFile(it) }.sorted()
        }
        val dexDirsOrJars = dexDirs + dexJars

        val fileChanges = if (inputChanges.isIncremental) {
            inputChanges.getFileChanges(dexDirs) + inputChanges.getFileChanges(fileDependencyDexDir)
        } else {
            null
        }

        @Suppress("UnstableApiUsage")
        workerExecutor.noIsolation().submit(DexMergingTaskDelegate::class.java) {
            it.initialize(
                sharedParams, numberOfBuckets.get(), dexDirsOrJars.toList(), outputDir, inputChanges.isIncremental,
                fileChanges?.toSerializable(),
                mainDexListOutput = mainDexListOutput
            )
        }
    }

    class CreationAction @JvmOverloads constructor(
        creationConfig: ApkCreationConfig,
        private val action: DexMergingAction,
        private val dexingType: DexingType,
        private val dexingUsingArtifactTransforms: Boolean = true,
        private val separateFileDependenciesDexingTask: Boolean = false,
        private val outputType: InternalMultipleArtifactType<Directory> = InternalMultipleArtifactType.DEX
    ) : VariantTaskCreationAction<DexMergingTask, ApkCreationConfig>(creationConfig) {

        private val internalName: String = when (action) {
            MERGE_LIBRARY_PROJECTS -> creationConfig.computeTaskName("mergeLibDex")
            MERGE_EXTERNAL_LIBS -> creationConfig.computeTaskName("mergeExtDex")
            MERGE_PROJECT -> creationConfig.computeTaskName("mergeProjectDex")
            MERGE_ALL -> creationConfig.computeTaskName("mergeDex")
            MERGE_TRANSFORMED_CLASSES -> creationConfig.computeTaskName("mergeDex")
        }

        override val name = internalName
        override val type = DexMergingTask::class.java

        override fun handleProvider(taskProvider: TaskProvider<DexMergingTask>) {
            super.handleProvider(taskProvider)
            creationConfig.artifacts
                .use(taskProvider)
                .wiredWith(DexMergingTask::outputDir)
                .toAppendTo(outputType)

            if (dexingType === LEGACY_MULTIDEX) {
                creationConfig
                    .artifacts
                    .setInitialProvider(taskProvider, DexMergingTask::mainDexListOutput)
                    .withName("mainDexList.txt")
                    .on(InternalArtifactType.LEGACY_MULTIDEX_MAIN_DEX_LIST)
            }
        }

        override fun configure(task: DexMergingTask) {
            super.configure(task)

            val projectOptions = creationConfig.services.projectOptions

            task.sharedParams.dexingType.setDisallowChanges(dexingType)
            task.sharedParams.minSdkVersion.setDisallowChanges(
                creationConfig.minSdkVersionForDexing.getFeatureLevel()
            )
            task.sharedParams.debuggable.setDisallowChanges(creationConfig.debuggable)
            task.sharedParams.errorFormatMode.setDisallowChanges(
                SyncOptions.getErrorFormatMode(projectOptions)
            )
            if (dexingType === LEGACY_MULTIDEX) {
                creationConfig.artifacts.setTaskInputToFinalProduct(
                    InternalArtifactType.LEGACY_MULTIDEX_AAPT_DERIVED_PROGUARD_RULES,
                    task.sharedParams.mainDexListConfig.aaptGeneratedRules
                )

                task.sharedParams.mainDexListConfig.userMultidexProguardRules.setDisallowChanges(
                    creationConfig.artifacts.getAll(MultipleArtifact.MULTIDEX_KEEP_PROGUARD)
                )

                creationConfig.multiDexKeepFile?.let {
                    task.sharedParams.mainDexListConfig.userMultidexKeepFile.setDisallowChanges(it)
                }

                task.sharedParams.mainDexListConfig.platformMultidexProguardRules
                    .setDisallowChanges(getPlatformRules())

                val libraryScopes = setOf(
                    QualifiedContent.Scope.PROVIDED_ONLY,
                    QualifiedContent.Scope.TESTED_CODE
                )
                val libraryClasses = creationConfig.transformManager
                    .getPipelineOutputAsFileCollection { contentTypes, scopes ->
                        contentTypes.contains(
                            QualifiedContent.DefaultContentType.CLASSES
                        ) && libraryScopes.intersect(scopes).isNotEmpty()
                    }

                val bootClasspath = creationConfig.global.bootClasspath
                task.sharedParams.mainDexListConfig.libraryClasses
                    .from(bootClasspath, libraryClasses).disallowChanges()
            }

            task.numberOfBuckets.setDisallowChanges(
                task.project.providers.provider { getNumberOfBuckets(projectOptions) }
            )

            task.dexDirs = getDexDirs(creationConfig, action)
            if (separateFileDependenciesDexingTask) {
                creationConfig.artifacts.setTaskInputToFinalProduct(
                    InternalArtifactType.EXTERNAL_FILE_LIB_DEX_ARCHIVES,
                    task.fileDependencyDexDir
                )
            }
            if (projectOptions[BooleanOption.ENABLE_DUPLICATE_CLASSES_CHECK]) {
                creationConfig.artifacts.setTaskInputToFinalProduct(
                    InternalArtifactType.DUPLICATE_CLASSES_CHECK,
                    task.duplicateClassesCheck
                )
            }
        }

        private fun getDexDirs(
            creationConfig: ApkCreationConfig,
            action: DexMergingAction
        ): FileCollection {
            val attributes = getDexingArtifactConfiguration(creationConfig).getAttributes()

            fun forAction(action: DexMergingAction): FileCollection {
                when (action) {
                    MERGE_EXTERNAL_LIBS -> {
                        return if (dexingUsingArtifactTransforms) {
                            val artifactScope: AndroidArtifacts.ArtifactScope =
                                if (separateFileDependenciesDexingTask) {
                                    AndroidArtifacts.ArtifactScope.REPOSITORY_MODULE
                                } else {
                                    AndroidArtifacts.ArtifactScope.EXTERNAL
                                }
                            creationConfig.variantDependencies.getArtifactFileCollection(
                                AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                                artifactScope,
                                AndroidArtifacts.ArtifactType.DEX,
                                attributes
                            )
                        } else {
                            creationConfig.services.fileCollection(
                                creationConfig.artifacts.get(InternalArtifactType.EXTERNAL_LIBS_DEX_ARCHIVE),
                                creationConfig.artifacts.get(InternalArtifactType.EXTERNAL_LIBS_DEX_ARCHIVE_WITH_ARTIFACT_TRANSFORMS)
                            )
                        }
                    }
                    MERGE_LIBRARY_PROJECTS -> {
                        return if (dexingUsingArtifactTransforms) {
                            val classesLibraryElements =
                                creationConfig.services.named(
                                    LibraryElements::class.java,
                                    LibraryElements.CLASSES
                                )
                            check(attributes.libraryElementsAttribute == null)
                            val updatedAttributes = AndroidAttributes(
                                attributes.stringAttributes, classesLibraryElements
                            )
                            creationConfig.variantDependencies.getArtifactFileCollection(
                                AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                                AndroidArtifacts.ArtifactScope.PROJECT,
                                AndroidArtifacts.ArtifactType.DEX,
                                updatedAttributes
                            )
                        } else {
                            creationConfig.services.fileCollection(
                                creationConfig.artifacts.get(InternalArtifactType.SUB_PROJECT_DEX_ARCHIVE)
                            )
                        }
                    }
                    MERGE_PROJECT -> {
                        return creationConfig.services.fileCollection(
                            creationConfig.artifacts.get(InternalArtifactType.PROJECT_DEX_ARCHIVE),
                            creationConfig.artifacts.get(InternalArtifactType.MIXED_SCOPE_DEX_ARCHIVE)
                        )
                    }
                    MERGE_ALL -> {
                        return creationConfig.services.fileCollection(
                            forAction(MERGE_PROJECT),
                            forAction(MERGE_LIBRARY_PROJECTS),
                            if (dexingType == LEGACY_MULTIDEX) {
                                forAction(MERGE_EXTERNAL_LIBS)
                            } else {
                                creationConfig.artifacts.getAll(InternalMultipleArtifactType.EXTERNAL_LIBS_DEX)
                            }
                        )
                    }
                    MERGE_TRANSFORMED_CLASSES -> {
                        return creationConfig.services.fileCollection(
                            creationConfig.artifacts.get(InternalArtifactType.PROJECT_DEX_ARCHIVE),
                        )
                    }
                }
            }

            return forAction(action)
        }

        private fun getNumberOfBuckets(projectOptions: ProjectOptions): Int {
            return when (action) {
                MERGE_ALL, MERGE_EXTERNAL_LIBS, MERGE_TRANSFORMED_CLASSES -> 1 // No bucketing
                MERGE_PROJECT, MERGE_LIBRARY_PROJECTS -> {
                    check(dexingType == NATIVE_MULTIDEX)

                    val customNumberOfBuckets =
                        projectOptions.getProvider(IntegerOption.DEXING_NUMBER_OF_BUCKETS).orNull
                    if (customNumberOfBuckets != null) {
                        check(customNumberOfBuckets >= 1) {
                            "The value of ${IntegerOption.DEXING_NUMBER_OF_BUCKETS.propertyName}" +
                                    " is invalid (must be >= 1): $customNumberOfBuckets"
                        }
                        return customNumberOfBuckets
                    }

                    val targetDeployApi = creationConfig.minSdkVersionForDexing.getFeatureLevel()
                    val overrideMinSdkVersion = max(21, targetDeployApi)
                    getNumberOfBuckets(minSdkVersion = overrideMinSdkVersion)
                }
            }
        }

        private fun getNumberOfBuckets(minSdkVersion: Int): Int {
            return min(
                getMaxNumberOfBucketsBasedOnDexFileLimit(minSdkVersion),
                getRecommendedNumberOfBucketsBasedOnWorkers()
            )
        }

        private fun getMaxNumberOfBucketsBasedOnDexFileLimit(minSdkVersion: Int): Int {
            check(minSdkVersion >= 21) {
                "Expected minSdkVersion >= 21 but found $minSdkVersion"
            }
            return when (minSdkVersion) {
                21, 22 -> 20
                else -> 200
            }
        }

        private fun getRecommendedNumberOfBucketsBasedOnWorkers(): Int {
            return 16
        }
    }
}

enum class DexMergingAction {
    MERGE_EXTERNAL_LIBS,
    MERGE_LIBRARY_PROJECTS,
    MERGE_PROJECT,
    MERGE_ALL,
    MERGE_TRANSFORMED_CLASSES,
}

@VisibleForTesting
abstract class DexMergingTaskDelegate : WorkAction<DexMergingTaskDelegate.Params> {

    abstract class Params : WorkParameters {

        abstract val sharedParams: Property<DexMergingTask.SharedParams>
        abstract val numberOfBuckets: Property<Int>
        abstract val dexDirsOrJars: ListProperty<File>
        abstract val outputDir: DirectoryProperty
        abstract val mainDexListOutput: RegularFileProperty

        abstract val incremental: Property<Boolean>
        abstract val fileChanges: Property<SerializableFileChanges>

        fun initialize(
            sharedParams: DexMergingTask.SharedParams,
            numberOfBuckets: Int,
            dexDirsOrJars: List<File>,
            outputDir: DirectoryProperty,
            incremental: Boolean,
            fileChanges: SerializableFileChanges?,
            mainDexListOutput: RegularFileProperty?
        ) {
            this.sharedParams.set(sharedParams)
            this.numberOfBuckets.set(numberOfBuckets)
            this.dexDirsOrJars.set(dexDirsOrJars)
            this.outputDir.set(outputDir)
            this.incremental.set(incremental)
            this.fileChanges.set(fileChanges)
            mainDexListOutput?.let { this.mainDexListOutput.set(it) }
        }
    }

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    override fun execute() {
        @Suppress("UnstableApiUsage")
        with(parameters) {
            val buckets =
                getBucketsToMerge(dexDirsOrJars.get(),
                    numberOfBuckets.get(),
                    incremental.get(),
                    fileChanges.orNull)

            val workQueue = workerExecutor.noIsolation()
            for ((bucketNumber, bucket) in buckets) {
                val outputDirForBucket = if (numberOfBuckets.get() == 1) {
                    outputDir.get().asFile
                } else {
                    outputDir.get().asFile.resolve(bucketNumber.toString())
                }
                outputDirForBucket.cleanOutputDir()

                workQueue.submit(DexMergingWorkAction::class.java) {
                    it.initialize(
                        sharedParams = sharedParams,
                        useForkJoinPool = numberOfBuckets.get() == 1,
                        dexEntryBucket = bucket,
                        outputDirForBucket = outputDirForBucket,
                        mainDexListOutput = mainDexListOutput.asFile.orNull
                    )
                }
            }
        }
    }

    companion object {

        private val isDexFile: (relativePath: String) -> Boolean =
            { it.endsWith(SdkConstants.DOT_DEX, ignoreCase = true) }

        private fun getBucketsToMerge(
            dexDirsOrJars: List<File>,
            numberOfBuckets: Int,
            incremental: Boolean,
            fileChanges: SerializableFileChanges?
        ): Map<Int, DexEntryBucket> {
            val bucketsToMerge = if (incremental) {
                getImpactedBuckets(fileChanges!!, numberOfBuckets)
            } else {
                (0 until numberOfBuckets).toSet()
            }

            val bucketMap = mutableMapOf<Int, MutableList<DexEntry>>()
            for (bucketNumber in bucketsToMerge) {
                bucketMap[bucketNumber] = mutableListOf()
            }

            for (dexDirOrJar in dexDirsOrJars) {
                val dexEntryRelativePaths = if (dexDirOrJar.isDirectory) {
                    getSortedFilesInDir(dexDirOrJar.toPath(), isDexFile).map {
                        dexDirOrJar.toPath().relativize(it).toString()
                    }
                } else {
                    getSortedRelativePathsInJar(dexDirOrJar, isDexFile)
                }
                for (relativePath in dexEntryRelativePaths) {
                    val bucketNumber = getBucketNumber(relativePath, numberOfBuckets)
                    if (bucketNumber in bucketsToMerge) {
                        bucketMap[bucketNumber]!!.add(DexEntry(dexDirOrJar, relativePath))
                    }
                }
            }

            return bucketMap.map { it.key to DexEntryBucket(it.value) }.toMap()
        }

        private fun getImpactedBuckets(
            fileChanges: SerializableFileChanges,
            numberOfBuckets: Int
        ): Set<Int> {
            val hasModifiedRemovedJars =
                (fileChanges.modifiedFiles + fileChanges.removedFiles)
                    .find { isJarFile(it.file) } != null
            if (hasModifiedRemovedJars) {
                return (0 until numberOfBuckets).toSet()
            }

            val addedJars = (fileChanges.addedFiles).map { it.file }.filter { isJarFile(it) }
            val relativePathsOfDexFilesInAddedJars =
                addedJars.flatMap { getSortedRelativePathsInJar(it, isDexFile) }
            val relativePathsOfChangedDexFilesInDirs =
                fileChanges.fileChanges.map { it.normalizedPath }.filter { isDexFile(it) }

            return (relativePathsOfDexFilesInAddedJars + relativePathsOfChangedDexFilesInDirs)
                .map { getBucketNumber(it, numberOfBuckets) }.toSet()
        }

        @VisibleForTesting
        fun getBucketNumber(relativePath: String, numberOfBuckets: Int): Int {
            check(!File(relativePath).isAbsolute) {
                "Expected relative path but found absolute path: $relativePath"
            }
            check(relativePath.endsWith(SdkConstants.DOT_DEX, ignoreCase = true)) {
                "Expected .dex file but found: $relativePath"
            }

            val packagePath = File(relativePath).parent
            return if (packagePath.isNullOrEmpty()) {
                0
            } else {
                when (numberOfBuckets) {
                    1 -> 0
                    else -> {
                        val normalizedPackagePath = File(packagePath).invariantSeparatorsPath
                        return kotlin.math.abs(normalizedPackagePath.hashCode()) % (numberOfBuckets - 1) + 1
                    }
                }
            }
        }
    }
}

@VisibleForTesting
abstract class DexMergingWorkAction : WorkAction<DexMergingWorkAction.Params> {

    abstract class Params : WorkParameters {

        abstract val sharedParams: Property<DexMergingTask.SharedParams>
        abstract val useForkJoinPool: Property<Boolean>
        abstract val dexEntryBucket: Property<DexEntryBucket>
        abstract val outputDirForBucket: DirectoryProperty
        abstract val mainDexListOutput: RegularFileProperty

        fun initialize(
            sharedParams: Property<DexMergingTask.SharedParams>,
            useForkJoinPool: Boolean,
            dexEntryBucket: DexEntryBucket,
            outputDirForBucket: File,
            mainDexListOutput: File?
        ) {
            this.sharedParams.set(sharedParams)
            this.useForkJoinPool.set(useForkJoinPool)
            this.dexEntryBucket.set(dexEntryBucket)
            this.outputDirForBucket.set(outputDirForBucket)
            this.mainDexListOutput.set(mainDexListOutput)
        }
    }

    @Suppress("UnstableApiUsage")
    override fun execute() {
        val dexArchiveEntries = parameters.dexEntryBucket.get().getDexEntriesWithContents()
        if (dexArchiveEntries.isEmpty()) {
            return
        }

        val forkJoinPool = if (parameters.useForkJoinPool.get()) {
            ForkJoinPool()
        } else {
            null
        }
        try {
            merge(
                parameters.sharedParams.get(),
                forkJoinPool,
                dexArchiveEntries,
                parameters.outputDirForBucket.get().asFile,
                parameters.mainDexListOutput.asFile.orNull?.toPath()
            )
        } finally {
            forkJoinPool?.shutdown()
            forkJoinPool?.awaitTermination(100, TimeUnit.SECONDS)
        }
    }

    private fun merge(
        sharedParams: DexMergingTask.SharedParams,
        forkJoinPool: ForkJoinPool?,
        dexArchiveEntries: List<DexArchiveEntry>,
        outputDir: File,
        mainDexListOutput: Path?
    ) {
        val logger = Logging.getLogger(DexMergingWorkAction::class.java)
        val messageReceiver = MessageReceiverImpl(
            sharedParams.errorFormatMode.get(),
            Logging.getLogger(DexMergingTask::class.java)
        )

        try {
            var d8MinSdkVersion: Int = sharedParams.minSdkVersion.get()
            val dexingType = sharedParams.dexingType.get()
            if (d8MinSdkVersion < 21 && dexingType === NATIVE_MULTIDEX) {
                d8MinSdkVersion = 21
            }
            val merger = DexArchiveMerger.createD8DexMerger(
                messageReceiver,
                d8MinSdkVersion,
                sharedParams.debuggable.get(),
                forkJoinPool
            )

            val proguardRules = mutableListOf<Path>()
            sharedParams.mainDexListConfig.aaptGeneratedRules.asFile.orNull?.let {
                proguardRules.add(it.toPath())
            }
            sharedParams.mainDexListConfig.userMultidexProguardRules.get()
                .asSequence()
                .map(RegularFile::getAsFile)
                .map(File::toPath)
                .forEach { proguardRules.add(it) }
            merger.mergeDexArchives(
                dexArchiveEntries,
                outputDir.toPath(),
                proguardRules,
                sharedParams.mainDexListConfig.platformMultidexProguardRules.orNull,
                sharedParams.mainDexListConfig.userMultidexKeepFile.orNull?.toPath(),
                sharedParams.mainDexListConfig.libraryClasses.map { it.toPath() },
                mainDexListOutput
            )
        } catch (e: Exception) {
            logger.error(Throwables.getStackTraceAsString(e))
            throw GradleException(e.message?:"")
        }
    }
}
