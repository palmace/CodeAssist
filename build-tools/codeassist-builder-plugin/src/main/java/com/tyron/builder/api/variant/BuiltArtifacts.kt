package com.tyron.builder.api.variant

import com.tyron.builder.api.artifact.Artifact
import org.gradle.api.file.Directory
import org.gradle.workers.WorkParameters
import java.io.File
import java.io.Serializable

/**
 * Represents a [Collection] of [BuiltArtifact] produced by a [org.gradle.api.Task].
 */
interface BuiltArtifacts {

    companion object {
        const val METADATA_FILE_VERSION = 3
    }

    val version: Int get() = METADATA_FILE_VERSION

    val artifactType: Artifact<*>

    val applicationId: String

    val variantName: String

    val elements: Collection<BuiltArtifact>

    fun save(out: Directory)

    /**
     * Re-introducidos para facilitar el acceso a artefactos específicos.
     */
    fun getBuiltArtifact(outputType: VariantOutputConfiguration.OutputType): BuiltArtifact? {
        return elements.firstOrNull { it.outputType == outputType }
    }

    fun getBuiltArtifact(variantOutputConfiguration: VariantOutputConfiguration): BuiltArtifact? {
        return elements.firstOrNull {
            it.outputType == variantOutputConfiguration.outputType &&
            it.filters == variantOutputConfiguration.filters
        }
    }

    interface TransformParams: WorkParameters, Serializable {
        val output: File
    }
}
