package com.tyron.builder.api.variant.impl

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.tyron.builder.api.variant.BuiltArtifact
import com.tyron.builder.api.variant.FilterConfiguration
import com.tyron.builder.api.variant.VariantOutputConfiguration
import java.io.IOException
import java.io.Serializable
import java.nio.file.Path

/**
 * Re-implementación autónoma de BuiltArtifactImpl.
 */
data class BuiltArtifactImpl(
    override val outputFile: String,
    override val versionCode: Int?,
    override val versionName: String?,
    val variantOutputConfiguration: VariantOutputConfiguration = VariantOutputConfigurationImpl(),
    val attributes: Map<String, String> = mapOf()
) : BuiltArtifact, CommonBuiltArtifact, Serializable, VariantOutputConfiguration by variantOutputConfiguration {

    /**
     * Helper to get a filter of a specific type.
     */
    fun getFilter(filterType: FilterConfiguration.FilterType): FilterConfiguration? {
        return filters.find { it.filterType == filterType }
    }

    fun newOutput(newOutputFile: Path): BuiltArtifactImpl {
        return make(
            outputFile = newOutputFile.toString(),
            versionCode = versionCode,
            versionName = versionName,
            variantOutputConfiguration = variantOutputConfiguration,
            attributes = attributes
        )
    }

    companion object {
        @JvmStatic
        fun make(
            outputFile: String,
            versionCode: Int? = null,
            versionName: String? = null,
            variantOutputConfiguration: VariantOutputConfiguration = VariantOutputConfigurationImpl(),
            attributes: Map<String, String> = mapOf()
        ): BuiltArtifactImpl {
            val normalizedPath = outputFile.replace('\\', '/')
            return BuiltArtifactImpl(
                normalizedPath,
                versionCode,
                versionName,
                variantOutputConfiguration,
                attributes
            )
        }
    }
}

internal object BuiltArtifactTypeAdapter : TypeAdapter<BuiltArtifactImpl>() {
    override fun write(out: JsonWriter, value: BuiltArtifactImpl) {
        out.beginObject()
        out.name("outputFile").value(value.outputFile)
        out.name("versionCode").value(value.versionCode)
        out.name("versionName").value(value.versionName)
        out.name("type").value(value.outputType.toString())
        
        out.name("filters").beginArray()
        for (filter in value.filters) {
            out.beginObject()
            out.name("filterType").value(filter.filterType.toString())
            out.name("value").value(filter.identifier)
            out.endObject()
        }
        out.endArray()
        
        out.name("attributes").beginArray()
        for (attribute in value.attributes) {
            out.beginObject()
            out.name("key").value(attribute.key)
            out.name("value").value(attribute.value)
            out.endObject()
        }
        out.endArray()
        out.endObject()
    }

    override fun read(reader: JsonReader): BuiltArtifactImpl {
        var outputFile = ""
        var versionCode: Int? = null
        var versionName: String? = null
        var outputType = VariantOutputConfiguration.OutputType.SINGLE
        val filters = mutableListOf<FilterConfigurationImpl>()
        val attributes = mutableMapOf<String, String>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "outputFile" -> outputFile = reader.nextString()
                "versionCode" -> versionCode = if (reader.peek() == com.google.gson.stream.JsonToken.NULL) { reader.nextNull(); null } else reader.nextInt()
                "versionName" -> versionName = if (reader.peek() == com.google.gson.stream.JsonToken.NULL) { reader.nextNull(); null } else reader.nextString()
                "type" -> outputType = VariantOutputConfiguration.OutputType.valueOf(reader.nextString())
                "filters" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.beginObject()
                        var ft: FilterConfiguration.FilterType? = null
                        var fv = ""
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "filterType" -> ft = FilterConfiguration.FilterType.valueOf(reader.nextString())
                                "value" -> fv = reader.nextString()
                            }
                        }
                        if (ft != null) filters.add(FilterConfigurationImpl(ft, fv))
                        reader.endObject()
                    }
                    reader.endArray()
                }
                "attributes" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.beginObject()
                        var k = ""; var v = ""
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "key" -> k = reader.nextString()
                                "value" -> v = reader.nextString()
                            }
                        }
                        attributes[k] = v
                        reader.endObject()
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return BuiltArtifactImpl.make(
            outputFile, versionCode, versionName,
            VariantOutputConfigurationImpl(isUniversal = outputType == VariantOutputConfiguration.OutputType.UNIVERSAL, filters = filters),
            attributes
        )
    }
}
