package com.tyron.builder.api.artifact.impl

import com.tyron.builder.api.artifact.*
import com.tyron.builder.api.artifact.Artifact.*
import com.tyron.builder.api.variant.BuiltArtifactsLoader
import com.tyron.builder.api.variant.ScopedArtifacts
import com.tyron.builder.api.variant.impl.BuiltArtifactsLoaderImpl
import com.tyron.builder.gradle.internal.scope.getIntermediateOutputPath
import com.tyron.builder.gradle.internal.scope.getOutputPath
import com.tyron.builder.internal.utils.setDisallowChanges
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.*

/**
 * Implementation of the [Artifacts] Variant API interface.
 */
class ArtifactsImpl(
    project: Project,
    private val identifier: String
): Artifacts {

    private val storageProvider = StorageProviderImpl()
    val objects: ObjectFactory = project.objects
    internal val buildDirectory = project.layout.buildDirectory
    private val outstandingRequests = Collections.synchronizedList(ArrayList<ArtifactOperationRequest>())

    override fun getBuiltArtifactsLoader(): BuiltArtifactsLoader {
        return BuiltArtifactsLoaderImpl()
    }

    private val projectScopedArtifacts = ScopedArtifactsImpl(
        ScopedArtifacts.Scope.PROJECT,
        identifier,
        project.layout,
        project::files,
    ).also {
        it.setInitialContent(
            ScopedArtifact.CLASSES,
            project.files().also { configurableFileCollection ->
                @Suppress("DEPRECATION")
                configurableFileCollection.from(
                    getAll(MultipleArtifact.ALL_CLASSES_DIRS),
                    getAll(MultipleArtifact.ALL_CLASSES_JARS),
                )
            }
        )
    }

    private val allScopedArtifacts = ScopedArtifactsImpl(
        ScopedArtifacts.Scope.ALL,
        identifier,
        project.layout,
        project::files,
    )

    override fun forScope(scope: ScopedArtifacts.Scope): ScopedArtifactsImpl =
        when(scope) {
            ScopedArtifacts.Scope.PROJECT -> projectScopedArtifacts
            ScopedArtifacts.Scope.ALL -> allScopedArtifacts
            else -> throw IllegalArgumentException("Not implemented yet !")
        }

    override fun <FILE_TYPE : FileSystemLocation> get(
        type: SingleArtifact<FILE_TYPE>
    ): Provider<FILE_TYPE>  = getArtifactContainer(type).get()

    fun <FILE_TYPE : FileSystemLocation> get(
        type: Single<FILE_TYPE>
    ): Provider<FILE_TYPE> = getArtifactContainer(type).get()

    override fun <FileTypeT : FileSystemLocation> getAll(
        type: MultipleArtifact<FileTypeT>
    ): Provider<List<FileTypeT>> = getArtifactContainer(type).get()

    fun <FILE_TYPE : FileSystemLocation> getAll(
        type: Multiple<FILE_TYPE>
    ): Provider<List<FILE_TYPE>>
            = getArtifactContainer(type).get()

    override fun <TaskT : Task> use(taskProvider: TaskProvider<TaskT>): TaskBasedOperationImpl<TaskT> {
        return TaskBasedOperationImpl(objects, this, taskProvider).also {
            outstandingRequests.add(it)
        }
    }

    internal fun <T: FileSystemLocation> getOutputPath(type: Artifact<T>, vararg paths: String, forceFilename:String = "")=
        type.getOutputPath(buildDirectory, identifier, *paths, forceFilename = forceFilename)

    private fun <T: FileSystemLocation> getIntermediateOutputPath(type: Artifact<T>, vararg paths: String, forceFilename:String = "")=
        type.getIntermediateOutputPath(buildDirectory, identifier, *paths, forceFilename = forceFilename)

    fun calculateOutputPath(type: Single<*>, taskProvider: Task): File {
        with(getArtifactContainer(type)) {
            val fileName = finalFilename?.orNull ?: calculateFileName(type)
            return if (getFinalProvider() == null || taskProvider.name == getFinalProvider()?.name) {
                if (buildOutputLocation?.isPresent == true)
                    File(buildOutputLocation!!.get().asFile, fileName)
                else
                    getOutputPath(type, forceFilename = fileName)
            } else getIntermediateOutputPath(
                type,
                taskProvider.name,
                forceFilename = fileName
            )
        }
    }

    fun calculateFileName(type:Single<*>):String {
        if(type.kind is ArtifactKind.FILE){
            return if(!type.getFileSystemLocationName().isNullOrEmpty())
                type.getFileSystemLocationName()!!
            else
                DEFAULT_FILE_NAME_OF_REGULAR_FILE_ARTIFACTS
        }
        return ""
    }

    internal fun <FILE_TYPE : FileSystemLocation> getArtifactContainer(
        type: Single<FILE_TYPE>
    ): SingleArtifactContainer<FILE_TYPE> {
        return storageProvider.getStorage(type.kind).getArtifact(objects, type)
    }

    internal fun <FILE_TYPE : FileSystemLocation> getArtifactContainer(
        type: Multiple <FILE_TYPE>
    ): MultipleArtifactContainer<FILE_TYPE> {

        return storageProvider.getStorage(type.kind).getArtifact(objects, type)
    }

    fun <T: FileSystemLocation> republish(
        source: Single<T>,
        target: Single<T>) {
        storageProvider.getStorage(target.kind).copy(target, getArtifactContainer(source))
    }

    private fun <T: FileSystemLocation> republish(
        source: Multiple<T>,
        target: Multiple<T>) {
        storageProvider.getStorage(target.kind).copy(target, getArtifactContainer(source))
    }

    @JvmName("setInitialProvider")
    internal fun <FILE_TYPE, TASK> setInitialProvider(
        taskProvider: TaskProvider<TASK>,
        property: (TASK) -> FileSystemLocationProperty<FILE_TYPE>
    ): SingleInitialProviderRequestImpl<TASK, FILE_TYPE>
            where FILE_TYPE : FileSystemLocation,
                  TASK: Task
            = SingleInitialProviderRequestImpl(this, taskProvider, property)

    @JvmName("setInitialProvider")
    internal fun <FILE_TYPE, TASK> setInitialProvider(
        taskProvider: TaskProvider<TASK>,
        property: (TASK) -> FileSystemLocationProperty<FILE_TYPE>,
        customProvider: ((TASK) -> Provider<FILE_TYPE>)
    ): SingleInitialProviderRequestImpl<TASK, FILE_TYPE>
            where FILE_TYPE : FileSystemLocation,
                  TASK: Task
            = SingleInitialProviderRequestImpl(this, taskProvider, property, customProvider)


    internal fun <FILE_TYPE: FileSystemLocation, TASK: Task> addInitialProvider(
        type: Multiple<FILE_TYPE>,
        taskProvider: TaskProvider<TASK>,
        property: (TASK) -> FileSystemLocationProperty<FILE_TYPE>
    ) {
        val artifactContainer = getArtifactContainer(type)
        taskProvider.configure {
            property(it).set(type.getOutputPath(buildDirectory, identifier, taskProvider.name))
        }
        artifactContainer.addInitialProvider(taskProvider, taskProvider.flatMap { property(it) })
    }

    fun <T: FileSystemLocation> setTaskInputToFinalProduct(
        artifactType: Single<T>, taskInputProperty: Property<T>
    ) {
        val finalProduct = get(artifactType)
        taskInputProperty.setDisallowChanges(finalProduct)
    }

    fun <FILE_TYPE : FileSystemLocation> copy(
        artifactType: Single<FILE_TYPE>,
        from: ArtifactsImpl
    ) {
        val artifactContainer = from.getArtifactContainer(artifactType)
        storageProvider.getStorage(artifactType.kind).copy(artifactType, artifactContainer)
    }

    fun <FILE_TYPE : FileSystemLocation> copy(
        artifactType: Multiple<FILE_TYPE>,
        from: ArtifactsImpl
    ) {
        val artifactContainer = from.getArtifactContainer(artifactType)
        storageProvider.getStorage(artifactType.kind).copy(artifactType, artifactContainer)
    }

    fun <T: FileSystemLocation> appendTo(type: Multiple<T>, from: Single<T>) {
        getArtifactContainer(type).transferFrom(this, from)
    }

    fun <T: FileSystemLocation> appendTo(type: ScopedArtifact, from: Single<T>) {
        forScope(ScopedArtifacts.Scope.PROJECT)
            .setInitialContent(type, this, from)
    }

    fun <T: FileSystemLocation> appendAll(type: Multiple<T>, elements: Provider<List<T>>) {
        getArtifactContainer(type).addInitialProvider(listOf(), elements);
    }

    fun addRequest(request: ArtifactOperationRequest) {
        outstandingRequests.add(request)
    }

    fun closeRequest(request: ArtifactOperationRequest) {
        outstandingRequests.remove(request)
    }

    internal fun finalizeAndLock() {
        if (outstandingRequests.isEmpty()) return
        throw RuntimeException(outstandingRequests.joinToString(
            separator = "\n\t",
            prefix = "The following Variant API operations are incomplete :\n",
            transform = ArtifactOperationRequest::description,
            postfix = "\nMake sure to correctly chain all calls."
        ))
    }
}

internal class SingleInitialProviderRequestImpl<TASK: Task, FILE_TYPE: FileSystemLocation>(
    private val artifactsImpl: ArtifactsImpl,
    private val taskProvider: TaskProvider<TASK>,
    private val fromProperty: (TASK) -> FileSystemLocationProperty<FILE_TYPE>,
    private val fromProvider: ((TASK) -> Provider<FILE_TYPE>) = fromProperty
) {

    private var fileName: Property<String> = artifactsImpl.objects.property(String::class.java)
    private var buildOutputLocation: DirectoryProperty = artifactsImpl.objects.directoryProperty()

    private var absoluteOutputLocation: String? =  null
    private var buildOutputLocationResolver: ((TASK) -> Provider<Directory>)? = null

    fun atLocation(location: String?): SingleInitialProviderRequestImpl<TASK, FILE_TYPE> {
        absoluteOutputLocation = location
        return this
    }

    fun atLocation(
        finalLocation: Provider<Directory>
    ): SingleInitialProviderRequestImpl<TASK, FILE_TYPE> {
        buildOutputLocation.set(finalLocation)
        return this
    }

    fun atLocation(
        finalLocation: File
    ): SingleInitialProviderRequestImpl<TASK, FILE_TYPE> {
        buildOutputLocation.set(finalLocation)
        return this
    }

    fun withBuildOutput(
        finalLocation: Provider<Directory>
    ): SingleInitialProviderRequestImpl<TASK, FILE_TYPE> {
        buildOutputLocation.set(finalLocation)
        return this
    }

    fun withBuildOutput(
        finalLocation: File
    ): SingleInitialProviderRequestImpl<TASK, FILE_TYPE> {
        buildOutputLocation.fileValue(finalLocation)
        return this
    }

    fun atLocation(location: (TASK) -> Provider<Directory>)
            : SingleInitialProviderRequestImpl<TASK, FILE_TYPE> {
        buildOutputLocationResolver = location
        return this
    }

    fun withName(nameProperty: Property<String>): SingleInitialProviderRequestImpl<TASK, FILE_TYPE> {
        fileName.set(nameProperty)
        return this
    }

    fun withName(name: String): SingleInitialProviderRequestImpl<TASK, FILE_TYPE> {
        fileName.set(name)
        return this
    }

    fun on(type: Single<FILE_TYPE>) {
        val artifactContainer = artifactsImpl.getArtifactContainer(type)
        fileName.convention(artifactsImpl.calculateFileName(type))
        artifactContainer.initOutputs(
            fileName,
            buildOutputLocation)

        taskProvider.configure {
            val fileNameOrEmpty = fileName.getOrElse("")
            val outputAbsolutePath = when {
                absoluteOutputLocation != null ->
                    File(absoluteOutputLocation, fileNameOrEmpty)
                buildOutputLocationResolver != null -> {
                    val resolver = buildOutputLocationResolver!!
                    File(resolver.invoke(it).get().asFile, fileNameOrEmpty)
                }
                else -> artifactsImpl.calculateOutputPath(type, it)
            }
            fromProperty(it).set(outputAbsolutePath)
        }
        artifactContainer.setInitialProvider(
            taskProvider, taskProvider.flatMap { fromProvider(it) }
        )
    }
}

const val DEFAULT_FILE_NAME_OF_REGULAR_FILE_ARTIFACTS = "out.jar"
