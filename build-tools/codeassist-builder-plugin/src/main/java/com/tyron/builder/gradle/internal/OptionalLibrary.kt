package com.tyron.builder.gradle.internal

import java.io.File
import java.io.Serializable

/**
 * Interface representing an optional library needed by the build system.
 */
interface OptionalLibrary : Serializable {
    val name: String
    val jar: File
    val manifest: File?
}

data class OptionalLibraryImpl(
    override val name: String,
    override val jar: File,
    override val manifest: File? = null
) : OptionalLibrary
