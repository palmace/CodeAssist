package com.android.aaptcompiler

import com.android.aaptcompiler.buffer.BigBuffer
import com.tyron.builder.log.ILogger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A pool of strings that can be referenced by index.
 */
class StringPool {
    data class Context(
        val priority: Int = Priority.NORMAL.priority,
        val config: ConfigDescription = ConfigDescription.DEFAULT
    ) {
        enum class Priority(val priority: Int) {
            LOW(0),
            NORMAL(1),
            HIGH(2)
        }
    }

    class Ref(val pool: StringPool, val index: Int) {
        fun value(): String = pool.strings[index]
        fun spans(): List<Span> = pool.styles[index] ?: emptyList()
        override fun toString(): String = value()
    }

    val strings = mutableListOf<String>()
    val styles = mutableMapOf<Int, List<Span>>()

    fun makeRef(value: String, context: Context = Context()): Ref {
        var index = strings.indexOf(value)
        if (index == -1) {
            index = strings.size
            strings.add(value)
        }
        return Ref(this, index)
    }

    fun makeRef(styleString: StyleString, context: Context = Context()): Ref {
        val ref = makeRef(styleString.str, context)
        if (styleString.spans.isNotEmpty()) {
            styles[ref.index] = styleString.spans
        }
        return ref
    }

    /**
     * Represents a span (tag) within a styled string.
     */
    data class Span(val name: String, var firstChar: Int, var lastChar: Int)

    /**
     * Serializes the string pool into a UTF-8 format within the given buffer.
     */
    fun flattenUtf8(buffer: BigBuffer, logger: ILogger?) {
        val encodingMask = 1 shl 8 // UTF-8 flag in ResStringPool_header

        // Header: type (2), headerSize (2), size (4), stringCount (4), styleCount (4), flags (4), stringsStart (4), stylesStart (4)
        val headerSize = 28
        val stringOffsets = IntArray(strings.size)
        val encodedStrings = strings.map { it.toByteArray(Charsets.UTF_8) }

        var currentOffset = 0
        for (i in encodedStrings.indices) {
            stringOffsets[i] = currentOffset
            // Simplified UTF-8 length encoding
            val len = encodedStrings[i].size
            if (len > 0x7F) {
                currentOffset += 2 // 2 bytes for length
            } else {
                currentOffset += 1 // 1 byte for length
            }
            currentOffset += len + 1 // + null terminator
        }

        val stringsStart = headerSize + (strings.size * 4) + (if (styles.isNotEmpty()) strings.size * 4 else 0)
        
        val totalSize = stringsStart + currentOffset

        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(0x0001.toShort()) // RES_STRING_POOL_TYPE
        out.putShort(headerSize.toShort())
        out.putInt(totalSize)
        out.putInt(strings.size)
        out.putInt(0) // styleCount - simplified to 0 for now in binary output
        out.putInt(encodingMask)
        out.putInt(stringsStart)
        out.putInt(0) // stylesStart

        for (offset in stringOffsets) {
            out.putInt(offset)
        }

        for (bytes in encodedStrings) {
            val len = bytes.size
            if (len > 0x7F) {
                out.put((0x80 or (len shr 8)).toByte())
                out.put((len and 0xFF).toByte())
            } else {
                out.put(len.toByte())
            }
            out.put(bytes)
            out.put(0.toByte())
        }

        buffer.write(out.array())
    }
}

data class StyleString(val str: String, val spans: List<StringPool.Span>)

class SpanNameAccessor {
    companion object {
        fun getName(span: StringPool.Span): StringPool.Ref {
            return StringPool().makeRef(span.name)
        }
    }
}
