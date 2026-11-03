package com.tyron.builder.gradle.internal.tasks

import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.hash.Hashing
import com.google.common.io.Closer
import com.tyron.builder.dexing.*
import com.tyron.builder.dexing.r8.ClassFileProviderFactory
import com.tyron.builder.internal.dexing.DexParameters
import com.tyron.builder.internal.dexing.DexWorkAction
import com.tyron.builder.internal.dexing.IncrementalDexSpec
import com.tyron.builder.gradle.internal.workeractions.WorkerActionServiceRegistry
import com.android.SdkConstants
import com.tyron.builder.gradle.internal.utils.deleteIfExists
import com.tyron.builder.gradle.internal.utils.cleanOutputDir
import org.gradle.api.file.FileType
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.work.ChangeType
import org.gradle.work.FileChange
import org.gradle.workers.WorkerExecutor
import java.io.BufferedInputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Path
import java.util.ArrayList

/**
 * Delegate for the [DexArchiveBuilderTask].
 */
class DexArchiveBuilderTaskDelegate(
    isIncremental: Boolean,
    private val projectClasses: Set<File>,
    private val projectChangedClasses: Set<FileChange> = emptySet(),
    private val subProjectClasses: Set<File>,
    private val subProjectChangedClasses: Set<FileChange> = emptySet(),
    private val externalLibClasses: Set<File>,
    private val externalLibChangedClasses: Set<FileChange> = emptySet(),
    private val mixedScopeClasses: Set<File>,
    private val mixedScopeChangedClasses: Set<FileChange> = emptySet(),
    private val projectOutputs: DexingOutputs,
    private val subProjectOutputs: DexingOutputs?,
    private val externalLibsOutputs: DexingOutputs?,
    private val mixedScopeOutputs: DexingOutputs?,
    private val dexParams: DexParameters,
    private val desugarClasspathChangedClasses: Set<FileChange> = emptySet(),
    private val desugarGraphDir: File?,
    private val inputJarHashesFile: File,
    private val numberOfBuckets: Int,
    private val workerExecutor: WorkerExecutor,
    private val projectPath: Provider<String>,
    private val taskPath: String,
) {
    private val outputMapping = OutputMapping(isIncremental)

    private val isIncremental =
        isIncremental && projectOutputs.keepRules == null && subProjectOutputs?.keepRules == null
                && externalLibsOutputs?.keepRules == null && mixedScopeOutputs?.keepRules == null
                && outputMapping.canProcessIncrementally

    private val changedFiles =
        with(
            HashSet<File>(
                projectChangedClasses.size +
                        subProjectChangedClasses.size +
                        externalLibChangedClasses.size +
                        mixedScopeChangedClasses.size +
                        desugarClasspathChangedClasses.size
            )
        ) {
            addAll(projectChangedClasses.map { it.file })
            addAll(subProjectChangedClasses.map { it.file })
            addAll(externalLibChangedClasses.map { it.file })
            addAll(mixedScopeChangedClasses.map { it.file })
            addAll(desugarClasspathChangedClasses.map { it.file })
            this
        }

    init {
        check(dexParams.withDesugaring xor (desugarGraphDir == null))
    }

    data class ClasspathServiceKey(private val id: Long) :
        WorkerActionServiceRegistry.ServiceKey<ClassFileProviderFactory> {
        override val type = ClassFileProviderFactory::class.java
    }

    companion object {
        internal val sharedState = WorkerActionServiceRegistry()
    }

    fun doProcess() {
        try {
            Closer.create().use { closer ->
                val classpath = getClasspath(dexParams.withDesugaring)
                val bootclasspath =
                    getBootClasspath(dexParams.desugarBootclasspath, dexParams.withDesugaring)

                val bootClasspathProvider = ClassFileProviderFactory(bootclasspath)
                closer.register(bootClasspathProvider)
                val libraryClasspathProvider = ClassFileProviderFactory(classpath)
                closer.register(libraryClasspathProvider)

                val bootclasspathServiceKey = ClasspathServiceKey(bootClasspathProvider.id)
                val classpathServiceKey = ClasspathServiceKey(libraryClasspathProvider.id)

                sharedState.registerServiceAsCloseable(
                    bootclasspathServiceKey, bootClasspathProvider
                ).also { closer.register(it) }

                sharedState.registerServiceAsCloseable(
                    classpathServiceKey, libraryClasspathProvider
                ).also { closer.register(it) }

                val processInputType = { classes: Set<File>,
                                         changedClasses: Set<FileChange>,
                                         outputDir: File,
                                         outputKeepRules: File?,
                                         desugarGraphDir: File? ->
                    processClassFromInput(
                        inputFiles = classes,
                        inputFileChanges = changedClasses,
                        outputDir = outputDir,
                        outputKeepRules = outputKeepRules,
                        desugarGraphDir = desugarGraphDir,
                        bootClasspathKey = bootclasspathServiceKey,
                        classpathKey = classpathServiceKey
                    )
                }

                processInputType(
                    projectClasses,
                    projectChangedClasses,
                    projectOutputs.dex,
                    projectOutputs.keepRules,
                    desugarGraphDir?.resolve("currentProject")
                )
                subProjectOutputs?.let {
                    processInputType(
                        subProjectClasses,
                        subProjectChangedClasses,
                        subProjectOutputs.dex,
                        subProjectOutputs.keepRules,
                        desugarGraphDir?.resolve("otherProjects")
                    )
                }
                mixedScopeOutputs?.let {
                    processInputType(
                        mixedScopeClasses,
                        mixedScopeChangedClasses,
                        mixedScopeOutputs.dex,
                        mixedScopeOutputs.keepRules,
                        desugarGraphDir?.resolve("mixedScopes")
                    )
                }
                externalLibsOutputs?.let {
                    processInputType(
                        externalLibClasses,
                        externalLibChangedClasses,
                        externalLibsOutputs.dex,
                        externalLibsOutputs.keepRules,
                        desugarGraphDir?.resolve("externalLibs")
                    )
                }

                workerExecutor.await()
            }
        } catch (e: Exception) {
            loggerWrapper.error(Throwables.getStackTraceAsString(e))
            throw e
        }
    }

    private fun processClassFromInput(
        inputFiles: Set<File>,
        inputFileChanges: Set<FileChange>,
        outputDir: File,
        outputKeepRules: File?,
        desugarGraphDir: File?,
        bootClasspathKey: ClasspathServiceKey,
        classpathKey: ClasspathServiceKey
    ) {
        if (!isIncremental) {
            outputDir.cleanOutputDir()
            outputKeepRules?.cleanOutputDir()
            desugarGraphDir?.cleanOutputDir()
        } else {
            removeChangedJarOutputs(inputFileChanges, outputDir)
            deletePreviousOutputsFromDirs(inputFileChanges, outputDir)
        }

        val (directoryInputs, jarInputs) =
            inputFiles
                .filter { it.exists() }
                .partition { it.isDirectory }

        if (directoryInputs.isNotEmpty()) {
            convertToDexArchive(
                inputs = DirectoryBucketGroup(directoryInputs, numberOfBuckets),
                outputDir = outputDir,
                isIncremental = isIncremental,
                bootClasspath = bootClasspathKey,
                classpath = classpathKey,
                changedFiles = changedFiles,
                desugarGraphDir = desugarGraphDir,
                outputKeepRulesDir = outputKeepRules
            )
        }

        for (input in jarInputs) {
            check(input.extension == SdkConstants.EXT_JAR) { "Expected jar, received $input" }

            convertJarToDexArchive(
                isIncremental = isIncremental,
                jarInput = input,
                outputDir = outputDir,
                bootclasspath = bootClasspathKey,
                classpath = classpathKey,
                changedFiles = changedFiles,
                desugarGraphDir = desugarGraphDir,
                outputKeepRulesDir = outputKeepRules
            )
        }
    }

    private fun convertJarToDexArchive(
        isIncremental: Boolean,
        jarInput: File,
        outputDir: File,
        bootclasspath: ClasspathServiceKey,
        classpath: ClasspathServiceKey,
        changedFiles: Set<File>,
        desugarGraphDir: File?,
        outputKeepRulesDir: File?
    ) {
        if (dexParams.withDesugaring) {
            convertToDexArchive(
                inputs = JarBucketGroup(jarInput, numberOfBuckets),
                outputDir = outputDir,
                isIncremental = isIncremental,
                bootClasspath = bootclasspath,
                classpath = classpath,
                changedFiles = changedFiles,
                desugarGraphDir = desugarGraphDir!!,
                outputKeepRulesDir = outputKeepRulesDir
            )
        } else {
            if (isIncremental && jarInput !in changedFiles) {
                return
            }

            convertToDexArchive(
                inputs = JarBucketGroup(jarInput, numberOfBuckets),
                outputDir = outputDir,
                isIncremental = false,
                bootClasspath = bootclasspath,
                classpath = classpath,
                changedFiles = setOf(),
                desugarGraphDir = null,
                outputKeepRulesDir = outputKeepRulesDir
            )
        }
    }

    private fun deletePreviousOutputsFromDirs(inputFileChanges: Set<FileChange>, output: File) {
        inputFileChanges.forEach {
            if (it.changeType == ChangeType.REMOVED) {
                val fileOrDirToDelete: File? = when {
                    it.fileType == org.gradle.api.file.FileType.DIRECTORY -> output.resolve(it.normalizedPath)
                    ClassFileInput.CLASS_MATCHER.test(it.normalizedPath) -> output.resolve(ClassFileEntry.withDexExtension(it.normalizedPath))
                    else -> null
                }
                fileOrDirToDelete?.deleteIfExists()
            }
        }
    }

    private fun removeChangedJarOutputs(changes: Set<FileChange>, output: File) {
        changes.filter { it.file.extension == SdkConstants.EXT_JAR }.forEach {
            outputMapping.getPreviousDexOutputsForJar(it.file, output).forEach { file ->
                file.deleteIfExists()
            }
        }
    }

    class DexingOutputs(
        val dex: File,
        val keepRules: File?
    ) {
        constructor(outputs: DexArchiveBuilderTask.DexingOutputs)
                : this(outputs.dex.asFile.get(), outputs.keepRules.asFile.orNull)
    }

    private fun convertToDexArchive(
        inputs: ClassBucketGroup,
        outputDir: File,
        isIncremental: Boolean,
        bootClasspath: ClasspathServiceKey,
        classpath: ClasspathServiceKey,
        changedFiles: Set<File>,
        desugarGraphDir: File?,
        outputKeepRulesDir: File?
    ) {
        for (bucketId in 0 until numberOfBuckets) {
            val dexPerClass = inputs is DirectoryBucketGroup && outputKeepRulesDir == null

            val preDexOutputFile = when (inputs) {
                is DirectoryBucketGroup -> {
                    if (dexPerClass) outputDir else outputDir.resolve(bucketId.toString())
                }
                is JarBucketGroup -> {
                    outputMapping.getDexOutputForJar(inputs.jarFile, outputDir, bucketId)
                }
            }
            if (!preDexOutputFile.exists() && inputs is DirectoryBucketGroup && !dexPerClass) preDexOutputFile.mkdirs()
            else if (inputs is JarBucketGroup && !preDexOutputFile.parentFile.exists()) preDexOutputFile.parentFile.mkdirs()

            val outputKeepRuleFile = outputKeepRulesDir?.let { outputKeepRuleDir ->
                when (inputs) {
                    is DirectoryBucketGroup -> outputKeepRuleDir.resolve(bucketId.toString())
                    is JarBucketGroup -> getKeepRulesOutputForJar(inputs.jarFile, outputKeepRuleDir, bucketId)
                }.also {
                    if (!it.parentFile.exists()) it.parentFile.mkdirs()
                    it.createNewFile()
                }
            }

            val classBucket = ClassBucket(inputs, bucketId)
            workerExecutor.noIsolation().submit(DexWorkAction::class.java) { params ->
                params.dexSpec.set(
                    IncrementalDexSpec(
                        inputClassFiles = classBucket,
                        outputPath = preDexOutputFile,
                        dexParams = dexParams.toDexParametersForWorkers(
                            dexPerClass,
                            bootClasspath,
                            classpath,
                            outputKeepRuleFile
                        ),
                        isIncremental = isIncremental,
                        changedFiles = changedFiles,
                        desugarGraphFile = desugarGraphDir?.let {
                            getDesugarGraphFile(it, classBucket)
                        }
                    ))
            }
        }
    }

    private fun getClasspath(withDesugaring: Boolean): List<Path> {
        if (!withDesugaring) return emptyList()
        val list = mutableListOf<Path>()
        list.addAll(projectClasses.map { it.toPath() })
        list.addAll(subProjectClasses.map { it.toPath() })
        list.addAll(externalLibClasses.map { it.toPath() })
        list.addAll(mixedScopeClasses.map { it.toPath() })
        list.addAll(dexParams.desugarClasspath.map { it.toPath() })
        return list
    }

    private fun getBootClasspath(androidJarClasspath: List<File>, withDesugaring: Boolean): List<Path> {
        return if (!withDesugaring) emptyList() else androidJarClasspath.map { it.toPath() }
    }

    private fun getKeepRulesOutputForJar(input: File, outputDir: File, bucketId: Int): File {
        val hash = outputMapping.getCurrentHash(input)
        return outputDir.resolve("${hash}_$bucketId")
    }

    private fun getDesugarGraphFile(desugarGraphDir: File, classBucket: ClassBucket): File {
        return when (classBucket.bucketGroup) {
            is DirectoryBucketGroup -> File(desugarGraphDir, "dirs_bucket_${classBucket.bucketNumber}/graph.bin")
            is JarBucketGroup -> {
                val jarFilePath = (classBucket.bucketGroup as JarBucketGroup).jarFile.path
                File(desugarGraphDir, "jar_${Hashing.sha256().hashUnencodedChars(jarFilePath)}_bucket_${classBucket.bucketNumber}/graph.bin")
            }
        }
    }

    private inner class OutputMapping(isAbleToRunIncrementally: Boolean) {
        private val currentFileHashes: Map<File, String>
        private val previousFileHashes: Map<File, String>
        val canProcessIncrementally: Boolean

        init {
            val (fileHashes, isPreviousLoaded) = if (!inputJarHashesFile.exists() || !isAbleToRunIncrementally) {
                Pair(mutableMapOf<File, String>(), false)
            } else {
                BufferedInputStream(inputJarHashesFile.inputStream()).use { input ->
                    try {
                        ObjectInputStream(input).use {
                            @Suppress("UNCHECKED_CAST")
                            val previousState = it.readObject() as MutableMap<File, String>
                            if (ifPreviousStateHasAllInputFiles(previousState)) Pair(previousState, true) else Pair(mutableMapOf(), false)
                        }
                    } catch (e: Exception) {
                        Pair(mutableMapOf<File, String>(), false)
                    }
                }
            }
            previousFileHashes = fileHashes.toMap()

            fun getFileHash(file: File): String = file.inputStream().buffered().use {
                Hashing.sha256().hashBytes(it.readBytes()).toString()
            }

            if (isPreviousLoaded) {
                sequenceOf(projectChangedClasses, subProjectChangedClasses, externalLibChangedClasses, mixedScopeChangedClasses).flatten().filter { it.file.extension == SdkConstants.EXT_JAR }.forEach {
                    fileHashes[it.file] = getFileHash(it.file)
                }
            } else {
                getAllFilesToProcess().forEach { fileHashes[it] = getFileHash(it) }
            }
            inputJarHashesFile.deleteIfExists()
            if (!inputJarHashesFile.parentFile.exists()) inputJarHashesFile.parentFile.mkdirs()
            ObjectOutputStream(inputJarHashesFile.outputStream().buffered()).use { it.writeObject(fileHashes) }

            currentFileHashes = fileHashes
            canProcessIncrementally = isPreviousLoaded
        }

        fun getCurrentHash(file: File) = currentFileHashes.getValue(file)
        fun getDexOutputForJar(input: File, outputDir: File, bucketId: Int?): File = computeOutputPath(outputDir, getCurrentHash(input), bucketId)
        fun getPreviousDexOutputsForJar(input: File, outputDir: File): List<File> {
            val hash = previousFileHashes.getValue(input)
            val list = mutableListOf<File>()
            list.add(computeOutputPath(outputDir, hash, null))
            (0 until numberOfBuckets).forEach { list.add(computeOutputPath(outputDir, hash, it)) }
            return list
        }

        private fun ifPreviousStateHasAllInputFiles(previousMapping: Map<File, String>): Boolean {
            val allFilesToProcess = getAllFilesToProcess()
            return previousMapping.size == allFilesToProcess.count() && allFilesToProcess.all { it in previousMapping.keys }
        }

        private fun getAllFilesToProcess() = sequenceOf(projectClasses, subProjectClasses, externalLibClasses, mixedScopeClasses).flatten().filter { it.extension == SdkConstants.EXT_JAR }
        private fun computeOutputPath(outputDir: File, hash: String, bucketId: Int?): File = if (bucketId != null) outputDir.resolve("${hash}_$bucketId.jar") else outputDir.resolve("$hash.jar")
    }
}
private val loggerWrapper = Logging.getLogger(DexArchiveBuilderTask::class.java)
