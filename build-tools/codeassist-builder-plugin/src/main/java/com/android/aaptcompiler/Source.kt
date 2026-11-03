package com.android.aaptcompiler

import com.android.ide.common.blame.SourceFilePosition
import com.android.ide.common.blame.SourcePosition
import java.io.File

/**
 * Represents a location in a source file.
 * Used for error reporting and tracking resource origins.
 */
data class Source(
    val path: String,
    val line: Int = -1,
    val column: Int = -1
) {
    override fun toString(): String {
        var result = path
        if (line != -1) {
            result += ":$line"
            if (column != -1) {
                result += ":$column"
            }
        }
        return "$result: "
    }

    fun isNotEmpty(): Boolean = path.isNotEmpty()

    fun toSourceFilePosition() =
        SourceFilePosition(File(path), SourcePosition(
            line,
            column,
            -1,
            line,
            column,
            -1
        ))

    companion object {
        val EMPTY = Source("")

        fun fromSourceFilePosition(filePosition: SourceFilePosition) =
            Source(filePosition.file.sourcePath ?: "",
                filePosition.position.startLine,
                filePosition.position.startColumn)
    }
}
