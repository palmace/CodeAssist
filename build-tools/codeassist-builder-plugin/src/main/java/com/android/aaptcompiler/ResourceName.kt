package com.android.aaptcompiler

/**
 * Represents a resource name in the form package:type/entry.
 */
data class ResourceName(
    val pck: String? = "",
    val type: AaptResourceType = AaptResourceType.RAW,
    val entry: String? = ""
) : Comparable<ResourceName> {

    override fun toString(): String {
        val sb = StringBuilder()
        if (!pck.isNullOrEmpty()) {
            sb.append(pck).append(":")
        }
        sb.append(type.tagName).append("/").append(entry)
        return sb.toString()
    }

    override fun compareTo(other: ResourceName): Int {
        var cmp = (pck ?: "").compareTo(other.pck ?: "")
        if (cmp != 0) return cmp
        cmp = type.compareTo(other.type)
        if (cmp != 0) return cmp
        return (entry ?: "").compareTo(other.entry ?: "")
    }

    companion object {
        val EMPTY = ResourceName("", AaptResourceType.RAW, "")
    }
}

/**
 * Data class that holds a [ResourceName] and its source location.
 */
data class SourcedResourceName(val name: ResourceName, val line: Int = -1) {
    // Compatibility with ProtoDeserialize.kt
    val resourceName: ResourceName get() = name
}

/**
 * Helper to parse a string into a [ResourceName].
 */
fun parseResourceName(name: String): SourcedResourceName? {
    var str = name.trim()
    if (str.isEmpty()) return null

    var pck = ""
    val colonIndex = str.indexOf(':')
    if (colonIndex != -1) {
        pck = str.substring(0, colonIndex)
        str = str.substring(colonIndex + 1)
    }

    val slashIndex = str.indexOf('/')
    if (slashIndex == -1) return null

    val typeName = str.substring(0, slashIndex)
    val entry = str.substring(slashIndex + 1)

    val type = AaptResourceType.fromString(typeName) ?: return null
    return SourcedResourceName(ResourceName(pck, type, entry), -1)
}

data class ParsedReference(val reference: Reference, val createNew: Boolean)

/**
 * Parses a reference from a string.
 */
fun parseReference(value: String): ParsedReference? {
    var str = value.trim()
    if (str.isEmpty()) return null

    var createNew = false
    var type = Reference.Type.RESOURCE

    if (str.startsWith('@')) {
        str = str.substring(1)
        if (str.startsWith('+')) {
            str = str.substring(1)
            createNew = true
        }
    } else if (str.startsWith('?')) {
        str = str.substring(1)
        type = Reference.Type.ATTRIBUTE
    } else {
        return null
    }

    val sourcedName = parseResourceName(str) ?: return null
    val reference = Reference(sourcedName.name, type = type)
    return ParsedReference(reference, createNew)
}
