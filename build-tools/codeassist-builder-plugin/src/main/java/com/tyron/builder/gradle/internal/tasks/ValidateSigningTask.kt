package com.tyron.builder.gradle.internal.tasks

import com.tyron.builder.api.artifact.impl.ArtifactsImpl
import com.tyron.builder.api.dsl.SigningConfig
import com.tyron.builder.core.BuilderConstants
import com.tyron.builder.gradle.internal.component.ApkCreationConfig
import com.tyron.builder.gradle.internal.packaging.createDefaultDebugStore
import com.tyron.builder.gradle.internal.packaging.getDefaultDebugKeystoreSigningConfig
import com.tyron.builder.gradle.internal.scope.InternalArtifactType
import com.tyron.builder.gradle.internal.signing.SigningConfigData
import com.tyron.builder.gradle.internal.tasks.factory.VariantTaskCreationAction
import com.tyron.builder.signing.DefaultSigningConfig
import com.tyron.builder.utils.SynchronizedFile
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.util.concurrent.ExecutionException

@DisableCachingByDefault
@BuildAnalyzer(primaryTaskCategory = TaskCategory.VERIFICATION)
abstract class ValidateSigningTask : NonIncrementalTask() {

    @get:OutputDirectory
    abstract val dummyOutputDirectory: DirectoryProperty

    @get:Internal
    abstract val signingConfigData: Property<SigningConfigData>

    @get:Internal
    abstract val defaultDebugKeystoreLocation: Property<File>

    override fun doTaskAction() {
        val configData = signingConfigData.get()
        when {
            configData.storeFile == null -> throw InvalidUserDataException(
                "Keystore file not set for signing config ${configData.name}")
            isSigningConfigUsingTheDefaultDebugKeystore() -> createDefaultDebugKeystoreIfNeeded()
            configData.storeFile!!.isFile -> { } // Forzado de no nulidad seguro
            else -> throw InvalidUserDataException(
                "Keystore file '${configData.storeFile?.absolutePath}' not found")
        }
    }

    @Throws(ExecutionException::class, IOException::class)
    private fun createDefaultDebugKeystoreIfNeeded() {
        val location = defaultDebugKeystoreLocation.get()
        if (!location.parentFile.exists()) location.parentFile.mkdirs()
        
        SynchronizedFile
                .getInstanceWithMultiProcessLocking(location)
                .createIfAbsent { createDefaultDebugStore(it, this.logger) }
    }

    private fun isSigningConfigUsingTheDefaultDebugKeystore(): Boolean {
        val config = signingConfigData.get()
        return config.name == BuilderConstants.DEBUG &&
                config.storeFile?.name == "debug.keystore"
    }

    fun forceRerun(): Boolean {
        val storeFile = signingConfigData.get().storeFile
        return storeFile == null || !storeFile.isFile
    }

    class CreationAction(
        creationConfig: ApkCreationConfig,
        private val defaultKeystore: File
    ) : VariantTaskCreationAction<ValidateSigningTask, ApkCreationConfig>(creationConfig) {
        override val name: String get() = computeTaskName("validateSigning")
        override val type: Class<ValidateSigningTask> get() = ValidateSigningTask::class.java

        override fun handleProvider(taskProvider: TaskProvider<ValidateSigningTask>) {
            super.handleProvider(taskProvider)
            creationConfig.artifacts.setInitialProvider(taskProvider, ValidateSigningTask::dummyOutputDirectory)
                .on(InternalArtifactType.VALIDATE_SIGNING_CONFIG)
        }

        override fun configure(task: ValidateSigningTask) {
            super.configure(task)
            val signingConfig = creationConfig.signingConfigImpl ?: throw IllegalStateException("No signing config")
            task.signingConfigData.set(creationConfig.services.provider { SigningConfigData.fromSigningConfig(signingConfig) })
            task.defaultDebugKeystoreLocation.set(defaultKeystore)
            task.outputs.upToDateWhen { !task.forceRerun() }
        }
    }
}
