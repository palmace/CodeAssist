package com.tyron.builder.gradle.internal.tasks

import com.tyron.builder.gradle.internal.component.ComponentCreationConfig
import com.tyron.builder.gradle.internal.scope.InternalArtifactType
import com.tyron.builder.gradle.internal.tasks.factory.VariantTaskCreationAction
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import java.util.Properties

@DisableCachingByDefault
abstract class JacocoPropertiesTask : NonIncrementalTask() {

    @get:OutputFile
    abstract val propertiesFile: RegularFileProperty

    override fun doTaskAction() {
        val props = Properties()
        props.setProperty("destfile", "jacoco.exec")
        props.setProperty("append", "true")
        
        propertiesFile.get().asFile.outputStream().use { 
            props.store(it, "Jacoco Agent Properties")
        }
    }

    class CreationAction(creationConfig: ComponentCreationConfig) :
        VariantTaskCreationAction<JacocoPropertiesTask, ComponentCreationConfig>(creationConfig) {
        override val name: String
            get() = computeTaskName("jacoco", "Properties")
        override val type: Class<JacocoPropertiesTask>
            get() = JacocoPropertiesTask::class.java

        override fun handleProvider(taskProvider: TaskProvider<JacocoPropertiesTask>) {
            super.handleProvider(taskProvider)
            creationConfig.artifacts.setInitialProvider(
                taskProvider,
                JacocoPropertiesTask::propertiesFile
            ).withName("jacoco-agent.properties").on(InternalArtifactType.JACOCO_CONFIG_RESOURCES_JAR)
        }
    }
}
