package com.tyron.builder.gradle.internal.tasks.manifest

import com.tyron.builder.compiler.manifest.ManifestProvider
import java.io.File

/**
 * Implementation of [ManifestProvider] for Gradle tasks.
 */
class ManifestProviderImpl(
    private val manifest: File,
    private val name: String
) : ManifestProvider {

    override fun getManifest(): File {
        return manifest
    }

    override fun getName(): String {
        return name
    }
}
