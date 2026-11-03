package com.android.aaptcompiler

/**
 * A class that keeps track of the current namespace context.
 */
class NamespaceContext : javax.xml.namespace.NamespaceContext {
    private val prefixes = mutableMapOf<String, MutableList<String>>()

    internal fun push(prefix: String, uri: String) {
        prefixes.getOrPut(prefix) { mutableListOf() }.add(uri)
    }

    internal fun pop(prefix: String) {
        prefixes[prefix]?.let {
            if (it.isNotEmpty()) {
                it.removeAt(it.size - 1)
            }
        }
    }

    fun findUri(prefix: String): String? {
        return prefixes[prefix]?.lastOrNull()
    }

    override fun getNamespaceURI(prefix: String): String {
        return findUri(prefix) ?: ""
    }

    override fun getPrefix(namespaceURI: String): String? {
        return prefixes.entries.find { it.value.contains(namespaceURI) }?.key
    }

    override fun getPrefixes(namespaceURI: String): Iterator<String> {
        return prefixes.entries.filter { it.value.contains(namespaceURI) }.map { it.key }.iterator()
    }
}
