package com.android.aaptcompiler

import java.io.File

/**
 * Holds information about a resource file's path and its components.
 */
data class ResourcePathData(
    val file: File,
    val resourceDirectory: String,
    val name: String,
    var extension: String,
    val config: ConfigDescription,
    val source: Source,
    val type: AaptResourceType? = null
) {
    fun getIntermediateContainerFilename(): String {
        return "${resourceDirectory}_${name}.${extension}.flat"
    }
}

fun extractPathData(file: File, sourcePath: String = file.absolutePath): ResourcePathData {
    val parentFile = file.parentFile
    val resourceDirectory = parentFile.name
    val fileName = file.name
    val dotIndex = fileName.indexOf('.')
    val name = if (dotIndex != -1) fileName.substring(0, dotIndex) else fileName
    val extension = if (dotIndex != -1) fileName.substring(dotIndex + 1) else ""
    
    // Simplified config extraction
    val config = ConfigDescription() 
    val source = Source(sourcePath)
    val type = resourceTypeFromTag(resourceDirectory)

    return ResourcePathData(file, resourceDirectory, name, extension, config, source, type)
}

class ResourceCompilationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
