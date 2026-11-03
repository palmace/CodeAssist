package com.tyron.builder.gradle.internal.services

import com.android.sdklib.AndroidVersion
import com.tyron.builder.BuildModule
import com.tyron.builder.gradle.internal.services.SdkComponentsBuildService.VersionedSdkLoader
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File

/**
 * Object responsible for creating (and memoizing) the instance of VersionedSdkLoader.
 */
class VersionedSdkLoaderService(
    private val services: BaseServices,
    private val project: Project,
    private val compileSdkVersionAction: () -> String?,
) {
    val versionedSdkLoader: Provider<VersionedSdkLoader> = project.provider {
        object : VersionedSdkLoader() {
            override val targetBootClasspathProvider: Provider<List<File>>
                get() = project.provider { listOf(BuildModule.getAndroidJar()) }
            
            override val coreLambdaStubsProvider: Provider<RegularFile>
                get() = project.objects.fileProperty().fileProvider(project.provider { BuildModule.getLambdaStubs() })
            
            override val annotationsJarProvider: Provider<File>
                get() = project.provider { File("annotations.jar") }
            
            override val adbExecutableProvider: Provider<RegularFile>
                get() = project.objects.fileProperty().fileProvider(project.provider { File("adb") })
            
            override val optionalLibrariesProvider: Provider<List<com.tyron.builder.gradle.internal.OptionalLibrary>>
                get() = project.provider { emptyList<com.tyron.builder.gradle.internal.OptionalLibrary>() }
            
            override val targetAndroidVersionProvider: Provider<AndroidVersion>
                get() = project.provider { AndroidVersion(31) }
            
            override val additionalLibrariesProvider: Provider<List<com.tyron.builder.gradle.internal.OptionalLibrary>>
                get() = project.provider { emptyList<com.tyron.builder.gradle.internal.OptionalLibrary>() }
            
            override val sdkSetupCorrectly: Provider<Boolean>
                get() = project.provider { true }
            
            override val apiVersionsFile: Provider<RegularFile>
                get() = project.objects.fileProperty()
        }
    }
}
