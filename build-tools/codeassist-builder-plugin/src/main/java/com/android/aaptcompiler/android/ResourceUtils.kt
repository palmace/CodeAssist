package com.android.aaptcompiler.android

import com.android.aaptcompiler.ResourceName
import com.android.aaptcompiler.Reference

/**
 * Utilities for resource ID manipulation and string parsing.
 */

fun resourceIdFromParts(packageId: Byte, typeId: Byte, entryId: Short): Int {
    return (packageId.toInt() shl 24) or (typeId.toInt() shl 16) or (entryId.toInt() and 0xffff)
}

fun Int.isValidId(): Boolean = this != 0 && (this and 0xff000000.toInt()) != 0

fun Int.isValidDynamicId(): Boolean = this != 0 && (this and 0xff000000.toInt()) == 0

fun Int.getPackageId(): Byte = (this ushr 24).toByte()
fun Int.getTypeId(): Byte = (this ushr 16).toByte()
fun Int.getEntryId(): Short = (this and 0xffff).toShort()

fun isValidResourceEntryName(name: String): Boolean {
    if (name.isEmpty()) return false
    val first = name[0]
    if (!first.isLetter() && first != '_') return false
    for (i in 1 until name.length) {
        val c = name[i]
        if (!c.isLetterOrDigit() && c != '_' && c != '.') return false
    }
    return true
}

data class ResValue(val dataType: DataType, val data: Int) {
    enum class DataType(val byteValue: Byte) {
        NULL(0x00),
        REFERENCE(0x01),
        ATTRIBUTE(0x02),
        STRING(0x03),
        FLOAT(0x04),
        DIMENSION(0x05),
        FRACTION(0x06),
        DYNAMIC_REFERENCE(0x07),
        DYNAMIC_ATTRIBUTE(0x08),
        INT_DEC(0x10),
        INT_HEX(0x11),
        INT_BOOLEAN(0x12),
        INT_COLOR_ARGB8(0x1c),
        INT_COLOR_RGB8(0x1d),
        INT_COLOR_ARGB4(0x1e),
        INT_COLOR_RGB4(0x1f);
    }
    
    object NullFormat {
        const val UNDEFINED = 0
        const val EMPTY = 1
    }
}

fun stringToInt(value: String): ResValue? {
    if (value.startsWith("0x")) {
        return try { ResValue(ResValue.DataType.INT_HEX, value.substring(2).toLong(16).toInt()) } catch(e: Exception) { null }
    }
    return try { ResValue(ResValue.DataType.INT_DEC, value.toInt()) } catch(e: Exception) { null }
}

fun parseAsBool(value: String): Boolean? {
    return when (value.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }
}

fun parseHex(codePoint: Int): Int {
    return when (codePoint.toChar()) {
        in '0'..'9' -> codePoint - '0'.toInt()
        in 'a'..'f' -> codePoint - 'a'.toInt() + 10
        in 'A'..'F' -> codePoint - 'A'.toInt() + 10
        else -> -1
    }
}

fun Int.deviceToHost(): Int = this 

class ResStringPool {
    val strings = mutableListOf<String>()
    companion object {
        fun get(buffer: java.nio.ByteBuffer, size: Int): ResStringPool = ResStringPool()
    }
}

class LocaleValue {
    fun initFromBcp47Tag(tag: String): Boolean = true
    fun writeTo(config: com.android.aaptcompiler.ConfigDescription) {}
}
