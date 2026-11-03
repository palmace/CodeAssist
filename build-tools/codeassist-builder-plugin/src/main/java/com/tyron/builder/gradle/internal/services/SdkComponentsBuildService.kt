package com.tyron.builder.gradle.internal.services

import com.android.sdklib.AndroidVersion
import com.android.sdklib.IAndroidTarget
import com.tyron.builder.gradle.internal.SdkComponents
import com.tyron.builder.gradle.internal.OptionalLibrary
import com.tyron.builder.gradle.options.ProjectOptions
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.provider.Provider
import java.io.File

/**
 * Implementación de emergencia de SdkComponentsBuildService con VersionedSdkLoader.
 * Suturado: Completado RegistrationAction para evitar fallos de inyección.
 */
abstract class SdkComponentsBuildService : BuildService<SdkComponentsBuildService.Parameters>, SdkComponents {
    interface Parameters : BuildServiceParameters {
        val sdkDirectory: DirectoryProperty
    }

    abstract override val sdkDirectory: DirectoryProperty
    abstract override val ndkDirectory: DirectoryProperty
    abstract override val adbExecutable: RegularFileProperty
    abstract override val bootClasspath: ConfigurableFileCollection

    override fun getAdditionalLibraries(): List<com.tyron.builder.gradle.internal.OptionalLibrary> = emptyList()

    /**
     * Clase requerida por VersionedSdkLoaderService
     */
    abstract class VersionedSdkLoader {
        abstract val targetBootClasspathProvider: Provider<List<File>>
        abstract val coreLambdaStubsProvider: Provider<org.gradle.api.file.RegularFile>
        abstract val annotationsJarProvider: Provider<File>
        abstract val adbExecutableProvider: Provider<org.gradle.api.file.RegularFile>
        abstract val optionalLibrariesProvider: Provider<List<com.tyron.builder.gradle.internal.OptionalLibrary>>
        abstract val targetAndroidVersionProvider: Provider<AndroidVersion>
        abstract val additionalLibrariesProvider: Provider<List<com.tyron.builder.gradle.internal.OptionalLibrary>>
        abstract val sdkSetupCorrectly: Provider<Boolean>
        open val apiVersionsFile: Provider<org.gradle.api.file.RegularFile>
            get() = throw UnsupportedOperationException()
    }

    class RegistrationAction(
        project: Project,
        private val projectOptions: ProjectOptions
    ) : ServiceRegistrationAction<SdkComponentsBuildService, Parameters>(
        project,
        SdkComponentsBuildService::class.java
    ) {
        override fun configure(parameters: Parameters) {
            // Buscamos el SDK de forma agresiva
            val sdkDir = project.findProperty("android.sdk.path") as? String
                ?: System.getenv("ANDROID_HOME")
                ?: "/home/vit/Android/Sdk" // Default path for this environment
            
            parameters.sdkDirectory.set(File(sdkDir))
        }
    }
}
