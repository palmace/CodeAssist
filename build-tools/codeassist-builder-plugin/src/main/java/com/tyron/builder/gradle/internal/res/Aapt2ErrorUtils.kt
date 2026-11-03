package com.tyron.builder.gradle.internal.res

import com.android.ide.common.resources.MergingException
import com.tyron.builder.gradle.internal.utils.toSystemIndependentPath
import java.io.File
import java.util.regex.Pattern

/**
 * Utilidades para el manejo de errores de AAPT2.
 */
object Aapt2ErrorUtils {
    private val AAPT2_ERROR_PATTERN = Pattern.compile("^(.*):(\\d+): error: (.*)$")

    @JvmStatic
    fun blameLoggerFor(logger: com.android.utils.ILogger): BlameLoggerWrapper {
        return if (logger is com.tyron.builder.log.ILogger) {
            Aapt2ErrorUtilsJava.blameLoggerFor(logger)
        } else {
            // Fallback: This should ideally be a LoggerWrapper
            Aapt2ErrorUtilsJava.blameLoggerFor(logger as com.tyron.builder.log.ILogger)
        }
    }

    @JvmStatic
    fun parseAapt2Error(error: String): MergingException? {
        val matcher = AAPT2_ERROR_PATTERN.matcher(error)
        if (matcher.matches()) {
            val path = matcher.group(1).toSystemIndependentPath()
            val message = matcher.group(3)
            return MergingException.withMessage(message)
                .withFile(File(path))
                .build()
        }
        return null
    }
}

/**
 * Función de compatibilidad para cuando se recibe un logger de Android (com.android.utils.ILogger).
 */
fun blameLoggerFor(request: Any, logger: com.android.utils.ILogger): BlameLoggerWrapper {
    return Aapt2ErrorUtils.blameLoggerFor(logger)
}

fun blameLoggerFor(request: Any, logger: com.tyron.builder.log.ILogger): BlameLoggerWrapper {
    return Aapt2ErrorUtilsJava.blameLoggerFor(logger)
}
