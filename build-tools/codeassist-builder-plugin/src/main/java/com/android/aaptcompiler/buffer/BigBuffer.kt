package com.android.aaptcompiler.buffer

import java.io.ByteArrayOutputStream

/**
 * A simple buffer that can grow as needed.
 */
class BigBuffer(initialCapacity: Int) {
    private val stream = ByteArrayOutputStream(initialCapacity)

    fun write(bytes: ByteArray) {
        stream.write(bytes)
    }

    fun toBytes(): ByteArray = stream.toByteArray()
}
