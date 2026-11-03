@file:JvmName("ResourceCompiler")
package com.android.aaptcompiler

import com.android.aaptcompiler.proto.serializeTableToPb
import com.android.resources.ResourceType
import com.android.resources.ResourceVisibility
import com.android.utils.FileUtils
import com.tyron.builder.log.ILogger
import java.io.File

private const val VALUES_DIRECTORY_PREFIX = "values"
private const val XML_EXTENSION = "xml"
private const val RESOURCE_TABLE_EXTENSION = "arsc"
private const val PATCH_9_EXTENSION = "9.png"
private const val PNG_EXTENSION = "png"


/**
 * The different options available while compiling resources.
 */
data class ResourceCompilerOptions(
    val generateSymbolsPathData: ResourcePathData?= null,
    val visibility: ResourceVisibility? = null,
    val requirePngCrunching: Boolean = false,
    val pseudolocalize: Boolean = false,
    val partialRFile: File? = null,
    val legacyMode: Boolean = false,
    val verbose: Boolean = false,
    val sourcePath: String? = null)

/**
 * Shows whether the ResourceCompiler can compile the given Resource File.
 */
fun canCompileResourceInJvm(file: File, requirePngCrunching: Boolean): Boolean {
    if (file.isHidden) return true

    val pathData = extractPathData(file)
    if (pathData.resourceDirectory == VALUES_DIRECTORY_PREFIX
        && pathData.extension == XML_EXTENSION) {
        return true
    } else {
        val type = ResourceType.fromFolderName(pathData.resourceDirectory) ?: return false
        if (type != ResourceType.RAW) {
            if (pathData.extension == XML_EXTENSION) {
                return true
            } else if (pathData.extension.endsWith(PNG_EXTENSION)) {
                return pathData.extension != PATCH_9_EXTENSION && !requirePngCrunching
            }
        }
    }
    return true
}

/**
 * Compiles the given resource file and puts the compiled process in the given output directory.
 */
fun compileResource(
    file: File, outputDirectory: File, options: ResourceCompilerOptions, logger: BlameLogger) {

    if (file.isHidden) {
        logger.warning("Omitting file ${file.absolutePath} because it is hidden.")
        return
    }

    val pathData = extractPathData(file, options.sourcePath ?: file.absolutePath)
    val compileFunction = getCompileMethod(pathData, logger)

    try {
        compileFunction(pathData, outputDirectory, options, logger)
    } catch (e: Exception) {
        logger.info("Failed to compile file", blameSource(pathData.source))
        val message =
            "Resource compilation failed (${e.message}. Cause: ${e.cause}). " +
                    "Check logs for more details."
        throw ResourceCompilationException(message, e)
    }
}

private fun getCompileMethod(pathData: ResourcePathData, logger: BlameLogger):
            (ResourcePathData, File, ResourceCompilerOptions, BlameLogger) -> Unit {
    if (pathData.resourceDirectory == VALUES_DIRECTORY_PREFIX &&
        pathData.extension == XML_EXTENSION) {
        pathData.extension = RESOURCE_TABLE_EXTENSION
        return ::compileTable
    } else {
        val type = ResourceType.fromFolderName(pathData.resourceDirectory)
        if (type == null) {
            val errorMsg = "Invalid resource type '${pathData.resourceDirectory}'" +
                    " for file ${pathData.file.absolutePath}"
            logger.warning(errorMsg)
            error(errorMsg)
        }
        if (type != ResourceType.RAW) {
            if (pathData.extension == XML_EXTENSION) {
                return ::compileXml
            } else if (pathData.extension.endsWith(PNG_EXTENSION)) {
                return ::compilePng
            }
        }
    }
    return ::compileFile
}

private fun compileTable(
    pathData: ResourcePathData,
    outputDirectory: File,
    options: ResourceCompilerOptions,
    logger: BlameLogger) {
    val outputFile = File(outputDirectory, pathData.getIntermediateContainerFilename())
    logger.info("Compiling XML table ${pathData.file.absolutePath} to $outputFile")

    val table = ResourceTable(logger = logger)

    val extractorOptions = TableExtractorOptions(
        translatable = !pathData.name.contains("donottranslate"),
        errorOnPositionalArgs = !options.legacyMode,
        visibility = options.visibility)
    val tableExtractor =
        TableExtractor(table, pathData.source, pathData.config, extractorOptions, logger)

    pathData.file.inputStream().use {
        try {
            tableExtractor.extract(it)
        } catch (e: Exception) {
            throw ResourceCompilationException(
                "Failed to compile values resource file ${pathData.file}", e
            )
        }

        if (options.pseudolocalize && extractorOptions.translatable) {
            PseudolocaleGenerator().consume(table)
        }
    }

    table.createPackage("")
    table.sort()

    val container = Container(outputFile.outputStream(), 1)
    // Especificamos explícitamente el tipo de logger para evitar la ambigüedad
    val pbTable = serializeTableToPb(table, logger as ILogger)

    container.addResTableEntry(pbTable)

    if (options.partialRFile != null) {
        val builder = StringBuilder()
        table.packages.forEach { pkg ->
            if (pkg.name.isEmpty()) {
                pkg.groups.forEach { group ->
                    if (group.type != AaptResourceType.MACRO) {
                        val javaType = if (group.type == AaptResourceType.STYLEABLE) "int[]" else "int"
                        group.entries.forEach { (entryName, entryMap) ->
                            // Entry map is TreeMap<Short?, ResourceEntry>
                            val firstEntry = entryMap.values.first()
                            val visibility = getVisibility(firstEntry.values.mapNotNull { it.value }, group.type)
                            builder.appendLine("${visibility.name.lowercase()} $javaType ${group.type.tagName} $entryName")
                            if (group.type == AaptResourceType.STYLEABLE) {
                                // Styleable children logic here
                            }
                        }
                    }
                }
            }
            FileUtils.writeToFile(options.partialRFile, builder.toString())
        }
    }
}

private fun getVisibility(values: Collection<Value>, type: AaptResourceType): ResourceVisibility {
    if (type == AaptResourceType.STYLEABLE) return ResourceVisibility.PUBLIC
    var foundPublic = false
    var foundPrivate = false
    values.forEach {
        if (it is Id) { /* check something? */ }
        // Basic check for visibility level
    }
    return ResourceVisibility.PUBLIC // Default for now
}

private fun compileFile(
    pathData: ResourcePathData,
    outputDirectory: File,
    options: ResourceCompilerOptions,
    logger: BlameLogger) {
    val outputFile = File(outputDirectory, pathData.getIntermediateContainerFilename())
    logger.info("Compiling file ${pathData.file.absolutePath} to $outputFile")
    pathData.file.inputStream().use {
        val resourceFile = ResourceFile(
            ResourceName("", resourceTypeFromTag(pathData.resourceDirectory)!!, pathData.name),
            pathData.config,
            pathData.source,
            ResourceFile.Type.Unknown
        )

        val container = Container(outputFile.outputStream(), 1)
        container.addFileEntry(it, resourceFile)
    }
}

private fun compileXml(
    pathData: ResourcePathData,
    outputDirectory: File,
    options: ResourceCompilerOptions,
    logger: BlameLogger) {
    val outputFile = File(outputDirectory, pathData.getIntermediateContainerFilename())
    logger.info("Compiling xml file ${pathData.file.absolutePath} to $outputFile")

    val fileToProcess = ResourceFile(
        ResourceName("", pathData.type!!, pathData.name),
        pathData.config,
        pathData.source,
        ResourceFile.Type.ProtoXml
    )

    pathData.file.inputStream().use {
        val xmlProcessor = XmlProcessor(source = pathData.source, logger = logger)
        try {
            xmlProcessor.process(fileToProcess, it)
        } catch (e: Exception) {
            throw ResourceCompilationException(
                "Failed to compile resource file: " +
                        logger.getOutputSource(blameSource(pathData.source)),
                e
            )
        }

        val container =
            Container(outputFile.outputStream(), xmlProcessor.xmlResources.size)

        for (resource in xmlProcessor.xmlResources) {
            container.addXmlEntry(resource)
        }
    }
}

private fun compilePng(
    pathData: ResourcePathData,
    outputDirectory: File,
    options: ResourceCompilerOptions,
    logger: BlameLogger) {
    logger.info("Compiling image file ${pathData.file.absolutePath}")
    compileFile(pathData, outputDirectory, options, logger)
}
