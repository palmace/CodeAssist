package com.android.aaptcompiler

import com.tyron.builder.log.ILogger
import javax.xml.stream.Location

/**
 * Utility to create a [Source] object representing a specific location in a file.
 */
fun blameSource(
    source: Source,
    line: Int? = source.line,
    column: Int? = null
): Source =
    Source(source.path, line ?: -1, column ?: -1)

/**
 * Utility to create a [Source] object from a [Location] in an XML file.
 */
fun blameSource(
    source: Source,
    location: Location
): Source =
    Source(source.path, location.lineNumber, location.columnNumber)

/**
 * Handles logging with source location information, allowing for mapping back to original files.
 */
open class BlameLogger(
    val logger: ILogger,
    private val userVisibleSourceTransform: (String) -> String = { it },
    val blameMap: (Source) -> Source = { it }
) {

    constructor(logger: ILogger, blameMap: (Source) -> Source = { it })
            : this(logger, { it }, blameMap)

    fun error(message: String, source: Source? = null, throwable: Throwable? = null) {
        if (source != null) {
            logger.error("${getOutputSource(source)}$message")
        } else {
            logger.error(message)
        }
    }

    fun warning(message: String, source: Source? = null) {
        if (source != null)
            logger.warning("${getOutputSource(source)}$message")
        else
            logger.warning(message)
    }

    fun info(message: String, source: Source? = null) {
        if (source != null)
            logger.info("${getOutputSource(source)}$message")
        else
            logger.info(message)
    }

    fun lifecycle(message: String, source: Source? = null) {
        // ILogger doesn't have lifecycle, using info
        if (source != null)
            logger.info("${getOutputSource(source)}$message")
        else
            logger.info(message)
    }

    fun quiet(message: String, source: Source? = null) {
        if (source != null)
            logger.quiet("${getOutputSource(source)}$message")
        else
            logger.quiet(message)
    }

    fun verbose(message: String, source: Source? = null) {
        if (source != null)
            logger.verbose("${getOutputSource(source)}$message")
        else
            logger.verbose(message)
    }

    /**
     * Transforms the given source to a version suitable for user display and maps it back
     * to its original source if necessary.
     */
    fun getOutputSource(source: Source) : Source {
        return getOriginalSource(
                source.copy(path = userVisibleSourceTransform(source.path))
            )
    }

    /**
     * Maps the given source back to its original location using the [blameMap].
     */
    fun getOriginalSource(source: Source): Source {
        return blameMap(source)
    }
}
