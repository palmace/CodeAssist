package com.tyron.builder.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty

/**
 * Interface representing SDK components needed by the build system.
 */
interface SdkComponents {
    val sdkDirectory: DirectoryProperty
    val ndkDirectory: DirectoryProperty
    val adbExecutable: RegularFileProperty
    val bootClasspath: ConfigurableFileCollection
    
    fun getAdditionalLibraries(): List<OptionalLibrary>
}
