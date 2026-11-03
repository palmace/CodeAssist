package com.tyron.builder.gradle.internal.tasks.factory

import com.android.sdklib.IAndroidTarget
import com.google.common.collect.ImmutableList
import com.tyron.builder.gradle.internal.OptionalLibrary
import com.tyron.builder.gradle.internal.OptionalLibraryImpl
import com.tyron.builder.gradle.internal.services.SdkComponentsBuildService
import com.tyron.builder.gradle.internal.dsl.CommonExtensionImpl
import com.tyron.builder.gradle.internal.publishing.AndroidArtifacts
import com.tyron.builder.gradle.internal.scope.BootClasspathBuilder
import com.tyron.builder.gradle.internal.services.ProjectServices
import com.tyron.builder.gradle.internal.services.TaskCreationServicesImpl
import com.tyron.builder.gradle.internal.services.VersionedSdkLoaderService
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

class BootClasspathConfigImpl(
    val project: Project,
    private val projectServices: ProjectServices,
    private val versionedSdkLoaderService: VersionedSdkLoaderService,
    // TODO: Remove dependency on common extension
    private val extension: CommonExtensionImpl<*,*,*,*>?,
    private val forUnitTest: Boolean
): BootClasspathConfig {

    private fun transformOptionalLibraries(
        libraries: List<com.tyron.builder.gradle.internal.OptionalLibrary>
    ): List<com.tyron.builder.gradle.internal.OptionalLibrary> {
        return libraries
    }

    override val fullBootClasspath: FileCollection by lazy {
        project.files(fullBootClasspathProvider)
    }

    override val fullBootClasspathProvider: Provider<List<RegularFile>> by lazy {
        val property = project.objects.listProperty(RegularFile::class.java)
        val versionedSdkLoader = versionedSdkLoaderService.versionedSdkLoader

        val taskService = TaskCreationServicesImpl(projectServices)

        property.set(
            BootClasspathBuilder.computeClasspath(
                taskService,
                project.objects,
                versionedSdkLoader
                    .flatMap { it.targetBootClasspathProvider },
                versionedSdkLoader
                    .flatMap { it.targetAndroidVersionProvider },
                versionedSdkLoader
                    .flatMap { it.additionalLibrariesProvider }
                    .map { transformOptionalLibraries(it) },
                versionedSdkLoader
                    .flatMap { it.optionalLibrariesProvider }
                    .map { transformOptionalLibraries(it) },
                versionedSdkLoader
                    .flatMap { it.annotationsJarProvider },
                addAllOptionalLibraries = true,
                ImmutableList.of()
            )
        )

        property.disallowChanges()
        property.finalizeValueOnRead()
        if (!forUnitTest) {
            property.disallowUnsafeRead()
        }

        property
    }

    override val filteredBootClasspath: Provider<List<RegularFile>> by lazy {
        val property = project.objects.listProperty(RegularFile::class.java)
        val versionedSdkLoader = versionedSdkLoaderService.versionedSdkLoader

        val taskService = TaskCreationServicesImpl(projectServices)

        property.set(
            BootClasspathBuilder.computeClasspath(
                taskService,
                project.objects,
                versionedSdkLoader.flatMap { it.targetBootClasspathProvider },
                versionedSdkLoader.flatMap { it.targetAndroidVersionProvider },
                versionedSdkLoader.flatMap { it.additionalLibrariesProvider }
                    .map { transformOptionalLibraries(it) },
                versionedSdkLoader.flatMap { it.optionalLibrariesProvider }
                    .map { transformOptionalLibraries(it) },
                versionedSdkLoader.flatMap { it.annotationsJarProvider },
                false,
                ImmutableList.copyOf(extension?.libraryRequests ?: emptyList())
            )
        )

        property.disallowChanges()
        property.finalizeValueOnRead()

        property
    }

    override val bootClasspath: Provider<List<RegularFile>> by lazy {
        val property = project.objects.listProperty(RegularFile::class.java)
        val versionedSdkLoader = versionedSdkLoaderService.versionedSdkLoader

        property.addAll(filteredBootClasspath)
        if (extension?.compileOptions?.targetCompatibility?.isJava8Compatible != false) {
            property.add(
                versionedSdkLoader
                    .flatMap { it.coreLambdaStubsProvider }
            )
        }

        property.disallowChanges()
        property.finalizeValueOnRead()
        if (!forUnitTest) {
            property.disallowUnsafeRead()
        }

        property
    }

    internal lateinit var androidJar: Configuration

    override val mockableJarArtifact: FileCollection by lazy {
        val attributes =
            Action { container: AttributeContainer ->
                container
                    .attribute(
                        AndroidArtifacts.ARTIFACT_TYPE,
                        AndroidArtifacts.TYPE_MOCKABLE_JAR
                    )
                    .attribute(
                        AndroidArtifacts.MOCKABLE_JAR_RETURN_DEFAULT_VALUES,
                        false
                    )
            }
        androidJar
            .incoming
            .artifactView { config -> config.attributes(attributes) }
            .artifacts
            .artifactFiles
    }
}
