package com.android.aaptcompiler

import com.android.aaptcompiler.android.ResTableConfig

/**
 * Handles pseudolocalization of resources.
 */
class PseudolocaleGenerator {

    fun consume(table: ResourceTable) {
        // Mock implementation for now to allow compilation
    }
}

class Pseudolocalizer(val method: Method) {
    enum class Method {
        ACCENT,
        BIDI
    }
    fun start(): String = ""
    fun text(text: String): String = text
    fun end(): String = ""
}
