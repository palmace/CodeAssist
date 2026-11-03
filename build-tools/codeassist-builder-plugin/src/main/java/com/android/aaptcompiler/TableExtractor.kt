package com.android.aaptcompiler

import com.android.aapt.Resources
import com.android.aaptcompiler.android.stringToInt
import com.android.aaptcompiler.android.parseAsBool
import com.android.resources.ResourceVisibility
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.Comment
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent
import java.io.InputStream
import java.util.*

/**
 * Namespace uri for the xliff:g tag in XML.
 *
 * This is used to identify the xliff:g spans in processed strings, in order to mark the
 * untranslatable sections of string resources.
 */
private const val XLIFF_NS_URI = "urn:oasis:names:tc:xliff:document:1.2"

/**
 * Resource parsed from the XML, with all relevant metadata.
 */
private class ParsedResource(
    var config: ConfigDescription, val source: Source, val comment: String) {

    constructor() : this(ConfigDescription(), Source(""), "")

    /** The name of the resource extracted from the xml. */
    var name: ResourceName = ResourceName.EMPTY
    /** The product name for the given resource value. */
    var productString: String = ""
    /** The id of the resource. A value of 0 means the id was not supplied. */
    var resourceId = 0
    /** The visibility of the extracted resource. */
    var visibility = ResourceVisibility.UNDEFINED
    /** Whether the resource has <add-resource> in an overlay. */
    var allowNew = false
    /**
     * The overlayable representation of this resource.
     */
    var overlayableItem: OverlayableItem? = null
    /**
     * The value of the resource.
     */
    var value : Value? = null
    /**
     * The child resources of the given resource.
     */
    val children = mutableListOf<ParsedResource>()
}

/**
 * All options for the Table Extractor.
 */
data class TableExtractorOptions(
    val translatable: Boolean = true,
    val errorOnPositionalArgs: Boolean = true,
    val visibility: ResourceVisibility? = null)

/**
 * Class that parses an XML file for resources and adds them to a ResourceTable.
 */
class TableExtractor(
    val table: ResourceTable,
    val source: Source,
    val config: ConfigDescription,
    val options: TableExtractorOptions,
    val logger: BlameLogger) {

    fun extract(inputFile: InputStream) {
        var eventReader : XMLEventReader? = null
        try {
            eventReader = xmlInputFactory.createXMLEventReader(inputFile)

            val documentStart = eventReader.nextEvent()
            if (!documentStart.isStartDocument) {
                val userReadableSource = logger.getOriginalSource(blameSource(source)).toString().trim()
                error("Failed to find start of XML $userReadableSource")
            }

            var rootStart: XMLEvent? = null
            while(eventReader.hasNext()) {
                rootStart = eventReader.nextEvent()
                if (rootStart.isStartElement) {
                    break
                }
            }
            rootStart ?: return

            val rootName = rootStart.asStartElement().name
            if (rootName.namespaceURI != null && rootName.namespaceURI.isNotEmpty() && rootName.localPart != "resources") {
                val userReadableSource = logger.getOriginalSource(blameSource(source)).toString().trim()
                error(
                    "Root xml element of resource table not labeled 'resources' ($userReadableSource)."
                )
            }
            extractResourceValues(eventReader)
        } catch (xmlException: XMLStreamException) {
            if (xmlException.message?.contains("Premature end of file.", true) != true) {
                throw xmlException
            }
        } finally {
            eventReader?.close()
        }
    }

    private fun logError(source: Source, message: String) {
        logger.error(message, source)
    }

    private fun extractResourceValues(eventReader: XMLEventReader) {
        val errors = mutableListOf<String>()
        var comment = ""

        while (eventReader.hasNext()) {
            val event = eventReader.nextEvent()

            if (event.eventType == XMLStreamConstants.COMMENT) {
                comment = (event as Comment).text.trim()
                continue
            }

            if (event.isCharacters) {
                if (!event.asCharacters().isWhiteSpace) {
                    errors += "Plain text is not allowed at ${blameSource(source, event.location)}."
                }
                continue
            }

            if (event.isEndElement) {
                break
            }

            if (!event.isStartElement) {
                errors +=
                    "Unexpected element type: ${event.eventType}, ${blameSource(source, event.location)}."
                continue
            }

            val element = event.asStartElement()
            val elementName = element.name
            if (elementName.namespaceURI != null && elementName.namespaceURI.isNotEmpty()) {
                walkToEndOfElement(element, eventReader)
                continue
            }

            if (elementName.localPart == "skip" || elementName.localPart == "eat-comment") {
                comment = ""
                walkToEndOfElement(element, eventReader)
                continue
            }

            val parsedResource =
                ParsedResource(config, source.withLine(element.location.lineNumber), comment)
            comment = ""

            val productName = element.getAttributeByName(QName("product"))
            if (productName != null) {
                parsedResource.productString = productName.value
            }

            if (!extractResource(element, eventReader, parsedResource)) {
                errors += "Can not extract resource from ${elementName.localPart} at ${parsedResource.source}."
            } else {
                if (!addResourceToTable(parsedResource)) {
                    errors += "Can not add resource ${parsedResource.name} to table."
                }
            }
        }
        if (errors.any()) {
            error(errors.joinToString(separator = "\n"))
        }
    }

    private fun extractResource(
        element : StartElement,
        eventReader : XMLEventReader,
        parsedResource : ParsedResource): Boolean {

        var resourceTypeName = element.name.localPart
        var resourceFormat = 0
        var canBeItem = true
        var canBeBag = true

        if (resourceTypeName == "item") {
            canBeBag = false
            resourceFormat = Resources.Attribute.FormatFlags.ANY_VALUE

            val formatAttribute = element.getAttributeByName(QName("format"))
            if (formatAttribute != null) {
                resourceFormat = parseFormatNoEnumsOrFlags(formatAttribute.value)
                if (resourceFormat == 0) {
                    logError(parsedResource.source, "Resource has an invalid format of ${formatAttribute.value}.")
                    walkToEndOfElement(element, eventReader)
                    return false
                }
            }

            val typeAttribute = element.getAttributeByName(QName("type"))
            if (typeAttribute == null) {
                logError(parsedResource.source, "<item> must have a 'type' attribute")
                walkToEndOfElement(element, eventReader)
                return false
            }
            resourceTypeName = typeAttribute.value

        } else if (resourceTypeName == "bag") {
            canBeItem = false
            val typeAttribute = element.getAttributeByName(QName("type"))
            if (typeAttribute == null) {
                logError(parsedResource.source, "<bag> must have a 'type' attribute")
                walkToEndOfElement(element, eventReader)
                return false
            }
            resourceTypeName = typeAttribute.value
        }

        val nameAttribute = element.getAttributeByName(QName("name"))

        if (resourceTypeName == "id") {
            if (nameAttribute == null) {
                logError(parsedResource.source, "<${element.name}> is missing the 'name' attribute.")
                walkToEndOfElement(element, eventReader)
                return false
            }

            parsedResource.name = ResourceName("", AaptResourceType.ID, nameAttribute.value)
            parseItem(element, eventReader, parsedResource, resourceFormat)

            val item = parsedResource.value
            when {
                item is BasicString && item.ref.value().isEmpty() -> parsedResource.value = Id()
                item is Reference && item.id == null -> parsedResource.value = Id()
                (item is Reference && item.name.type != AaptResourceType.ID) || (item != null && item !is Reference && item !is BasicString) -> {
                    // This happens if the user specifically creates an id resource, but gives it a value that isn't a reference.
                    // This is still fine as it allows the user to preallocate IDs for use elsewhere.
                    parsedResource.value = Id()
                }
            }
            return true
        }

        if (canBeItem) {
            val (type, typeMask) = when (resourceTypeName) {
                "bool" -> Pair(AaptResourceType.BOOL, Resources.Attribute.FormatFlags.BOOLEAN_VALUE)
                "color" -> Pair(AaptResourceType.COLOR, Resources.Attribute.FormatFlags.COLOR_VALUE)
                "configVarying" -> Pair(AaptResourceType.CONFIG_VARYING, Resources.Attribute.FormatFlags.ANY_VALUE)
                "dimen" -> Pair(AaptResourceType.DIMEN, Resources.Attribute.FormatFlags.FLOAT_VALUE or Resources.Attribute.FormatFlags.FRACTION_VALUE or Resources.Attribute.FormatFlags.DIMENSION_VALUE)
                "drawable" -> Pair(AaptResourceType.DRAWABLE, Resources.Attribute.FormatFlags.COLOR_VALUE)
                "fraction" -> Pair(AaptResourceType.FRACTION, Resources.Attribute.FormatFlags.FLOAT_VALUE or Resources.Attribute.FormatFlags.FRACTION_VALUE or Resources.Attribute.FormatFlags.DIMENSION_VALUE)
                "integer" -> Pair(AaptResourceType.INTEGER, Resources.Attribute.FormatFlags.INTEGER_VALUE)
                "string" -> Pair(AaptResourceType.STRING, Resources.Attribute.FormatFlags.STRING_VALUE)
                else -> Pair(null, Resources.Attribute.FormatFlags.ANY_VALUE)
            }
            if (type != null) {
                if (nameAttribute == null) {
                    logError(parsedResource.source, "<${element.name}> is missing the 'name' attribute.")
                    walkToEndOfElement(element, eventReader)
                    return false
                }
                parsedResource.name = ResourceName( "", type, nameAttribute.value)
                if (resourceFormat == 0) {
                    resourceFormat = typeMask
                }
                return parseItem(element, eventReader, parsedResource, resourceFormat)
            }
        }

        if (canBeBag) {
            val parseBagMethod = when(resourceTypeName) {
                "array" -> ::parseArray
                "attr" -> ::parseAttr
                "style" -> ::parseStyle
                "plurals" -> ::parsePlural
                "string-array" -> ::parseStringArray
                "integer-array" -> ::parseIntegerArray
                "declare-styleable" -> ::parseDeclareStyleable
                else -> null
            }

            if (parseBagMethod != null) {
                if (nameAttribute == null) {
                    walkToEndOfElement(element, eventReader)
                    logError(parsedResource.source, "<${element.name}> is missing the 'name' attribute.")
                    return false
                }
                parsedResource.name = ResourceName("", resourceTypeFromTag(resourceTypeName) ?: AaptResourceType.RAW, nameAttribute.value)
                return parseBagMethod(element, eventReader, parsedResource)
            }
        }

        walkToEndOfElement(element, eventReader)
        return false
    }

    private fun parseItem(
        element: StartElement,
        eventReader: XMLEventReader,
        parsedResource: ParsedResource,
        resourceFormat: Int) : Boolean {
        parsedResource.value = parseXml(element, eventReader, resourceFormat, false)
        return parsedResource.value != null
    }

    private fun parseXml(
        element: StartElement,
        eventReader: XMLEventReader,
        resourceFormat: Int,
        allowRawString : Boolean) : Item? {
        val xmlStringBuilder = XmlStringBuilder()
        val tagStack = Stack<String>()
        
        while (eventReader.hasNext()) {
            val event = eventReader.nextEvent()
            if (event.isCharacters) {
                xmlStringBuilder.append(event.asCharacters().data)
            } else if (event.isStartElement) {
                val subElement = event.asStartElement()
                val subName = subElement.name.localPart
                if (subElement.name.namespaceURI == XLIFF_NS_URI && subName == "g") {
                    xmlStringBuilder.startUntranslatable()
                    tagStack.push("xliff:g")
                } else {
                    xmlStringBuilder.startSpan(subName)
                    tagStack.push(subName)
                }
            } else if (event.isEndElement) {
                val endElement = event.asEndElement()
                if (endElement.name == element.name) break
                
                val lastTag = if (tagStack.isNotEmpty()) tagStack.pop() else ""
                if (lastTag == "xliff:g") {
                    xmlStringBuilder.endUntranslatable()
                } else {
                    xmlStringBuilder.endSpan()
                }
            }
        }
        val flattened = xmlStringBuilder.getFlattenedXml()
        if (!flattened.success) return null
        
        val text = flattened.styleString.str
        if (resourceFormat and Resources.Attribute.FormatFlags.REFERENCE_VALUE != 0) {
            val parsedRef = parseReference(text)
            if (parsedRef != null) return parsedRef.reference
        }
        
        return if (flattened.styleString.spans.isEmpty()) {
            BasicString(table.stringPool.makeRef(flattened.styleString.str), flattened.untranslatableSections)
        } else {
            StyledString(table.stringPool.makeRef(flattened.styleString), flattened.untranslatableSections)
        }
    }
    
    private fun parseArray(e: StartElement, r: XMLEventReader, p: ParsedResource): Boolean {
        val array = ArrayResource()
        p.value = array
        while (r.hasNext()) {
            val event = r.nextEvent()
            if (event.isStartElement) {
                val item = parseXml(event.asStartElement(), r, Resources.Attribute.FormatFlags.ANY_VALUE, true)
                if (item != null) array.elements.add(item)
            } else if (event.isEndElement) break
        }
        return true
    }

    private fun parseAttr(e: StartElement, r: XMLEventReader, p: ParsedResource): Boolean {
        val formatAttr = e.getAttributeByName(QName("format"))
        val mask = if (formatAttr != null) parseFormatAttribute(formatAttr.value) else Resources.Attribute.FormatFlags.ANY_VALUE
        val attr = AttributeResource(mask)
        p.value = attr
        
        while (r.hasNext()) {
            val event = r.nextEvent()
            if (event.isStartElement) {
                val subElement = event.asStartElement()
                val subName = subElement.name.localPart
                if (subName == "enum" || subName == "flag") {
                    val name = subElement.getAttributeByName(QName("name"))?.value ?: ""
                    val valueStr = subElement.getAttributeByName(QName("value"))?.value ?: ""
                    val value = stringToInt(valueStr)?.data ?: 0
                    attr.symbols.add(AttributeResource.Symbol(Reference(ResourceName("", AaptResourceType.ID, name)), value))
                    walkToEndOfElement(subElement, r)
                }
            } else if (event.isEndElement) break
        }
        return true
    }

    private fun parseStyle(e: StartElement, r: XMLEventReader, p: ParsedResource): Boolean {
        val style = Style()
        p.value = style
        val parentAttr = e.getAttributeByName(QName("parent"))
        if (parentAttr != null) {
            val parentRef = parseReference(parentAttr.value)
            if (parentRef != null) {
                style.parent = parentRef.reference
            } else {
                style.parent = Reference(ResourceName("", AaptResourceType.STYLE, parentAttr.value))
            }
        }
        while (r.hasNext()) {
            val event = r.nextEvent()
            if (event.isStartElement) {
                val item = event.asStartElement()
                val name = item.getAttributeByName(QName("name"))?.value ?: ""
                val value = parseXml(item, r, Resources.Attribute.FormatFlags.ANY_VALUE, false)
                if (value != null) {
                    style.entries.add(Style.Entry(Reference(ResourceName("", AaptResourceType.ATTR, name)), value))
                }
            } else if (event.isEndElement) break
        }
        return true
    }

    private fun parsePlural(e: StartElement, r: XMLEventReader, p: ParsedResource): Boolean {
        val plural = Plural()
        p.value = plural
        while (r.hasNext()) {
            val event = r.nextEvent()
            if (event.isStartElement) {
                val item = event.asStartElement()
                val quantity = item.getAttributeByName(QName("quantity"))?.value ?: ""
                val type = when (quantity) {
                    "zero" -> Plural.Type.ZERO
                    "one" -> Plural.Type.ONE
                    "two" -> Plural.Type.TWO
                    "few" -> Plural.Type.FEW
                    "many" -> Plural.Type.MANY
                    else -> Plural.Type.OTHER
                }
                val value = parseXml(item, r, Resources.Attribute.FormatFlags.STRING_VALUE, false)
                if (value != null) plural.setValue(type, value)
            } else if (event.isEndElement) break
        }
        return true
    }

    private fun parseStringArray(e: StartElement, r: XMLEventReader, p: ParsedResource) = parseArray(e, r, p)
    private fun parseIntegerArray(e: StartElement, r: XMLEventReader, p: ParsedResource) = parseArray(e, r, p)
    
    private fun parseDeclareStyleable(e: StartElement, r: XMLEventReader, p: ParsedResource): Boolean {
        val styleable = Styleable()
        p.value = styleable
        while (r.hasNext()) {
            val event = r.nextEvent()
            if (event.isStartElement) {
                val attrElement = event.asStartElement()
                val name = attrElement.getAttributeByName(QName("name"))?.value ?: ""
                styleable.entries.add(Reference(ResourceName("", AaptResourceType.ATTR, name)))
                walkToEndOfElement(attrElement, r)
            } else if (event.isEndElement) break
        }
        return true
    }
    
    private fun addResourceToTable(parsedResource: ParsedResource): Boolean {
        if (parsedResource.value == null) return true
        val res = parsedResource.value!!
        res.source = parsedResource.source
        res.comment = parsedResource.comment
        return table.addResource(parsedResource.name, parsedResource.config, parsedResource.productString, res)
    }

    fun parseFormatNoEnumsOrFlags(name: String): Int =
        when (name) {
            "reference" -> Resources.Attribute.FormatFlags.REFERENCE_VALUE
            "string" -> Resources.Attribute.FormatFlags.STRING_VALUE
            "integer" -> Resources.Attribute.FormatFlags.INTEGER_VALUE
            "boolean" -> Resources.Attribute.FormatFlags.BOOLEAN_VALUE
            "color" -> Resources.Attribute.FormatFlags.COLOR_VALUE
            "float" -> Resources.Attribute.FormatFlags.FLOAT_VALUE
            "dimension" -> Resources.Attribute.FormatFlags.DIMENSION_VALUE
            "fraction" -> Resources.Attribute.FormatFlags.FRACTION_VALUE
            else -> 0
        }

    fun parseFormatType(name: String): Int =
        when (name) {
            "enum" -> Resources.Attribute.FormatFlags.ENUM_VALUE
            "flags" -> Resources.Attribute.FormatFlags.FLAGS_VALUE
            else -> parseFormatNoEnumsOrFlags(name)
        }

    fun parseFormatAttribute(value: String): Int {
        var mask = 0
        for (part in value.split('|')) {
            val type = parseFormatType(part.trim())
            if (type == 0) {
                return 0
            }
            mask = mask or type
        }
        return mask
    }
}

fun Source.withLine(line: Int) = this.copy(line = line)

// Missing from original snippet but used
fun parseXmlAttributeName(name: String): Reference = Reference()
fun parseStyleParentReference(name: String): StyleParentResult = StyleParentResult(null, "")
data class StyleParentResult(val parent: Reference?, val errorString: String)
