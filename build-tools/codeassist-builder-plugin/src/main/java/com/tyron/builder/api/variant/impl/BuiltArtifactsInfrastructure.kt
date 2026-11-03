package com.tyron.builder.api.variant.impl

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.tyron.builder.api.artifact.Artifact
import com.tyron.builder.gradle.internal.api.artifact.toArtifactType
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.util.Properties

/**
 * Interfaces base reconstruidas.
 */
interface CommonBuiltArtifact : Serializable {
    val outputFile: String
    val versionCode: Int?
    val versionName: String?
}

interface CommonBuiltArtifacts : Serializable {
    val version: Int
    val artifactType: Any
    val applicationId: String
    val variantName: String
    val elements: Collection<CommonBuiltArtifact>
}

object ListingFileRedirect {
    fun maybeExtractRedirectedFile(inputFile: File, content: String): File? {
        if (content.startsWith("redirect=")) {
            val props = Properties()
            props.load(content.reader())
            val redirect = props.getProperty("redirect")
            if (redirect != null) {
                val redirectedFile = File(redirect)
                return if (redirectedFile.isAbsolute) redirectedFile else File(inputFile.parentFile, redirect)
            }
        }
        return null
    }
}

internal object ArtifactTypeTypeAdapter : TypeAdapter<Artifact<*>>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Artifact<*>) {
        out.beginObject()
        out.name("type").value(value.name())
        out.name("kind").value(value.kind.dataType().simpleName)
        out.endObject()
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): Artifact<*> {
        reader.beginObject()
        var artifactType: Artifact<*>? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> artifactType = reader.nextString().toArtifactType()
                "kind" -> reader.nextString()
            }
        }
        reader.endObject()
        return artifactType ?: throw IOException("Invalid artifact type declaration")
    }
}

internal abstract class CommonBuiltArtifactTypeAdapter<T> : TypeAdapter<T>() {
    @Throws(IOException::class)
    fun read(
        reader: JsonReader,
        attributeReader: (String) -> Unit,
        instantiator: (String, Int?, String?) -> T
    ): T {
        var outputFile = ""
        var versionCode: Int? = null
        var versionName: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when (name) {
                "outputFile" -> outputFile = reader.nextString()
                "versionCode" -> versionCode = if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
                    reader.nextNull()
                    null
                } else {
                    reader.nextInt()
                }
                "versionName" -> versionName = if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
                    reader.nextNull()
                    null
                } else {
                    reader.nextString()
                }
                else -> attributeReader(name)
            }
        }
        reader.endObject()
        return instantiator(outputFile, versionCode, versionName)
    }
}

internal abstract class CommonBuiltArtifactsTypeAdapter<T, AT, E> : TypeAdapter<T>() {
    abstract val artifactTypeTypeAdapter: TypeAdapter<AT>
    abstract val elementTypeAdapter: TypeAdapter<E>
    
    abstract fun getArtifactType(artifacts: T): AT
    abstract fun getElementType(artifacts: T): String?
    abstract fun getElements(artifacts: T): Collection<E>
    
    abstract fun instantiate(
        version: Int,
        artifactType: AT,
        applicationId: String,
        variantName: String,
        elements: List<E>,
        elementType: String?
    ): T

    override fun write(out: JsonWriter, value: T) {
        out.beginObject()
        out.name("version").value(1)
        out.name("artifactType")
        artifactTypeTypeAdapter.write(out, getArtifactType(value))
        out.name("applicationId").value("TODO")
        out.name("variantName").value("TODO")
        out.name("elements").beginArray()
        for (element in getElements(value)) {
            elementTypeAdapter.write(out, element)
        }
        out.endArray()
        out.name("elementType").value(getElementType(value))
        out.endObject()
    }

    override fun read(reader: JsonReader): T {
        var version = 0
        var artifactType: AT? = null
        var applicationId = ""
        var variantName = ""
        val elements = mutableListOf<E>()
        var elementType: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "version" -> version = reader.nextInt()
                "artifactType" -> artifactType = artifactTypeTypeAdapter.read(reader)
                "applicationId" -> applicationId = reader.nextString()
                "variantName" -> variantName = reader.nextString()
                "elements" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        elements.add(elementTypeAdapter.read(reader))
                    }
                    reader.endArray()
                }
                "elementType" -> elementType = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return instantiate(
            version,
            artifactType ?: throw IOException("Missing artifactType"),
            applicationId,
            variantName,
            elements,
            elementType
        )
    }
}
