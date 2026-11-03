package com.tyron.builder.compiler.manifest

import com.google.gson.Gson
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Cargador de archivos de navegación XML.
 * Reemplaza la funcionalidad de com.android.manifmerger.NavigationXmlLoader
 */
object NavigationXmlLoader {
    
    private val gson = Gson()
    
    fun load(navigationId: String, file: File, inputStream: InputStream): NavigationXmlDocumentData {
        return try {
            val doc = parseXml(inputStream)
            val graphs = extractNavGraphs(doc)
            NavigationXmlDocumentData(graphs)
        } catch (e: Exception) {
            NavigationXmlDocumentData(emptyList())
        }
    }
    
    private fun parseXml(inputStream: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(inputStream)
    }
    
    private fun extractNavGraphs(doc: Document): List<NavigationXmlDocumentData.NavGraphData> {
        val graphs = mutableListOf<NavigationXmlDocumentData.NavGraphData>()
        val navGraphNodes = doc.getElementsByTagName("navigation")
        
        for (i in 0 until navGraphNodes.length) {
            val node = navGraphNodes.item(i)
            val name = node.attributes.getNamedItem("android:name")?.nodeValue ?: "navigation"
            val deepLinks = extractDeepLinks(node)
            
            graphs.add(
                NavigationXmlDocumentData.NavGraphData(
                    name = name,
                    deepLinks = deepLinks
                )
            )
        }
        
        return graphs
    }
    
    private fun extractDeepLinks(node: Node): List<NavigationXmlDocumentData.DeepLinkData> {
        val deepLinks = mutableListOf<NavigationXmlDocumentData.DeepLinkData>()
        val childNodes = node.childNodes
        
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeName == "deepLink") {
                val uri = child.attributes.getNamedItem("android:uri")?.nodeValue
                val name = child.attributes.getNamedItem("android:name")?.nodeValue ?: ""
                
                if (uri != null) {
                    deepLinks.add(
                        NavigationXmlDocumentData.DeepLinkData(
                            uri = uri,
                            name = name
                        )
                    )
                }
            }
        }
        
        return deepLinks
    }
    
    fun toJson(data: NavigationXmlDocumentData): String {
        return gson.toJson(data)
    }
    
    fun fromJson(json: String): NavigationXmlDocumentData? {
        return try {
            gson.fromJson(json, NavigationXmlDocumentData::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
