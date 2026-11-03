package com.android.aaptcompiler

import com.android.aaptcompiler.android.ResValue

/**
 * Base class for all resource values.
 */
abstract class Value {
    var source: Source = Source("")
    var comment: String = ""
    var weak: Boolean = false

    abstract fun clone(): Value
}

/**
 * An Item is a Value that can be used in a simple resource (e.g. string, color).
 */
abstract class Item : Value() {
    var translatable: Boolean = true
}

class Id : Item() {
    override fun clone(): Id = Id().also {
        it.source = source
        it.comment = comment
        it.weak = weak
    }
}

class Reference(
    var name: ResourceName = ResourceName.EMPTY,
    var id: Int? = null,
    var isPrivate: Boolean = false,
    var type: Type = Type.RESOURCE
) : Item() {
    enum class Type {
        RESOURCE,
        ATTRIBUTE
    }

    var referenceType: Type = type
    var allowRaw: Boolean = false
    var typeFlags: Int = 0

    override fun clone(): Reference = Reference(name, id, isPrivate, type).also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.referenceType = referenceType
        it.allowRaw = allowRaw
        it.typeFlags = typeFlags
    }
}

class BinaryPrimitive(val value: ResValue) : Item() {
    override fun clone(): BinaryPrimitive = BinaryPrimitive(
        ResValue(value.dataType, value.data)
    ).also {
        it.source = source
        it.comment = comment
        it.weak = weak
    }

    fun flatten(): ResValue = value
}

class FileReference(val path: StringPool.Ref) : Item() {
    var file: java.io.File? = null
    var type: Type = Type.Unknown

    enum class Type {
        Unknown,
        Png,
        BinaryXml,
        ProtoXml
    }

    override fun clone(): FileReference = FileReference(path).also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.file = file
        it.type = type
    }
}

class BasicString(
    val ref: StringPool.Ref,
    val untranslatables: List<UntranslatableSection> = emptyList()
) : Item() {
    override fun clone(): BasicString = BasicString(ref, untranslatables).also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.translatable = translatable
    }
    override fun toString(): String = ref.value()
}

class StyledString(
    val ref: StringPool.Ref,
    val untranslatables: List<UntranslatableSection> = emptyList()
) : Item() {
    override fun clone(): StyledString = StyledString(ref, untranslatables).also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.translatable = translatable
    }
}

class RawString(val ref: StringPool.Ref) : Item() {
    override fun clone(): RawString = RawString(ref).also {
        it.source = source
        it.comment = comment
        it.weak = weak
    }
}

class AttributeResource(val typeMask: Int) : Value() {
    var weakAttr: Boolean = false
    var minInt: Int = Int.MIN_VALUE
    var maxInt: Int = Int.MAX_VALUE
    val symbols = mutableListOf<Symbol>()

    data class Symbol(val symbol: Reference, val value: Int, val type: Byte = 0)

    override fun clone(): AttributeResource = AttributeResource(typeMask).also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.weakAttr = weakAttr
        it.minInt = minInt
        it.maxInt = maxInt
        it.symbols.addAll(symbols)
    }

    fun isCompatibleWith(other: AttributeResource): Boolean = typeMask == other.typeMask
}

class Style : Value() {
    var parent: Reference? = null
    var parentInferred: Boolean = false
    val entries = mutableListOf<Entry>()

    data class Entry(val key: Reference, val value: Item)

    override fun clone(): Style = Style().also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.parent = parent
        it.parentInferred = parentInferred
        it.entries.addAll(entries)
    }
}

class Styleable : Value() {
    val entries = mutableListOf<Reference>()
    override fun clone(): Styleable = Styleable().also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.entries.addAll(entries)
    }
}

class ArrayResource : Value() {
    var translatable: Boolean = true
    val elements = mutableListOf<Item>()
    override fun clone(): ArrayResource = ArrayResource().also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.translatable = translatable
        it.elements.addAll(elements)
    }
}

class Plural : Value() {
    enum class Type {
        ZERO, ONE, TWO, FEW, MANY, OTHER;
        
        companion object {
            val TYPES = values()
        }
    }
    val values = arrayOfNulls<Item>(Type.values().size)

    fun setValue(type: Type, value: Item) {
        values[type.ordinal] = value
    }

    override fun clone(): Plural = Plural().also {
        it.source = source
        it.comment = comment
        it.weak = weak
        System.arraycopy(values, 0, it.values, 0, values.size)
    }
}

class Macro(
    var rawValue: String = "",
    var styleString: StyleString = StyleString("", emptyList()),
    var untranslatables: List<UntranslatableSection> = emptyList()
) : Value() {
    data class Namespace(val prefix: String, val packageName: String, val isPrivate: Boolean)
    var aliasNamespaces = mutableListOf<Namespace>()

    override fun clone(): Macro = Macro(rawValue, styleString, untranslatables).also {
        it.source = source
        it.comment = comment
        it.weak = weak
        it.aliasNamespaces.addAll(aliasNamespaces)
    }
}

data class UntranslatableSection(var startIndex: Int, var endIndex: Int = 0) {
    fun shift(offset: Int): UntranslatableSection = UntranslatableSection(startIndex + offset, endIndex + offset)
}

fun makeNull(): Item = Id()

fun Value?.isTruthy(): Boolean = this != null
