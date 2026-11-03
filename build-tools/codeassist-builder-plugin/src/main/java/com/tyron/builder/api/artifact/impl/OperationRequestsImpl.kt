package com.tyron.builder.api.artifact.impl

import com.tyron.builder.api.artifact.*
import com.tyron.builder.api.artifact.Artifact.*
import com.tyron.builder.api.variant.impl.BuiltArtifactsImpl
import com.tyron.builder.gradle.internal.scope.InternalArtifactType
import com.tyron.builder.gradle.internal.scope.getOutputPath
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class OutOperationRequestImpl<TaskT: Task, FileTypeT: FileSystemLocation>(
    override val artifacts: ArtifactsImpl,
    val taskProvider: TaskProvider<TaskT>,
    val with: (TaskT) -> FileSystemLocationProperty<FileTypeT>
) : OutOperationRequest<FileTypeT>, ArtifactOperationRequest {

    override fun <ArtifactTypeT> toAppendTo(type: ArtifactTypeT)
            where ArtifactTypeT : Multiple<FileTypeT>,
                  ArtifactTypeT : Appendable {
        closeRequest()
        toAppend(artifacts, taskProvider, with, type)
    }

    override fun <ArtifactTypeT> toCreate(type: ArtifactTypeT)
            where ArtifactTypeT : Single<FileTypeT>,
                  ArtifactTypeT : Replaceable {
        closeRequest()
        toCreate(artifacts, taskProvider, with, type)
    }

    override val description: String
        get() = "Task ${taskProvider.name} was wired with an output but neither toAppend or toCreate " +
                "methods were invoked."
}

class InAndOutFileOperationRequestImpl<TaskT: Task>(
    override val artifacts: ArtifactsImpl,
    val taskProvider: TaskProvider<TaskT>,
    val from: (TaskT) -> RegularFileProperty,
    val into: (TaskT) -> RegularFileProperty
): InAndOutFileOperationRequest, ArtifactOperationRequest {

    override fun <ArtifactTypeT> toTransform(type: ArtifactTypeT)
            where ArtifactTypeT : Single<RegularFile>,
                  ArtifactTypeT : Transformable {
        closeRequest()
        toTransform(artifacts, taskProvider, from, into, type)
    }

    override val description: String
        get() = "Task ${taskProvider.name} was wired with an Input and an Output but " +
                "toTransform method was never invoked"
}

class CombiningOperationRequestImpl<TaskT: Task, FileTypeT: FileSystemLocation>(
    val objects: ObjectFactory,
    override val artifacts: ArtifactsImpl,
    val taskProvider: TaskProvider<TaskT>,
    val from: (TaskT) -> ListProperty<FileTypeT>,
    val into: (TaskT) -> FileSystemLocationProperty<FileTypeT>
): CombiningOperationRequest<FileTypeT>, ArtifactOperationRequest {
    override fun <ArtifactTypeT> toTransform(type: ArtifactTypeT)
            where ArtifactTypeT : Multiple<FileTypeT>,
                  ArtifactTypeT : Transformable {
        closeRequest()
        val artifactContainer = artifacts.getArtifactContainer(type)
        val newList = objects.listProperty(type.kind.dataType().java)
        val currentProviders= artifactContainer.transform(taskProvider, taskProvider.flatMap { newList })
        taskProvider.configure {
            newList.add(into(it))
            into(it).set(artifacts.getOutputPath(type, taskProvider.name))
            from(it).set(currentProviders)
        }
    }

    override val description: String
        get() = "Task ${taskProvider.name} was wired to combine multiple inputs into an output but " +
                "toTransform method was never invoked"
}

class InAndOutDirectoryOperationRequestImpl<TaskT: Task>(
    override val artifacts: ArtifactsImpl,
    val taskProvider: TaskProvider<TaskT>,
    val from: (TaskT) -> DirectoryProperty,
    val into: (TaskT) -> DirectoryProperty
): InAndOutDirectoryOperationRequest<TaskT>, ArtifactOperationRequest {

    override fun <ArtifactTypeT> toTransform(type: ArtifactTypeT)
            where ArtifactTypeT : Single<Directory>,
                  ArtifactTypeT : Transformable {
        closeRequest()
        toTransform(artifacts, taskProvider, from, into, type)
    }

    override fun <ArtifactTypeT> toTransformMany(
        type: ArtifactTypeT
    ): ArtifactTransformationRequest<TaskT>
            where ArtifactTypeT : Single<Directory>,
                  ArtifactTypeT : ContainsMany {

        closeRequest()
        val artifactContainer = artifacts.getArtifactContainer(type)
        val currentProvider =  artifactContainer.transform(taskProvider, taskProvider.flatMap { into(it) })
        val builtArtifactsReference = AtomicReference<BuiltArtifactsImpl>()

        initializeInput(
            taskProvider,
            from,
            into,
            currentProvider,
            builtArtifactsReference
        )

        taskProvider.configure { task ->
            into(task).set(type.getOutputPath(artifacts.buildDirectory, taskProvider.name))
        }

        publicTypesToIdeModelTypeMap[type]?.let {
            val ideModelContainer = artifacts.getArtifactContainer(it)
            ideModelContainer.replace(taskProvider,
                taskProvider.flatMap { task ->
                    into(task).file(BuiltArtifactsImpl.METADATA_FILE_NAME)
                })
        }

        return ArtifactTransformationRequestImpl(
            builtArtifactsReference,
            inputLocation = from,
            outputArtifactType = type,
            outputLocation = into
        )
    }

    fun <ArtifactTypeT, ArtifactTypeU> toTransformMany(
        sourceType: ArtifactTypeT,
        targetType: ArtifactTypeU,
        atLocation: String? = null
    ): ArtifactTransformationRequestImpl<TaskT>
            where
            ArtifactTypeT : Single<Directory>,
            ArtifactTypeT : ContainsMany,
            ArtifactTypeU : Single<Directory>,
            ArtifactTypeU : ContainsMany {
        closeRequest()
        val initialProvider = artifacts.setInitialProvider(taskProvider, into)
        if (atLocation != null) {
            initialProvider.atLocation(atLocation)
        }
        initialProvider.on(targetType)

        return initializeTransform(sourceType, targetType, from, into)
    }

    private fun <ArtifactTypeT, ArtifactTypeU> initializeTransform(
        sourceType: ArtifactTypeT,
        targetType: ArtifactTypeU,
        inputLocation: (TaskT) -> DirectoryProperty,
        outputLocation: (TaskT) -> DirectoryProperty
    ): ArtifactTransformationRequestImpl<TaskT>
            where ArtifactTypeT : Single<Directory>,
                  ArtifactTypeT : ContainsMany,
                  ArtifactTypeU : Single<Directory>,
                  ArtifactTypeU : ContainsMany {

        val builtArtifactsReference = AtomicReference<BuiltArtifactsImpl>()
        val inputProvider = artifacts.get(sourceType)

        initializeInput(taskProvider, inputLocation, outputLocation, inputProvider, builtArtifactsReference)

        return ArtifactTransformationRequestImpl(
            builtArtifactsReference,
            inputLocation = inputLocation,
            outputArtifactType = targetType,
            outputLocation = outputLocation
        )
    }

    override val description: String
        get() = "Task ${taskProvider.name} was wired with an Input and an Output but " +
                "toTransform or toTransformMany methods were never invoked"

    companion object {
        val publicTypesToIdeModelTypeMap:
                Map<Single<Directory>, InternalArtifactType<RegularFile>>
                = mapOf(SingleArtifact.APK to InternalArtifactType.APK_IDE_MODEL,
        )

        fun <T : Task> initializeInput(
            taskProvider: TaskProvider<T>,
            inputLocation: (T) -> FileSystemLocationProperty<Directory>,
            outputLocation: (T) -> FileSystemLocationProperty<Directory>,
            inputProvider: Provider<Directory>,
            builtArtifactsReference: AtomicReference<BuiltArtifactsImpl>
        ) {
            taskProvider.configure { task: T ->
                inputLocation(task).set(inputProvider)
                task.doLast {
                    builtArtifactsReference.get().save(outputLocation(task).get())
                }
            }
        }
    }
}

private fun <TaskT: Task, FileTypeT: FileSystemLocation, ArtifactTypeT> toAppend(
    artifacts: ArtifactsImpl,
    taskProvider: TaskProvider<TaskT>,
    with: (TaskT) -> FileSystemLocationProperty<FileTypeT>,
    type: ArtifactTypeT
) where ArtifactTypeT : Multiple<FileTypeT>,
        ArtifactTypeT: Appendable {

    val artifactContainer = artifacts.getArtifactContainer(type)
    taskProvider.configure {
        with(it).set(artifacts.getOutputPath(type, taskProvider.name))
    }
    artifactContainer.addInitialProvider(taskProvider, taskProvider.flatMap { with(it) })
}


private fun <TaskT: Task, FileTypeT: FileSystemLocation, ArtifactTypeT> toCreate(
    artifacts: ArtifactsImpl,
    taskProvider: TaskProvider<TaskT>,
    with: (TaskT) -> FileSystemLocationProperty<FileTypeT>,
    type: ArtifactTypeT
) where ArtifactTypeT : Single<FileTypeT>,
        ArtifactTypeT: Replaceable {

    val artifactContainer = artifacts.getArtifactContainer(type)
    taskProvider.configure {
        with(it).set(artifacts.getOutputPath(type, taskProvider.name))
    }
    artifactContainer.replace(taskProvider, taskProvider.flatMap { with(it) })
}

private fun <TaskT: Task, FileTypeT: FileSystemLocation, ArtifactTypeT> toTransform(
    artifacts: ArtifactsImpl,
    taskProvider: TaskProvider<TaskT>,
    from: (TaskT) -> FileSystemLocationProperty<FileTypeT>,
    into: (TaskT) -> FileSystemLocationProperty<FileTypeT>,
    type: ArtifactTypeT)
        where ArtifactTypeT : Single<FileTypeT>,
              ArtifactTypeT : Transformable {
    val artifactContainer = artifacts.getArtifactContainer(type)
    val currentProvider =  artifactContainer.transform(taskProvider, taskProvider.flatMap { into(it) })
    taskProvider.configure { it ->
        from(it).set(currentProvider)
        val outputAbsolutePath:File = artifacts.calculateOutputPath(type, it)
        
        val intoProperty = into(it)
        if (intoProperty is RegularFileProperty) {
            intoProperty.fileValue(outputAbsolutePath)
        } else if (intoProperty is DirectoryProperty) {
            // Using set(File) which should be available on DirectoryProperty in AGP-compatible Gradle versions
            intoProperty.set(outputAbsolutePath)
        } else {
            intoProperty.set(outputAbsolutePath as File?)
        }
    }
}
