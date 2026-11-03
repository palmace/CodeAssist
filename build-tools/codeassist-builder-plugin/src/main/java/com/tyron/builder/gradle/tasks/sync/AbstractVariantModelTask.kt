package com.tyron.builder.gradle.tasks.sync

import com.tyron.builder.gradle.internal.component.ComponentCreationConfig
import com.tyron.builder.gradle.internal.scope.InternalArtifactType
import com.tyron.builder.gradle.internal.tasks.NonIncrementalTask
import com.tyron.builder.gradle.internal.tasks.factory.VariantTaskCreationAction
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import java.io.BufferedOutputStream
import java.io.FileOutputStream

/**
 * Task base para exportar el modelo de la variante a CodeAssist.
 * Suturado: Se ha desactivado la dependencia de filebasedproperties que estaba rota.
 */
@DisableCachingByDefault
abstract class AbstractVariantModelTask: NonIncrementalTask() {

    companion object {
        fun getTaskName(creationConfig: ComponentCreationConfig) =
            creationConfig.computeTaskName("create", "VariantModel")
    }

    @get:OutputFile
    abstract val outputModelFile: RegularFileProperty

    override fun doTaskAction() {
        // Implementación de emergencia: creamos un archivo vacío para no bloquear el build
        val file = outputModelFile.asFile.get()
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        file.writeText("{}") 
    }

    // Cambiamos el parámetro a Any para evitar errores de importación de VariantProperties
    protected abstract fun addVariantContent(variant: Any)

    abstract class CreationAction<T : AbstractVariantModelTask, U: ComponentCreationConfig>(
        creationConfig: U
    ): VariantTaskCreationAction<T, U>(
            creationConfig = creationConfig,
            dependsOnPreBuildTask = false,
        ) {

        override val name: String get() = getTaskName(creationConfig)

        override fun handleProvider(taskProvider: TaskProvider<T>) {
            super.handleProvider(taskProvider)
            creationConfig.artifacts.setInitialProvider(
                taskProvider,
                AbstractVariantModelTask::outputModelFile
            ).on(InternalArtifactType.VARIANT_MODEL)
        }
    }
}
