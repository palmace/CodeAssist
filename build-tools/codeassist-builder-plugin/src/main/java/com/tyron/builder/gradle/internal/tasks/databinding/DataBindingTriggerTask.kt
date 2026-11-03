package com.tyron.builder.gradle.internal.tasks.databinding

import com.tyron.builder.gradle.internal.component.ComponentCreationConfig
import com.tyron.builder.gradle.internal.scope.InternalArtifactType
import com.tyron.builder.gradle.internal.tasks.BuildAnalyzer
import com.tyron.builder.gradle.internal.tasks.NonIncrementalTask
import com.tyron.builder.gradle.internal.tasks.factory.VariantTaskCreationAction
import com.tyron.builder.internal.utils.setDisallowChanges
import com.tyron.builder.gradle.options.BooleanOption
import com.tyron.builder.gradle.internal.tasks.TaskCategory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Task to create an empty class annotated with a data binding annotation.
 */
@DisableCachingByDefault
@BuildAnalyzer(primaryTaskCategory = TaskCategory.DATA_BINDING)
abstract class DataBindingTriggerTask : NonIncrementalTask() {

    @get:Input
    abstract val namespace: Property<String>

    @get:Input
    abstract val useAndroidX: Property<Boolean>

    @get:OutputDirectory
    abstract val triggerDir: DirectoryProperty

    override fun doTaskAction() {
        val annotationName =
            if (useAndroidX.get()) {
                "androidx.databinding.BindingBuildInfo"
            } else {
                "android.databinding.BindingBuildInfo"
            }
            
        val fileContents =
            """
            package ${namespace.get()};

            @$annotationName
            public class $DATA_BINDING_TRIGGER_CLASS {}
            """.trimIndent()

        val outputDir = triggerDir.get().asFile
        val outputFile = File(outputDir, "${namespace.get().replace('.', '/')}/$DATA_BINDING_TRIGGER_CLASS.java")
        
        outputFile.parentFile.mkdirs()
        outputFile.writeText(fileContents)
    }

    class CreationAction(creationConfig: ComponentCreationConfig) :
        VariantTaskCreationAction<DataBindingTriggerTask, ComponentCreationConfig>(
            creationConfig
        ) {

        override val name: String = computeTaskName("dataBindingTrigger")

        override val type: Class<DataBindingTriggerTask> = DataBindingTriggerTask::class.java

        override fun handleProvider(
            taskProvider: TaskProvider<DataBindingTriggerTask>
        ) {
            super.handleProvider(taskProvider)
            creationConfig.artifacts.setInitialProvider(
                taskProvider,
                DataBindingTriggerTask::triggerDir
            ).on(InternalArtifactType.DATA_BINDING_TRIGGER)
        }

        override fun configure(task: DataBindingTriggerTask) {
            super.configure(task)
            task.namespace.setDisallowChanges(creationConfig.namespace)
            task.useAndroidX.setDisallowChanges(
                creationConfig.services.projectOptions[BooleanOption.USE_ANDROID_X]
            )
        }
    }
}

const val DATA_BINDING_TRIGGER_CLASS = "DataBindingTriggerClass"
