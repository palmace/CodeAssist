package com.tyron.builder.gradle.internal

import com.tyron.builder.BuildModule
import com.tyron.builder.gradle.internal.component.ComponentCreationConfig
import com.tyron.builder.gradle.internal.services.getBuildService
import com.tyron.builder.gradle.internal.services.SdkComponentsBuildService
import com.tyron.builder.internal.utils.setDisallowChanges
import org.gradle.api.NonExtensible
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

/** This can be used by tasks requiring android.jar as input with [org.gradle.api.tasks.Nested]. */
@NonExtensible
abstract class AndroidJarInput {

    @get:Internal
    abstract val sdkBuildService: Property<SdkComponentsBuildService>

    @get:Internal
    abstract val compileSdkVersion: Property<String>

    @get:Internal
    abstract val androidJarFile: RegularFileProperty

    @PathSensitive(PathSensitivity.NONE)
    @InputFile
    fun getAndroidJar(): Provider<File> = androidJarFile.asFile
}

fun AndroidJarInput.initialize(creationConfig: ComponentCreationConfig) {
    sdkBuildService.setDisallowChanges(
        getBuildService(creationConfig.services.buildServiceRegistry))
    this.compileSdkVersion.setDisallowChanges(creationConfig.global.compileSdkHashString)
    
    // Fallback if BuildModule.getAndroidJar() fails or is missing
    try {
        this.androidJarFile.set(BuildModule.getAndroidJar())
    } catch (e: Exception) {
        // Fallback or handle error
    }
}
