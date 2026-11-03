package com.tyron.builder.api.variant.impl

import com.google.gson.stream.JsonReader
import com.tyron.builder.api.variant.BuiltArtifactsLoader
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.StringReader
import java.nio.file.Paths

class BuiltArtifactsLoaderImpl : BuiltArtifactsLoader {

    override fun load(folder: Directory): BuiltArtifactsImpl? {
        return loadFromFile(File(folder.asFile, BuiltArtifactsImpl.METADATA_FILE_NAME))
    }

    override fun load(fileCollection: FileCollection): BuiltArtifactsImpl? {
        val metadataFile = fileCollection.asFileTree.files.find { it.name == BuiltArtifactsImpl.METADATA_FILE_NAME }
        return loadFromFile(metadataFile)
    }

    fun load(folder: Provider<Directory>): BuiltArtifactsImpl? = load(folder.get())

    companion object {
        @JvmStatic
        fun loadFromDirectory(folder: File): BuiltArtifactsImpl? =
            loadFromFile(File(folder, BuiltArtifactsImpl.METADATA_FILE_NAME))

        @JvmStatic
        fun loadFromFile(inputFile: File?): BuiltArtifactsImpl? {
            if (inputFile == null || !inputFile.exists()) {
                return null
            }
            val content = inputFile.readText()
            val redirectedFile = ListingFileRedirect.maybeExtractRedirectedFile(inputFile, content)
            val relativePathToUse = redirectedFile?.parentFile?.toPath() ?: inputFile.parentFile.toPath()

            val reader = redirectedFile?.let { FileReader(it) } ?: StringReader(content)
            val buildOutputs = try {
                JsonReader(reader).use {
                    BuiltArtifactsTypeAdapter.read(it)
                }
            } catch (e: Exception) {
                throw IOException("Error parsing build artifacts", e)
            }

            return BuiltArtifactsImpl(
                artifactType = buildOutputs.artifactType,
                version = buildOutputs.version,
                applicationId = buildOutputs.applicationId,
                variantName = buildOutputs.variantName,
                elements = buildOutputs.elements.map { builtArtifact ->
                    BuiltArtifactImpl.make(
                        outputFile = relativePathToUse.resolve(Paths.get(builtArtifact.outputFile)).normalize().toString(),
                        versionCode = builtArtifact.versionCode,
                        versionName = builtArtifact.versionName,
                        // We need to cast or convert CommonBuiltArtifact back to BuiltArtifactImpl details if needed, 
                        // but here we are using BuiltArtifactImpl directly from the adapter.
                        variantOutputConfiguration = (builtArtifact as BuiltArtifactImpl).variantOutputConfiguration,
                        attributes = builtArtifact.attributes
                    )
                }
            )
        }
    }
}
