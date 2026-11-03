package com.android.aaptcompiler

import android.aapt.pb.internal.ResourcesInternal
import com.android.aapt.Resources
import com.android.aaptcompiler.proto.serializeCompiledFileToPb
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A container for compiled resources in the AAPT2 .flat format.
 */
class Container(private val outputStream: OutputStream, private val count: Int) {

    init {
        // Write the Header: Magic (AAPT), Version (1), and total entries
        val header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        header.put("AAPT".toByteArray(Charsets.US_ASCII))
        header.putInt(1) // Version
        header.putInt(count)
        outputStream.write(header.array())
    }

    /**
     * Adds a ResourceTable entry to the container.
     */
    fun addResTableEntry(pbTable: Resources.ResourceTable) {
        // For RES_TABLE, the header is empty and the table is in the payload
        writeEntry(EntryType.RES_TABLE, ByteArray(0), pbTable.toByteArray())
    }

    /**
     * Adds a file entry (like a PNG or raw file) to the container.
     */
    fun addFileEntry(inputStream: InputStream, resourceFile: ResourceFile) {
        val pbFile = serializeCompiledFileToPb(resourceFile)
        val headerData = pbFile.toByteArray()
        val fileData = inputStream.readBytes()
        
        writeEntry(EntryType.RES_FILE, headerData, fileData)
    }

    /**
     * Adds an XML entry (compiled XML) to the container.
     */
    fun addXmlEntry(resource: XmlResource) {
        val pbFile = serializeCompiledFileToPb(resource.file)
        val headerData = pbFile.toByteArray()
        val xmlData = resource.root?.toByteArray() ?: ByteArray(0)
        
        writeEntry(EntryType.RES_FILE, headerData, xmlData)
    }

    private fun writeEntry(type: EntryType, headerData: ByteArray, payloadData: ByteArray) {
        val headerPadding = (4 - (headerData.size % 4)) % 4
        val payloadPadding = (4 - (payloadData.size % 4)) % 4
        
        val totalSize = headerData.size.toLong() + headerPadding + payloadData.size + payloadPadding
        
        val entryHeader = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        entryHeader.putInt(type.value)
        entryHeader.putLong(totalSize)
        entryHeader.putInt(headerData.size)
        
        outputStream.write(entryHeader.array())
        outputStream.write(headerData)
        if (headerPadding > 0) {
            outputStream.write(ByteArray(headerPadding))
        }
        outputStream.write(payloadData)
        if (payloadPadding > 0) {
            outputStream.write(ByteArray(payloadPadding))
        }
    }

    private enum class EntryType(val value: Int) {
        RES_TABLE(0x00000000),
        RES_FILE(0x00000001)
    }
}
