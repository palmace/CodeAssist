package com.tyron.builder.gradle.internal.utils

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Arsenal de utilidades nativas para sustituir GFileUtils y evitar errores de resolución en Kotlin.
 */

fun File.deleteIfExists() {
    if (exists()) {
        if (!delete()) {
            // Intentar con lógica de fuerza si es necesario
            deleteRecursively()
        }
    }
}

@Throws(IOException::class)
fun File.cleanOutputDir() {
    if (exists()) {
        deleteRecursively()
    }
    if (!mkdirs()) {
        throw IOException("No se pudo crear el directorio de salida: $absolutePath")
    }
}

fun String.toSystemIndependentPath(): String = replace('\\', '/')

fun String.toSystemDependentPath(): String = replace('/', File.separatorChar)

fun File.join(vararg paths: String): File {
    var result = this
    for (path in paths) {
        result = File(result, path)
    }
    return result
}
