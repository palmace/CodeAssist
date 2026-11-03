package com.tyron.builder.dexing

import com.android.SdkConstants
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.SortedMap
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.streams.toList

/**
 * Returns `true` if the given file's extension is `.jar`, ignoring case. It may or may not exist.
 */
val isJarFile: (File) -> Boolean = { it.extension.equals(SdkConstants.EXT_JAR, ignoreCase = true) }

/**
 * Returns a sorted list of files in the given directory whose relative paths satisfy the given
 * filter.
 */
fun getSortedFilesInDir(
    dir: Path,
    filter: (relativePath: String) -> Boolean = { true }
): List<Path> {
    return Files.walk(dir).use { files ->
        files
            .filter { file -> filter(dir.relativize(file).toString()) }
            .toList()
            .sortedWith(
                Comparator { left, right ->
                    // Normalización manual para evitar dependencia de GFileUtils
                    val leftPath = left.toString().replace(File.separatorChar, '/')
                    val rightPath = right.toString().replace(File.separatorChar, '/')
                    leftPath.compareTo(rightPath)
                }
            )
    }
}

/**
 * Returns a sorted list of the relative paths of the entries in the given jar.
 */
fun getSortedRelativePathsInJar(
    jar: File,
    filter: (relativePath: String) -> Boolean = { true }
): List<String> {
    ZipInputStream(jar.inputStream().buffered()).use { stream ->
        val relativePaths = mutableListOf<String>()
        while (true) {
            val entry = stream.nextEntry ?: break
            if (filter(entry.name)) {
                relativePaths.add(entry.name)
            }
        }
        return relativePaths.sorted()
    }
}

/**
 * Returns a sorted map of the entries in the given jar with their contents.
 */
fun getSortedRelativePathsInJarWithContents(
    jar: File,
    filter: (relativePath: String) -> Boolean = { true }
): SortedMap<String, ByteArray> {
    return ZipFile(jar).use { zipFile ->
        zipFile.entries()
            .toList()
            .filter { entry -> filter(entry.name) }.associate { entry ->
                entry.name to zipFile.getInputStream(entry).buffered().use { stream ->
                    stream.readBytes()
                }
            }
            .toSortedMap()
    }
}
