package com.tyron.builder.gradle.internal

import com.tyron.builder.gradle.internal.component.ComponentCreationConfig
import com.tyron.builder.gradle.internal.services.SdkComponentsBuildService
import com.tyron.builder.gradle.internal.services.getBuildService
import org.gradle.api.NonExtensible
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import java.io.File

/** This can be used by tasks requiring build-tools executables as input with [org.gradle.api.tasks.Nested]. */
@NonExtensible
abstract class BuildToolsExecutableInput {
    @get:Internal //used to create the SdkLoader but not an dependency input.
    abstract val compileSdkVersion: Property<String>

    @get:Internal
    abstract val sdkBuildService: Property<SdkComponentsBuildService>

    fun adbExecutable(): Provider<RegularFile> = sdkBuildService.flatMap { it.adbExecutable }

    fun splitSelectExecutable(): Provider<File> = TODO()

    fun supportBlasLibFolderProvider(): Provider<File> = TODO()

    fun supportNativeLibFolderProvider(): Provider<File> = TODO()

    fun aidlExecutableProvider(): Provider<File> = TODO()

    fun aidlFrameworkProvider(): Provider<File> = TODO()
}

fun BuildToolsExecutableInput.initialize(creationConfig: ComponentCreationConfig) {
    sdkBuildService.set(getBuildService(creationConfig.services.buildServiceRegistry))
    compileSdkVersion.set(creationConfig.global.compileSdkHashString)
}
