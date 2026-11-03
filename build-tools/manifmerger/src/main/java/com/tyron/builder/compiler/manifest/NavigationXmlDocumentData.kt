package com.tyron.builder.compiler.manifest

import com.google.gson.annotations.SerializedName

/**
 * Data class para representar los datos extraídos de archivos de navegación XML.
 * Equivalente a la versión original de com.android.manifmerger.NavigationXmlDocumentData
 */
data class NavigationXmlDocumentData(
    @SerializedName("graphs")
    val graphs: List<NavGraphData>
) {
    data class NavGraphData(
        @SerializedName("name")
        val name: String,
        
        @SerializedName("deepLinks")
        val deepLinks: List<DeepLinkData>
    )
    
    data class DeepLinkData(
        @SerializedName("uri")
        val uri: String,
        
        @SerializedName("name")
        val name: String
    )

    fun convertToData(placeholders: Map<String, String>, forAar: Boolean): NavigationXmlDocumentData {
        return this
    }
}
