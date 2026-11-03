package com.android.aaptcompiler

import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.*
import java.io.InputStream
import javax.xml.namespace.NamespaceContext

private const val AAPT_ATTR_TAG = "attr"

/** Mangles outlined resources so they are only accessible from this file */
private fun mangleEntry(pck: String, entry: String) = "$pck${'$'}$entry"

/**
 * Processes a XML file as a android resource.
 */
class XmlProcessor(val source: Source, val logger: BlameLogger?) {

    lateinit var primaryFile: ResourceFile
        private set

    var xmlResources = listOf<XmlResource>()
        private set

    /**
     * Processes the XML resource.
     */
    fun process(file: ResourceFile, inputFile: InputStream) {
        primaryFile = file

        var eventReader: XMLEventReader? = null
        try {
            eventReader = xmlInputFactory.createXMLEventReader(inputFile)
            val collectedIds = mutableMapOf<ResourceName, SourcedResourceName>()
            val protoBuilders = mutableMapOf<ResourceFile, XmlResourceBuilder>()

            val documentStart = eventReader.nextEvent()
            if (!documentStart.isStartDocument) {
                error("Failed to find start of XML from ${blameSource(source)}")
            }

            var rootStart: XMLEvent? = null
            while (eventReader.hasNext()) {
                rootStart = eventReader.nextEvent()
                if (rootStart.isStartElement) {
                    break
                }
            }
            rootStart ?: return

            val builder = XmlResourceBuilder(primaryFile)
            protoBuilders[file] = builder

            processElement(
                rootStart.asStartElement(),
                eventReader,
                collectedIds,
                protoBuilders,
                builder
            )

            primaryFile.exportedSymbols.addAll(collectedIds.values.toList().sortedBy { it.name })

            xmlResources = protoBuilders.values.map { it.build() }
                .sortedWith { left, right ->
                    when {
                        left === right -> 0
                        left.file.name === primaryFile.name -> -1
                        right.file.name === primaryFile.name -> 1
                        else -> left.file.name.compareTo(right.file.name)
                    }
                }
        } catch (xmlException: XMLStreamException) {
            val message = xmlException.message ?: ""
            if (!message.contains("Premature end of file", true)) {
                throw xmlException
            }
        } finally {
            eventReader?.close()
        }
    }

    private fun processElement(
        startElement: StartElement,
        eventReader: XMLEventReader,
        collectedIds: MutableMap<ResourceName, SourcedResourceName>,
        resourceBuilders: MutableMap<ResourceFile, XmlResourceBuilder>,
        currentBuilder: XmlResourceBuilder,
        inheritedNamespaceContext: NamespaceContext? = null
    ) {
        if (isAaptAttribute(startElement.name)) {
            val exception = RuntimeException(
                "${blameSource(source, startElement.location)}: " +
                        "<aapt:attr> blocks are not allowed as the root of documents, or " +
                        "as a child element under another <aapt:attr>."
            )
            walkToEndOfElement(startElement, eventReader)
            throw exception
        }

        gatherIds(startElement, collectedIds)

        val elementName = startElement.name
        val elementLocation = startElement.location
        currentBuilder.startElement(
            elementName.localPart,
            elementName.namespaceURI ?: "",
            elementLocation.lineNumber,
            elementLocation.columnNumber
        )

        val namespaceDeclarations = startElement.namespaces
        val namespaces = mutableListOf<Namespace>()
        while (namespaceDeclarations.hasNext()) {
            namespaces.add(namespaceDeclarations.next() as Namespace)
        }

        for (namespace in namespaces) {
            currentBuilder.addNamespaceDeclaration(
                namespace.namespaceURI,
                namespace.prefix,
                elementLocation.lineNumber,
                elementLocation.columnNumber
            )
        }

        val attributes = startElement.attributes
        while (attributes.hasNext()) {
            val attribute = attributes.next() as Attribute
            val attrName = attribute.name
            currentBuilder.addAttribute(
                attrName.localPart,
                attrName.namespaceURI,
                attribute.value,
                elementLocation.lineNumber,
                elementLocation.columnNumber
            )
        }

        while (eventReader.hasNext()) {
            val nextEvent = eventReader.nextEvent()

            if (nextEvent.isEndElement) {
                break
            }

            if (nextEvent.isStartElement) {
                val elName = nextEvent.asStartElement().name
                if (isAaptAttribute(elName)) {
                    outlineAttribute(
                      currentBuilder,
                      nextEvent.asStartElement(),
                      eventReader,
                      collectedIds,
                      resourceBuilders
                    )
                } else {
                    processElement(
                      nextEvent.asStartElement(),
                      eventReader,
                      collectedIds,
                      resourceBuilders,
                      currentBuilder
                    )
                }
                continue
            }

            if (nextEvent.isCharacters) {
                processText(nextEvent.asCharacters(), currentBuilder)
            }

            if (nextEvent.eventType == XMLStreamConstants.COMMENT) {
                processComment(currentBuilder)
            }
        }

        currentBuilder.endElement()
    }

    private fun outlineAttribute(
        parentBuilder: XmlResourceBuilder,
        attrElement: StartElement,
        eventReader: XMLEventReader,
        collectedIds: MutableMap<ResourceName, SourcedResourceName>,
        resourceBuilders: MutableMap<ResourceFile, XmlResourceBuilder>
    ) {

        val nameAttribute = attrElement.getAttributeByName(QName("name"))
        if (nameAttribute == null) {
            val exception = RuntimeException(
                "${blameSource(source, attrElement.location)}:" +
                        "<${attrElement.name}> tag requires the 'name' attribute."
            )
            walkToEndOfElement(attrElement, eventReader)
            throw exception
        }

        val reference = parseXmlAttributeName(nameAttribute.value)
        val nameValue = reference.name

        val extractedPackage = transformPackageAlias(attrElement, nameValue.pck!!)
        if (extractedPackage == null) {
            val exception = RuntimeException(
                blameSource(source, attrElement.location).toString() +
                        "Invalid namespace prefix '${nameValue.pck}' " +
                        "for value of 'name' attribute '$nameValue'."
            )
            walkToEndOfElement(attrElement, eventReader)
            throw exception
        }

        val isPrivateNs = extractedPackage.isPrivate || reference.isPrivate

        val attrUri = when {
            nameValue.pck.isNullOrEmpty() -> ""
            extractedPackage.packageName.isEmpty() -> SCHEMA_AUTO
            else -> constructPackageUri(extractedPackage.packageName, isPrivateNs)
        }

        val resource = processAaptAttr(
            attrElement,
            eventReader,
            collectedIds,
            resourceBuilders,
            parentBuilder.namespaceContext
        )

        if (parentBuilder.findAttribute(nameValue.entry!!, attrUri) != null) {
            error("Cannot find attribute ${nameValue.entry}")
        }

        parentBuilder.addAttribute(
            nameValue.entry!!,
            attrUri,
            "@${resource.name}",
            attrElement.location.lineNumber,
            attrElement.location.columnNumber
        )
    }

    private fun isAaptAttribute(elName: QName): Boolean =
        elName.namespaceURI == AAPT_ATTR_URI && elName.localPart == AAPT_ATTR_TAG

    private fun processAaptAttr(
        startElement: StartElement,
        eventReader: XMLEventReader,
        collectedIds: MutableMap<ResourceName, SourcedResourceName>,
        resourceBuilders: MutableMap<ResourceFile, XmlResourceBuilder>,
        namespaceContext: NamespaceContext
    ): ResourceFile {

        val outputFile = getNextAttrResourceFile(resourceBuilders.size - 1, startElement)
        val outputBuilder = XmlResourceBuilder(outputFile)

        resourceBuilders[outputFile] = outputBuilder

        val errors = mutableListOf<String>()
        var foundChild = false

        while (eventReader.hasNext()) {
            val event = eventReader.nextEvent()

            if (event.isEndElement) {
                break
            }

            if (event.isCharacters) {
                if (!event.asCharacters().isWhiteSpace) {
                    errors += "${blameSource(source, event.location)}: " +
                            "Can't extract text into its own resource."
                }
                continue
            }

            if (event.isStartElement) {
                if (foundChild) {
                    errors += "${blameSource(source, event.location)}: " +
                            "Inline XML resources must have a single root."
                    walkToEndOfElement(event.asStartElement(), eventReader)
                    continue
                }
                foundChild = true
                processElement(
                  event.asStartElement(),
                  eventReader,
                  collectedIds,
                  resourceBuilders,
                  outputBuilder,
                  namespaceContext
                )
            }
        }

        if (!foundChild) {
            error(
                "${blameSource(source, startElement.location)}:" +
                        " No resource to outline. <${startElement.name}> block is empty.")
        }

        if (errors.any()) {
            error(errors.joinToString(separator = ","))
        }

        return outputFile
    }

    private fun processText(characters: Characters, builder: XmlResourceBuilder) =
        builder.addText(characters.data)

    private fun processComment(builder: XmlResourceBuilder) = builder.addComment()

    private fun gatherIds(
        startElement: StartElement,
        collectedIds: MutableMap<ResourceName, SourcedResourceName>
    ) {
        val iterator = startElement.attributes
        while (iterator.hasNext()) {
            val attribute = iterator.next() as Attribute

            val parsedRef = parseReference(attribute.value)
            parsedRef ?: continue

            val resourceName = parsedRef.reference.name
            if (parsedRef.createNew && resourceName.type == AaptResourceType.ID) {
                if (!isValidResourceEntryName(resourceName.entry!!)) {
                    error("${blameSource(source, startElement.location)} " +
                            "Id '$resourceName' has an invalid entry name '${resourceName.entry}'.")
                } else {
                    collectedIds.putIfAbsent(
                        resourceName,
                        SourcedResourceName(resourceName, startElement.location.lineNumber)
                    )
                }
            }
        }
    }

    private fun getNextAttrResourceFile(
        suffix: Int, element: StartElement
    ): ResourceFile {

        val newEntryName = mangleEntry("", "${primaryFile.name.entry}__$suffix")

        return ResourceFile(
            primaryFile.name.copy(entry = newEntryName),
            primaryFile.configuration,
            primaryFile.source.withLine(element.location.lineNumber),
            ResourceFile.Type.ProtoXml
        )
    }
}

class ResourceFile(
    val name: ResourceName,
    val configuration: ConfigDescription,
    val source: Source,
    val type: Type
) {
    enum class Type {
        Unknown,
        Png,
        BinaryXml,
        ProtoXml
    }
    val exportedSymbols = mutableListOf<SourcedResourceName>()
}

class XmlResource(val file: ResourceFile, val root: com.android.aapt.Resources.XmlNode? = null)

fun isValidResourceEntryName(name: String): Boolean = com.android.aaptcompiler.android.isValidResourceEntryName(name)
