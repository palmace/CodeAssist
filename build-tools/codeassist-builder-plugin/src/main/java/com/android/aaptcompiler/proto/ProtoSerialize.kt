package com.android.aaptcompiler.proto

import android.aapt.pb.internal.ResourcesInternal
import com.android.aapt.ConfigurationOuterClass
import com.android.aapt.Resources
import com.android.aaptcompiler.*
import com.android.aaptcompiler.android.ResTableConfig
import com.android.aaptcompiler.android.ResValue
import com.android.aaptcompiler.buffer.BigBuffer
import com.android.resources.ResourceVisibility
import com.tyron.builder.log.ILogger
import com.google.protobuf.ByteString

internal fun serializePoolToPb(pool: StringPool, logger: ILogger?): Resources.StringPool {
    val buffer = BigBuffer(1024)
    try {
        pool.flattenUtf8(buffer, logger)
    } catch (e: Exception) {
        throw ResourceCompilationException("Failed to flatten utf-8 string.", e)
    }

    val builder = Resources.StringPool.newBuilder()
    builder.setData(ByteString.copyFrom(buffer.toBytes()))

    return builder.build()
}

internal fun serializeSourceToPb(source: Source, sourcePool: StringPool): Resources.Source {

    val ref = sourcePool.makeRef(source.path)
    val sourceBuilder = Resources.Source.newBuilder()

    sourceBuilder.setPathIdx(ref.index)
    if (source.line != -1) {
        sourceBuilder.setPosition(
            Resources.SourcePosition.newBuilder().setLineNumber(source.line).build())
    }
    return sourceBuilder.build()
}

fun serializeTableToPb(table: ResourceTable, logger: ILogger? = null): Resources.ResourceTable {
    val tableBuilder = Resources.ResourceTable.newBuilder()
    val sourcePool = StringPool()


    tableBuilder.addToolFingerprint(Resources.ToolFingerprint.newBuilder()
        .setTool(ToolFingerprint.TOOL_NAME)
        .setVersion(ToolFingerprint.FINGERPRINT)
        .build())
    val overlayables = mutableListOf<Overlayable>()
    for (resourcePackage in ResourceTableAcessor.getPackages(table)) {
        val packageBuilder = Resources.Package.newBuilder()

        val packageId = resourcePackage.id
        if (packageId != null) {
            packageBuilder.setPackageId(
                Resources.PackageId.newBuilder()
                    .setId(packageId.toInt())
                    .build())
        }
        packageBuilder.setPackageName(resourcePackage.name)

        for (resourceGroup in resourcePackage.groups) {
            val groupBuilder = Resources.Type.newBuilder()

            val groupId = resourceGroup.id
            if (groupId != null) {
                groupBuilder.setTypeId(
                    Resources.TypeId.newBuilder()
                        .setId(groupId.toInt())
                        .build())
            }

            groupBuilder.setName(resourceGroup.type.tagName)

            for (idToEntries in resourceGroup.entries.values) {
                for (idToEntry in idToEntries.values) {
                    val entryBuilder = Resources.Entry.newBuilder()

                    val entryId = idToEntry.id
                    if (entryId != null) {
                        entryBuilder.setEntryId(
                            Resources.EntryId.newBuilder()
                                .setId(entryId.toInt())
                                .build())
                    }
                    entryBuilder.setName(idToEntry.name)

                    val visibilityBuilder = Resources.Visibility.newBuilder()
                    if (idToEntry.visibility.source.path.isNotEmpty()) {
                        visibilityBuilder.setSource(serializeSourceToPb(idToEntry.visibility.source, sourcePool))
                    }
                    entryBuilder.setVisibility(
                        visibilityBuilder
                            .setLevel(serializeVisibilityToPb(idToEntry.visibility.level))
                            .setComment(idToEntry.visibility.comment)
                            .build())

                    val entryAllowNew = idToEntry.allowNew
                    if (entryAllowNew != null) {
                        val allowNewBuilder = Resources.AllowNew.newBuilder()
                        if (entryAllowNew.source.path.isNotEmpty()) {
                            allowNewBuilder.setSource(serializeSourceToPb(entryAllowNew.source, sourcePool))
                        }
                        entryBuilder.setAllowNew(
                            allowNewBuilder
                                .setComment(entryAllowNew.comment)
                                .build())
                    }

                    val entryOverlayableItem = idToEntry.overlayable
                    if (entryOverlayableItem != null) {

                        entryBuilder.setOverlayableItem(
                            serializeOverlayableToPb(
                                entryOverlayableItem,
                                overlayables,
                                tableBuilder,
                                sourcePool))
                    }

                    for (configValue in idToEntry.values) {
                        entryBuilder.addConfigValue(
                            Resources.ConfigValue.newBuilder()
                                .setConfig(serializeConfigToPb(configValue.config, configValue.product, logger))
                                .setValue(serializeValueToPb(configValue.value!!, sourcePool, logger))
                                .build()
                        )
                    }
                    groupBuilder.addEntry(entryBuilder.build())
                }
            }
            packageBuilder.addType(groupBuilder.build())
        }
        tableBuilder.addPackage(packageBuilder.build())
    }
    return tableBuilder.setSourcePool(serializePoolToPb(sourcePool, logger))
        .build()
}

fun serializeVisibilityToPb(resourceVisibility: ResourceVisibility) =
    when(resourceVisibility) {
        ResourceVisibility.PRIVATE,
        ResourceVisibility.PRIVATE_XML_ONLY -> Resources.Visibility.Level.PRIVATE
        ResourceVisibility.PUBLIC -> Resources.Visibility.Level.PUBLIC
        else -> Resources.Visibility.Level.UNKNOWN
    }

fun serializeCompiledFileToPb(file: ResourceFile): ResourcesInternal.CompiledFile {
    val compiledFile = ResourcesInternal.CompiledFile.newBuilder()
        .setResourceName(file.name.toString())
        .setSourcePath(file.source.path)
        .setType(serializeFileTypeToPb(file.type))
        .setConfig(serializeConfigToPb(file.configuration, null, null as ILogger?))

    for (exportedResourceName in file.exportedSymbols) {
        compiledFile.addExportedSymbol(
            ResourcesInternal.CompiledFile.Symbol.newBuilder()
                .setResourceName(exportedResourceName.name.toString())
                .setSource(
                    Resources.SourcePosition.newBuilder()
                        .setLineNumber(exportedResourceName.line)))
    }
    return compiledFile.build()
}

fun serializeConfigToPb(
    config: ConfigDescription,
    product: String?,
    @Suppress("UNUSED_PARAMETER") logger: ILogger?): ConfigurationOuterClass.Configuration {

    val configBuilder = ConfigurationOuterClass.Configuration.newBuilder()
    configBuilder.setMcc(config.mcc.toInt() and 0xffff)
    configBuilder.setMnc(config.mnc.toInt() and 0xffff)
    configBuilder.setLocale(config.getBcp47Locale())

    configBuilder.setLayoutDirection(
        when (config.screenLayout.toInt() and ResTableConfig.SCREEN_LAYOUT.DIR_MASK.toInt()) {
            ResTableConfig.SCREEN_LAYOUT.DIR_LTR.toInt() ->
                ConfigurationOuterClass.Configuration.LayoutDirection.LAYOUT_DIRECTION_LTR
            ResTableConfig.SCREEN_LAYOUT.DIR_RTL.toInt() ->
                ConfigurationOuterClass.Configuration.LayoutDirection.LAYOUT_DIRECTION_RTL
            else -> ConfigurationOuterClass.Configuration.LayoutDirection.LAYOUT_DIRECTION_UNSET
        })

    configBuilder.setScreenWidth(config.screenWidth)
    configBuilder.setScreenHeight(config.screenHeight)
    configBuilder.setScreenWidthDp(config.screenWidthDp)
    configBuilder.setScreenHeightDp(config.screenHeightDp)
    configBuilder.setSmallestScreenWidthDp(config.smallestScreenWidthDp)

    configBuilder.setScreenLayoutSize(
        when (config.screenLayout.toInt() and ResTableConfig.SCREEN_LAYOUT.SIZE_MASK.toInt()) {
            ResTableConfig.SCREEN_LAYOUT.SIZE_SMALL.toInt() ->
                ConfigurationOuterClass.Configuration.ScreenLayoutSize.SCREEN_LAYOUT_SIZE_SMALL
            ResTableConfig.SCREEN_LAYOUT.SIZE_NORMAL.toInt() ->
                ConfigurationOuterClass.Configuration.ScreenLayoutSize.SCREEN_LAYOUT_SIZE_NORMAL
            ResTableConfig.SCREEN_LAYOUT.SIZE_LARGE.toInt() ->
                ConfigurationOuterClass.Configuration.ScreenLayoutSize.SCREEN_LAYOUT_SIZE_LARGE
            ResTableConfig.SCREEN_LAYOUT.SIZE_XLARGE.toInt() ->
                ConfigurationOuterClass.Configuration.ScreenLayoutSize.SCREEN_LAYOUT_SIZE_XLARGE
            else -> ConfigurationOuterClass.Configuration.ScreenLayoutSize.SCREEN_LAYOUT_SIZE_UNSET
        })

    configBuilder.setScreenLayoutLong(
        when (
            config.screenLayout.toInt() and ResTableConfig.SCREEN_LAYOUT.SCREENLONG_MASK.toInt()) {

            ResTableConfig.SCREEN_LAYOUT.SCREENLONG_YES.toInt() ->
                ConfigurationOuterClass.Configuration.ScreenLayoutLong.SCREEN_LAYOUT_LONG_LONG
            ResTableConfig.SCREEN_LAYOUT.SCREENLONG_NO.toInt() ->
                ConfigurationOuterClass.Configuration.ScreenLayoutLong.SCREEN_LAYOUT_LONG_NOTLONG
            else -> ConfigurationOuterClass.Configuration.ScreenLayoutLong.SCREEN_LAYOUT_LONG_UNSET
        })

    configBuilder.setDensity(config.density)

    configBuilder.setSdkVersion(config.sdkVersion.toInt() and 0xffff)

    if (product != null) {
        configBuilder.setProduct(product)
    }

    return configBuilder.build()
}

fun serializeOverlayableToPb(
    item: OverlayableItem,
    overlayables: MutableList<Overlayable>,
    table: Resources.ResourceTable.Builder,
    sourcePool: StringPool): Resources.OverlayableItem {

    // Retrieve the index of the overlayable in the list of groups that have already been serialized.
    val foundIndex = overlayables.indexOf(item.overlayable)

    // Serialize the overlayable if it has not been serialized already.
    if (foundIndex == -1) {
        overlayables.add(item.overlayable)
        val overlayableBuilder = Resources.Overlayable.newBuilder()
            .setName(item.overlayable.name)
            .setActor(item.overlayable.actor)
        if (item.overlayable.source.path.isNotEmpty()) {
            overlayableBuilder.setSource(serializeSourceToPb(item.overlayable.source, sourcePool))
        }
        table.addOverlayable(overlayableBuilder.build())
    }

    val overlayableIndex = if (foundIndex == -1) overlayables.lastIndex else foundIndex

    val itemBuilder = Resources.OverlayableItem.newBuilder()
    itemBuilder.setOverlayableIdx(overlayableIndex)

    if ((item.policies and OverlayableItem.Policy.PUBLIC) != 0) {
        itemBuilder.addPolicy(Resources.OverlayableItem.Policy.PUBLIC)
    }
    if ((item.policies and OverlayableItem.Policy.PRODUCT) != 0) {
        itemBuilder.addPolicy(Resources.OverlayableItem.Policy.PRODUCT)
    }
    if ((item.policies and OverlayableItem.Policy.SYSTEM) != 0) {
        itemBuilder.addPolicy(Resources.OverlayableItem.Policy.SYSTEM)
    }
    if ((item.policies and OverlayableItem.Policy.VENDOR) != 0) {
        itemBuilder.addPolicy(Resources.OverlayableItem.Policy.VENDOR)
    }
    if ((item.policies and OverlayableItem.Policy.SIGNATURE) != 0) {
        itemBuilder.addPolicy(Resources.OverlayableItem.Policy.SIGNATURE)
    }
    if ((item.policies and OverlayableItem.Policy.ODM) != 0) {
        itemBuilder.addPolicy(Resources.OverlayableItem.Policy.ODM)
    }
    if ((item.policies and OverlayableItem.Policy.OEM) != 0) {
        itemBuilder.addPolicy(Resources.OverlayableItem.Policy.OEM)
    }

    if (item.source.path.isNotEmpty()) {
        itemBuilder.setSource(serializeSourceToPb(item.source, sourcePool))
    }
    itemBuilder.setComment(item.comment)

    return itemBuilder.build()
}

fun serializeReferenceTypeToPb(type: Reference.Type) = when (type) {
    Reference.Type.RESOURCE ->
        Resources.Reference.Type.REFERENCE
    Reference.Type.ATTRIBUTE ->
        Resources.Reference.Type.ATTRIBUTE
}

fun serializeReferenceToPb(ref: Reference): Resources.Reference {
    val refBuilder = Resources.Reference.newBuilder()
    refBuilder.setId(ref.id ?: 0)

    if (ref.name != ResourceName.EMPTY) {
        refBuilder.setName(ref.name.toString())
    }

    refBuilder.setPrivate(ref.isPrivate)
    refBuilder.setType(serializeReferenceTypeToPb(ref.referenceType))
    refBuilder.setAllowRaw(ref.allowRaw)
    refBuilder.setTypeFlags(ref.typeFlags)

    return refBuilder.build()
}

fun serializePluralTypeToPb(type: Plural.Type) = when (type) {
    Plural.Type.ZERO -> Resources.Plural.Arity.ZERO
    Plural.Type.ONE -> Resources.Plural.Arity.ONE
    Plural.Type.TWO -> Resources.Plural.Arity.TWO
    Plural.Type.FEW -> Resources.Plural.Arity.FEW
    Plural.Type.MANY -> Resources.Plural.Arity.MANY
    else -> Resources.Plural.Arity.OTHER
}

fun serializeFileTypeToPb(type: FileReference.Type) = when (type) {
    FileReference.Type.BinaryXml -> Resources.FileReference.Type.BINARY_XML
    FileReference.Type.ProtoXml -> Resources.FileReference.Type.PROTO_XML
    FileReference.Type.Png -> Resources.FileReference.Type.PNG
    else -> Resources.FileReference.Type.UNKNOWN
}

fun serializeFileTypeToPb(type: ResourceFile.Type) = when (type) {
    ResourceFile.Type.BinaryXml -> Resources.FileReference.Type.BINARY_XML
    ResourceFile.Type.ProtoXml -> Resources.FileReference.Type.PROTO_XML
    ResourceFile.Type.Png -> Resources.FileReference.Type.PNG
    else -> Resources.FileReference.Type.UNKNOWN
}

fun serializeStringToPb(string: BasicString): Resources.String =
    Resources.String.newBuilder().setValue(string.ref.value()).build()

fun serializeRawToPb(string: RawString): Resources.RawString =
    Resources.RawString.newBuilder().setValue(string.ref.value()).build()

fun serializeStyledStrToPb(string: StyledString): Resources.StyledString {
    val styleBuilder = Resources.StyledString.newBuilder()
    styleBuilder.setValue(string.ref.value())
    for (span in string.ref.spans()) {
        val spanBuilder = Resources.StyledString.Span.newBuilder()
        spanBuilder.setTag(span.name)
            .setFirstChar(span.firstChar)
            .setLastChar(span.lastChar)
        styleBuilder.addSpan(spanBuilder.build())
    }
    return styleBuilder.build()
}

fun serializeFileRefToPb(file: FileReference): Resources.FileReference =
    Resources.FileReference.newBuilder()
        .setPath(file.path.value())
        .setType(serializeFileTypeToPb(file.type))
        .build()

fun serializeBinPrimitiveToPb(primitive: BinaryPrimitive, logger: ILogger?): Resources.Primitive {
    val resVal = primitive.flatten()

    val primitiveBuilder = Resources.Primitive.newBuilder()

    when (resVal.dataType) {
        ResValue.DataType.NULL -> {
            when (resVal.data) {
                ResValue.NullFormat.UNDEFINED ->
                    primitiveBuilder.setNullValue(Resources.Primitive.NullType.newBuilder().build())
                ResValue.NullFormat.EMPTY ->
                    primitiveBuilder.setEmptyValue(Resources.Primitive.EmptyType.newBuilder().build())
                else -> {
                    val errorMsg = "%s, Invalid null format value '%s' for primitive %s."
                    logger?.error(String.format(errorMsg, blameSource(primitive.source), resVal.data, primitive))
                }
            }
        }
        ResValue.DataType.FLOAT -> primitiveBuilder.setFloatValue(Float.fromBits(resVal.data))
        ResValue.DataType.DIMENSION -> primitiveBuilder.setDimensionValue(resVal.data)
        ResValue.DataType.FRACTION -> primitiveBuilder.setFractionValue(resVal.data)
        ResValue.DataType.INT_DEC -> primitiveBuilder.setIntDecimalValue(resVal.data)
        ResValue.DataType.INT_HEX -> primitiveBuilder.setIntHexadecimalValue(resVal.data)
        ResValue.DataType.INT_BOOLEAN -> primitiveBuilder.setBooleanValue(resVal.data != 0)
        ResValue.DataType.INT_COLOR_ARGB8 -> primitiveBuilder.setColorArgb8Value(resVal.data)
        ResValue.DataType.INT_COLOR_RGB8 -> primitiveBuilder.setColorRgb8Value(resVal.data)
        ResValue.DataType.INT_COLOR_ARGB4 -> primitiveBuilder.setColorArgb4Value(resVal.data)
        ResValue.DataType.INT_COLOR_RGB4 -> primitiveBuilder.setColorRgb4Value(resVal.data)
        else -> {
            val errorMsg = "%s, Invalid data type '%s' for primitive %s."
            logger?.error(String.format(errorMsg, blameSource(primitive.source), resVal.dataType, primitive))
        }
    }

    return primitiveBuilder.build()
}

fun serializeAttrToPb(attribute: AttributeResource, sourcePool: StringPool): Resources.Attribute {
    val attrBuilder = Resources.Attribute.newBuilder()
        .setFormatFlags(attribute.typeMask)
        .setMinInt(attribute.minInt)
        .setMaxInt(attribute.maxInt)

    for (symbol in attribute.symbols) {
        val symbolBuilder = Resources.Attribute.Symbol.newBuilder()
        if (symbol.symbol.source.path.isNotEmpty()) {
            symbolBuilder.setSource(serializeSourceToPb(symbol.symbol.source, sourcePool))
        }
        attrBuilder.addSymbol(
            symbolBuilder
                .setComment(symbol.symbol.comment)
                .setName(serializeReferenceToPb(symbol.symbol))
                .setValue(symbol.value)
                .setType(symbol.type.toInt())
                .build())
    }

    return attrBuilder.build()
}

fun serializeStyleToPb(style: Style, sourcePool: StringPool, logger: ILogger?): Resources.Style {
    val styleBuilder = Resources.Style.newBuilder()

    val parent = style.parent
    if (parent != null) {
        styleBuilder.setParent(serializeReferenceToPb(parent))
        if (parent.source.path.isNotEmpty()) {
            styleBuilder.setParentSource(serializeSourceToPb(parent.source, sourcePool))
        }
    }

    for (entry in style.entries) {
        val entryBuilder = Resources.Style.Entry.newBuilder()
        if (entry.key.source.path.isNotEmpty()) {
            entryBuilder.setSource(serializeSourceToPb(entry.key.source, sourcePool))
        }
        styleBuilder.addEntry(
            entryBuilder
                .setKey(serializeReferenceToPb(entry.key))
                .setComment(entry.key.comment)
                .setItem(serializeItemToPb(entry.value, logger))
                .build())
    }
    return styleBuilder.build()
}

fun serializeStyleableToPb(styleable: Styleable, sourcePool: StringPool): Resources.Styleable {

    val styleableBuilder = Resources.Styleable.newBuilder()
    for (entry in styleable.entries) {
        val entryBuilder = Resources.Styleable.Entry.newBuilder()
        if (entry.source.path.isNotEmpty()) {
            entryBuilder.setSource(serializeSourceToPb(entry.source, sourcePool))
        }
        styleableBuilder.addEntry(
            entryBuilder
                .setComment(entry.comment)
                .setAttr(serializeReferenceToPb(entry))
                .build())
    }
    return styleableBuilder.build()
}

fun serializeArrayToPb(
    array: ArrayResource, sourcePool: StringPool, logger: ILogger?): Resources.Array {

    val arrayBuilder = Resources.Array.newBuilder()
    for (element in array.elements) {
        val entryBuilder = Resources.Array.Element.newBuilder()
        if (element.source.path.isNotEmpty()) {
            entryBuilder.setSource(serializeSourceToPb(element.source, sourcePool))
        }
        arrayBuilder.addElement(
            entryBuilder
                .setComment(element.comment)
                .setItem(serializeItemToPb(element, logger))
                .build())
    }
    return arrayBuilder.build()
}

fun serializePluralToPb(
    plural: Plural, sourcePool: StringPool, logger: ILogger?): Resources.Plural {

    val pluralBuilder = Resources.Plural.newBuilder()
    for (type in Plural.Type.TYPES) {
        val entry = plural.values[type.ordinal]
        entry ?: continue
        val entryBuilder = Resources.Plural.Entry.newBuilder()
        if (entry.source.path.isNotEmpty()) {
            entryBuilder.setSource(serializeSourceToPb(entry.source, sourcePool))
        }
        pluralBuilder.addEntry(
            entryBuilder
                .setComment(entry.comment)
                .setArity(serializePluralTypeToPb(type))
                .setItem(serializeItemToPb(entry, logger))
                .build())
    }
    return pluralBuilder.build()
}

fun serializeMacroToPb(macro: Macro): Resources.MacroBody {

    val macroBuilder = Resources.MacroBody.newBuilder()
    macroBuilder.setRawString(macro.rawValue)

    val styleString = Resources.StyleString.newBuilder()
    styleString.setStr(macro.styleString.str)
    for (span in macro.styleString.spans) {
        val spansBuilder = Resources.StyleString.Span.newBuilder()
        spansBuilder.setName(span.name)
            .setStartIndex(span.firstChar)
            .setEndIndex(span.lastChar)
        styleString.addSpans(spansBuilder.build())
    }
    macroBuilder.setStyleString(styleString.build())

    for (untranslatables in macro.untranslatables) {
        val section = Resources.UntranslatableSection.newBuilder()
        section.setStartIndex(untranslatables.startIndex.toLong())
        section.setEndIndex(untranslatables.endIndex.toLong())
        macroBuilder.addUntranslatableSections(section.build())
    }

    for (aliasNamespace in macro.aliasNamespaces) {
        val namespace = Resources.NamespaceAlias.newBuilder()
        namespace.setPrefix(aliasNamespace.prefix)
            .setPackageName(aliasNamespace.packageName)
            .setIsPrivate(aliasNamespace.isPrivate)
        macroBuilder.addNamespaceStack(namespace.build())
    }

    return macroBuilder.build()
}

fun serializeItemToPb(item: Item, logger: ILogger?): Resources.Item {
    val itemBuilder = Resources.Item.newBuilder()
    when (item) {
        is Reference -> itemBuilder.setRef(serializeReferenceToPb(item))
        is BasicString -> itemBuilder.setStr(serializeStringToPb(item))
        is RawString -> itemBuilder.setRawStr(serializeRawToPb(item))
        is StyledString -> itemBuilder.setStyledStr(serializeStyledStrToPb(item))
        is FileReference -> itemBuilder.setFile(serializeFileRefToPb(item))
        is Id -> itemBuilder.setId(Resources.Id.newBuilder().build())
        is BinaryPrimitive -> itemBuilder.setPrim(serializeBinPrimitiveToPb(item, logger))
        else -> {
            val errorMsg = "%s, Unrecognized item type %s, for value %s."
            logger?.error(String.format(errorMsg, blameSource(item.source), item.javaClass, item))
        }
    }
    return itemBuilder.build()
}

fun serializeValueToPb(value: Value, sourcePool: StringPool, logger: ILogger?): Resources.Value {
    val valueBuilder = Resources.Value.newBuilder()

    if (value is Item) {
        valueBuilder.setItem(serializeItemToPb(value, logger))
    } else {
        val compoundBuilder = Resources.CompoundValue.newBuilder()
        when (value) {
            is AttributeResource -> compoundBuilder.setAttr(serializeAttrToPb(value, sourcePool))
            is Style -> compoundBuilder.setStyle(serializeStyleToPb(value, sourcePool, logger))
            is Styleable -> compoundBuilder.setStyleable(serializeStyleableToPb(value, sourcePool))
            is ArrayResource -> compoundBuilder.setArray(serializeArrayToPb(value, sourcePool, logger))
            is Plural -> compoundBuilder.setPlural(serializePluralToPb(value, sourcePool, logger))
            is Macro -> compoundBuilder.setMacro(serializeMacroToPb(value))
            else -> {
                val errorMsg = "%s, Unrecognized value type %s, for value %s."
                logger?.error(String.format(errorMsg, blameSource(value.source), value.javaClass, value))
            }
        }
        valueBuilder.setCompoundValue(compoundBuilder.build())
    }
    if (value.source.path.isNotEmpty()){
        valueBuilder.setSource(serializeSourceToPb(value.source, sourcePool))
    }
    return valueBuilder
        .setComment(value.comment)
        .setWeak(value.weak)
        .build()
}
