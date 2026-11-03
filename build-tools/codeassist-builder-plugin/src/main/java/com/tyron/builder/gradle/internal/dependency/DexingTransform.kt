package com.tyron.builder.gradle.internal.dependency

import com.google.common.io.Closer
import com.google.common.io.Files
import com.tyron.builder.api.variant.impl.getFeatureLevel
import com.tyron.builder.common.AndroidVersion
import com.tyron.builder.dexing.*
import com.tyron.builder.dexing.r8.ClassFileProviderFactory
import com.tyron.builder.files.SerializableFileChanges
import com.tyron.builder.gradle.internal.component.ApkCreationConfig
import com.tyron.builder.gradle.internal.component.ComponentCreationConfig
import com.tyron.builder.internal.dexing.readDesugarGraph
import com.tyron.builder.internal.dexing.writeDesugarGraph
import com.tyron.builder.gradle.errors.MessageReceiverImpl
import com.tyron.builder.gradle.internal.publishing.AndroidArtifacts
import com.tyron.builder.gradle.internal.scope.Java8LangSupport
import com.tyron.builder.plugin.options.SyncOptions
import com.tyron.builder.tasks.toSerializable
import com.tyron.builder.gradle.internal.utils.deleteIfExists
import com.tyron.builder.gradle.internal.utils.cleanOutputDir
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.InputArtifactDependencies
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

@CacheableTransform
abstract class BaseDexingTransform<T : BaseDexingTransform.Parameters> : TransformAction<T> {

    interface Parameters : GenericTransformParameters {
        @get:Input
        val minSdkVersion: Property<Int>
        @get:Input
        val debuggable: Property<Boolean>
        @get:Input
        val enableDesugaring: Property<Boolean>
        @get:Classpath
        val bootClasspath: ConfigurableFileCollection
        @get:Internal
        val errorFormat: Property<SyncOptions.ErrorFormatMode>
        @get:Optional
        @get:Input
        val libConfiguration: Property<String>
    }

    @get:Inject
    abstract val inputChanges: InputChanges

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    @get:Incremental
    abstract val primaryInput: Provider<FileSystemLocation>

    protected abstract fun computeClasspathFiles(): List<Path>

    override fun transform(outputs: TransformOutputs) {
        val input = primaryInput.get().asFile
        val outputDir = outputs.dir(Files.getNameWithoutExtension(input.name))
        doTransform(input, outputDir)
    }

    private fun doTransform(inputFile: File, outputDir: File) {
        val outputKeepRulesEnabled =
            parameters.libConfiguration.isPresent && !parameters.debuggable.get()
        val provideIncrementalSupport = !isJarFile(inputFile) && !outputKeepRulesEnabled

        val dexOutputDir =
            if (outputKeepRulesEnabled) {
                outputDir.resolve(DEX_DIR_NAME)
            } else {
                outputDir
            }
        val keepRulesOutputFile =
            if (outputKeepRulesEnabled) {
                outputDir.resolve(KEEP_RULES_FILE_NAME)
            } else null
        val desugarGraphFile =
            if (provideIncrementalSupport) {
                outputDir.resolve("../$DESUGAR_GRAPH_FILE_NAME")
            } else null

        if (provideIncrementalSupport && inputChanges.isIncremental) {
            val classpathChanges = emptyList<FileChange>()
            check(keepRulesOutputFile == null)
            processIncrementally(
                inputFile,
                inputChanges.getFileChanges(primaryInput).toSerializable(),
                classpathChanges.toSerializable(),
                dexOutputDir,
                desugarGraphFile!!
            )
        } else {
            processNonIncrementally(
                inputFile,
                dexOutputDir,
                keepRulesOutputFile,
                provideIncrementalSupport,
                desugarGraphFile
            )
        }
    }

    private fun processIncrementally(
        input: File,
        inputChanges: SerializableFileChanges,
        classpathChanges: SerializableFileChanges,
        dexOutputDir: File,
        desugarGraphFile: File
    ) {
        val desugarGraph = try {
            readDesugarGraph(desugarGraphFile)
        } catch (e: Exception) {
            Logging.getLogger(BaseDexingTransform::class.java).warn(
                "Failed to read desugaring graph. Fall back to non-incremental mode."
            )
            processNonIncrementally(input, dexOutputDir, null, true, desugarGraphFile)
            return
        }

        val removedFiles =
            (inputChanges.removedFiles + classpathChanges.removedFiles).map { it.file }.toSet()
        val modifiedFiles =
            (inputChanges.modifiedFiles + classpathChanges.modifiedFiles).map { it.file }.toSet()
        val addedFiles =
            (inputChanges.addedFiles + classpathChanges.addedFiles).map { it.file }.toSet()
        val unchangedButImpactedFiles = desugarGraph.getAllDependents(removedFiles + modifiedFiles)
        val modifiedImpactedOrAddedFiles = modifiedFiles + unchangedButImpactedFiles + addedFiles
        val removedModifiedOrImpactedFiles =
            removedFiles + modifiedFiles + unchangedButImpactedFiles

        inputChanges.removedFiles.forEach {
            if (ClassFileInput.CLASS_MATCHER.test(it.file.path)) {
                val staleOutputFile =
                    dexOutputDir.resolve(ClassFileEntry.withDexExtension(it.normalizedPath))
                if (staleOutputFile.exists()) staleOutputFile.delete()
            }
        }

        removedModifiedOrImpactedFiles.forEach {
            desugarGraph.removeNode(it)
        }

        val filter: (File, String) -> Boolean = { rootPath: File, relativePath: String ->
            rootPath in modifiedImpactedOrAddedFiles ||
                    rootPath.resolve(relativePath) in modifiedImpactedOrAddedFiles
        }
        process(input, filter, dexOutputDir, null, true, desugarGraph)

        writeDesugarGraph(desugarGraphFile, desugarGraph)
    }

    private fun processNonIncrementally(
        input: File,
        dexOutputDir: File,
        keepRulesOutputFile: File?,
        provideIncrementalSupport: Boolean,
        desugarGraphFile: File?
    ) {
        dexOutputDir.cleanOutputDir()
        keepRulesOutputFile?.let {
            it.deleteIfExists()
            if (!it.parentFile.exists()) it.parentFile.mkdirs()
        }
        desugarGraphFile?.let {
            it.deleteIfExists()
            if (!it.parentFile.exists()) it.parentFile.mkdirs()
        }

        val desugarGraph = desugarGraphFile?.let {
            MutableDependencyGraph<File>()
        }

        process(
            input,
            { _, _ -> true },
            dexOutputDir,
            keepRulesOutputFile,
            provideIncrementalSupport,
            desugarGraph
        )

        desugarGraphFile?.let {
            writeDesugarGraph(it, desugarGraph!!)
        }
    }

    private fun process(
        input: File,
        inputFilter: (File, String) -> Boolean,
        dexOutputDir: File,
        keepRulesOutputFile: File?,
        provideIncrementalSupport: Boolean,
        desugarGraphUpdater: DependencyGraphUpdater<File>?
    ) {
        Closer.create().use { closer ->
            val d8DexBuilder = DexArchiveBuilder.createD8DexBuilder(
                DexParameters(
                    minSdkVersion = parameters.minSdkVersion.get(),
                    debuggable = parameters.debuggable.get(),
                    dexPerClass = provideIncrementalSupport,
                    withDesugaring = parameters.enableDesugaring.get(),
                    desugarBootclasspath = ClassFileProviderFactory(
                        parameters.bootClasspath.files.map(File::toPath)
                    )
                        .also { closer.register(it) },
                    desugarClasspath = ClassFileProviderFactory(computeClasspathFiles()).also {
                        closer.register(it)
                    },
                    coreLibDesugarConfig = parameters.libConfiguration.orNull,
                    coreLibDesugarOutputKeepRuleFile = keepRulesOutputFile,
                    messageReceiver = MessageReceiverImpl(
                        parameters.errorFormat.get(),
                        LoggerFactory.getLogger(BaseDexingTransform::class.java)
                    )
                )
            )

            ClassFileInputs.fromPath(input.toPath()).use { classFileInput ->
                classFileInput.entries { rootPath, relativePath ->
                    inputFilter(rootPath.toFile(), relativePath)
                }.use { classesInput ->
                    d8DexBuilder.convert(
                        classesInput,
                        dexOutputDir.toPath(),
                        desugarGraphUpdater
                    )
                }
            }
        }
    }
}

@CacheableTransform
abstract class DexingNoClasspathTransform : BaseDexingTransform<BaseDexingTransform.Parameters>() {
    override fun computeClasspathFiles() = listOf<Path>()
}

@CacheableTransform
abstract class DexingWithClasspathTransform : BaseDexingTransform<BaseDexingTransform.Parameters>() {
    @get:CompileClasspath
    @get:InputArtifactDependencies
    abstract val classpath: FileCollection

    override fun computeClasspathFiles() = classpath.files.map(File::toPath)
}

fun getDexingArtifactConfigurations(components: Collection<ComponentCreationConfig>): Set<DexingArtifactConfiguration> {
    return components
        .filterIsInstance<ApkCreationConfig>()
        .map { getDexingArtifactConfiguration(it) }.toSet()
}

fun getDexingArtifactConfiguration(creationConfig: ApkCreationConfig): DexingArtifactConfiguration {
    return DexingArtifactConfiguration(
        minSdk = creationConfig.minSdkVersionForDexing.getFeatureLevel(),
        isDebuggable = creationConfig.debuggable,
        enableDesugaring = creationConfig.getJava8LangSupportType() == Java8LangSupport.D8,
        enableCoreLibraryDesugaring = creationConfig.isCoreLibraryDesugaringEnabled,
        needsShrinkDesugarLibrary = creationConfig.needsShrinkDesugarLibrary,
        asmTransformedVariant = null,
        useJacocoTransformInstrumentation = creationConfig.useJacocoTransformInstrumentation
    )
}


data class DexingArtifactConfiguration(
    private val minSdk: Int,
    private val isDebuggable: Boolean,
    private val enableDesugaring: Boolean,
    private val enableCoreLibraryDesugaring: Boolean,
    private val needsShrinkDesugarLibrary: Boolean,
    private val asmTransformedVariant: String?,
    private val useJacocoTransformInstrumentation: Boolean
) {

    private val needsClasspath = enableDesugaring && minSdk < AndroidVersion.VersionCodes.N

    fun registerTransform(
        projectName: String,
        dependencyHandler: DependencyHandler,
        bootClasspath: FileCollection,
        libConfiguration: Provider<String>,
        errorFormat: SyncOptions.ErrorFormatMode
    ) {
        dependencyHandler.registerTransform(getTransformClass()) { spec ->
            spec.parameters { parameters ->
                parameters.projectName.set(projectName)
                parameters.minSdkVersion.set(minSdk)
                parameters.debuggable.set(isDebuggable)
                parameters.enableDesugaring.set(enableDesugaring)
                if (needsClasspath || enableCoreLibraryDesugaring) {
                    parameters.bootClasspath.from(bootClasspath)
                }
                parameters.errorFormat.set(errorFormat)
                if (enableCoreLibraryDesugaring) {
                    parameters.libConfiguration.set(libConfiguration)
                }
            }
            val inputArtifact: AndroidArtifacts.ArtifactType =
                if (useJacocoTransformInstrumentation) {
                    when {
                        asmTransformedVariant != null ->
                            AndroidArtifacts.ArtifactType.JACOCO_ASM_INSTRUMENTED_JARS
                        !needsClasspath ->
                            AndroidArtifacts.ArtifactType.JACOCO_CLASSES
                        else ->
                            AndroidArtifacts.ArtifactType.JACOCO_CLASSES_JAR
                    }
                } else {
                    when {
                        asmTransformedVariant != null ->
                            AndroidArtifacts.ArtifactType.ASM_INSTRUMENTED_JARS
                        !needsClasspath ->
                            AndroidArtifacts.ArtifactType.CLASSES
                        else ->
                            AndroidArtifacts.ArtifactType.CLASSES_JAR
                    }
                }
            spec.from.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                inputArtifact.type
            )
            if (needsShrinkDesugarLibrary) {
                spec.to.attribute(
                    ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                    AndroidArtifacts.ArtifactType.DEX_AND_KEEP_RULES.type
                )
            } else {
                spec.to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, AndroidArtifacts.ArtifactType.DEX.type)
            }

            getAttributes().apply {
                addAttributesToContainer(spec.from)
                addAttributesToContainer(spec.to)
            }
        }
    }

    private fun getTransformClass(): Class<out BaseDexingTransform<BaseDexingTransform.Parameters>> {
        return if (needsClasspath) {
            DexingWithClasspathTransform::class.java
        } else {
            DexingNoClasspathTransform::class.java
        }
    }

    fun getAttributes(): AndroidAttributes {
        return AndroidAttributes(
            mapOf(
                ATTR_MIN_SDK to minSdk.toString(),
                ATTR_IS_DEBUGGABLE to isDebuggable.toString(),
                ATTR_ENABLE_DESUGARING to enableDesugaring.toString(),
                ATTR_ENABLE_JACOCO_INSTRUMENTATION to useJacocoTransformInstrumentation.toString(),
            )
        )
    }
}

val ATTR_MIN_SDK: Attribute<String> = Attribute.of("dexing-min-sdk", String::class.java)
val ATTR_IS_DEBUGGABLE: Attribute<String> =
    Attribute.of("dexing-is-debuggable", String::class.java)
val ATTR_ENABLE_DESUGARING: Attribute<String> =
    Attribute.of("dexing-enable-desugaring", String::class.java)
val ATTR_ENABLE_JACOCO_INSTRUMENTATION: Attribute<String> =
    Attribute.of("dexing-enable-jacoco-instrumentation", String::class.java)

const val DESUGAR_GRAPH_FILE_NAME = "desugar_graph.bin"
