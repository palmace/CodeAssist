package com.tyron.builder.gradle.internal.scope

import com.tyron.builder.api.artifact.Artifact
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File
import java.util.Locale

/**
 * Returns a suitable output directory for this receiving artifact type.
 */
fun Artifact<*>.getOutputDir(parentDir: File): File {
    val categoryDir = File(parentDir, category.name.lowercase(Locale.US))
    return File(categoryDir, getFolderName())
}

fun Artifact<*>.getIntermediateOutputDir(parentDir: File): File {
    val intermediatesDir = File(parentDir, "intermediates")
    return File(intermediatesDir, getFolderName())
}

/**
 * Returns a [File] representing the artifact type location.
 */
fun Artifact<*>.getOutputPath(
    buildDirectory: DirectoryProperty,
    variantIdentifier: String,
    vararg paths: String,
    forceFilename: String = ""
): File {
    val baseDir = getOutputDir(buildDirectory.get().asFile)
    var resultFile = File(baseDir, variantIdentifier)
    for (path in paths) {
        resultFile = File(resultFile, path)
    }
    val fileName = if (forceFilename.isEmpty()) getFileSystemLocationName() ?: "" else forceFilename
    return File(resultFile, fileName)
}

fun Artifact<*>.getIntermediateOutputPath(
    buildDirectory: DirectoryProperty,
    variantIdentifier: String,
    vararg paths: String,
    forceFilename: String = ""
): File {
    val baseDir = getIntermediateOutputDir(buildDirectory.get().asFile)
    var resultFile = File(baseDir, variantIdentifier)
    for (path in paths) {
        resultFile = File(resultFile, path)
    }
    val fileName = if (forceFilename.isEmpty()) getFileSystemLocationName() ?: "" else forceFilename
    return File(resultFile, fileName)
}

/**
 * Converts a [FileCollection] to a [Provider] of a [List] of [RegularFile].
 */
fun FileCollection.getRegularFiles(projectDirectory: Directory): Provider<List<RegularFile>> =
    elements.map { list ->
        list.filter { it.asFile.isFile }
            .map { projectDirectory.file(it.asFile.absolutePath) }
    }

/**
 * Converts a [FileCollection] to a [Provider] of a [List] of [Directory].
 */
fun FileCollection.getDirectories(projectDirectory: Directory): Provider<List<Directory>> =
    elements.map { list ->
        list.filter { it.asFile.isDirectory }
            .map { projectDirectory.dir(it.asFile.absolutePath) }
    }
