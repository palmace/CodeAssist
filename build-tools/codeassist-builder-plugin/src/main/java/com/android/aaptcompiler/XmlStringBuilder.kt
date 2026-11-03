package com.android.aaptcompiler

import com.android.aaptcompiler.android.parseHex
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * A string flattened from an XML hierarchy, which maintains tags and untranslatable sections in
 * parallel data structures.
 */
data class FlattenedXml(
    val rawString: String,
    val styleString: StyleString,
    val untranslatableSections: List<UntranslatableSection>,
    val success: Boolean)

/**
 * Flattens an XML hierarchy into a FlattenedXml string.
 */
class XmlStringBuilder(private val preserveSpaces: Boolean = false) {
    var inQuote = preserveSpaces
        private set
    var lastCodepointWasSpace = false
        private set
    private var lastCodepointWasBackslash = false
    private var firstQuote = -1
    private var lastQuote = -1
    private var firstChar = -1
    private var lastChar = -1
    private var rawStringBuilder: StringBuilder = StringBuilder()
    private var textBuilder: StringBuilder = StringBuilder()

    private val spans = mutableListOf<StringPool.Span>()
    private val spanStack = mutableListOf<StringPool.Span>()

    private val untranslatableSections = mutableListOf<UntranslatableSection>()
    private var inUntranslatable = false

    var error = ""
        private set

    fun clear() {
        inQuote = preserveSpaces
        lastCodepointWasSpace = false
        lastCodepointWasBackslash = false
        firstQuote = -1
        lastQuote = -1
        firstChar = -1
        lastChar = -1
        rawStringBuilder = StringBuilder()
        textBuilder = StringBuilder()

        spans.clear()
        spanStack.clear()

        untranslatableSections.clear()
        inUntranslatable = false

        error = ""
    }

    fun handleEscape(
        codePoint: Int,
        textBuilder: StringBuilder,
        codePoints: PrimitiveIterator.OfInt,
        str: String
    ): Boolean {
        when (codePoint) {
            't'.code -> textBuilder.append('\t')
            'n'.code -> textBuilder.append('\n')
            '#'.code, '@'.code, '?'.code, '"'.code, '\''.code, '\\'.code ->
                textBuilder.appendCodePoint(codePoint)
            'u'.code -> {
                if (!appendUnicodeEscapeSequence(codePoints, textBuilder)) {
                    error = "Invalid unicode escape sequence in string\n\"$str\""
                    return false
                }
            }
            else -> textBuilder.appendCodePoint(codePoint)
        }
        return true
    }

    fun append(str : String) : XmlStringBuilder {
        rawStringBuilder.append(str)

        val codePoints = str.codePoints().iterator()
        while (codePoints.hasNext()) {
            var codePoint = codePoints.nextInt()

            if (!Character.isValidCodePoint(codePoint)) {
                error = "Invalid unicode code point in string\n\"$str\""
                return this
            }

            if (Character.isWhitespace(codePoint) && !preserveSpaces && !inQuote) {
                if (!lastCodepointWasSpace) {
                    textBuilder.append(' ')
                    lastCodepointWasSpace = true
                    lastCodepointWasBackslash = false
                }
                continue
            }

            if (firstChar == -1) {
                firstChar = textBuilder.length
            }

            lastCodepointWasSpace = false
            when {
                lastCodepointWasBackslash -> {
                    if (!handleEscape(codePoint, textBuilder, codePoints, str)) {
                        return this
                    }
                    lastChar = textBuilder.length
                    lastCodepointWasBackslash = false
                }
                codePoint == '\\'.code -> {
                    if (codePoints.hasNext()) {
                        codePoint = codePoints.nextInt()
                        if (!handleEscape(codePoint, textBuilder, codePoints, str)) {
                            return this
                        }
                        lastChar = textBuilder.length
                    } else {
                        lastCodepointWasBackslash = true
                    }
                }
                codePoint == '\"'.code && !preserveSpaces -> {
                    inQuote = !inQuote
                    if (firstQuote == -1) {
                        firstQuote = textBuilder.length
                    }
                    lastQuote = textBuilder.length
                    lastChar = textBuilder.length
                }
                codePoint == '\''.code && !preserveSpaces && !inQuote -> {
                    error = "Invalid unicode escape sequence in string\n\"$str\""
                    return this
                }
                else -> {
                    textBuilder.appendCodePoint(codePoint)
                    lastChar = textBuilder.length
                }
            }
        }

        return this
    }

    fun startSpan(name : String): XmlStringBuilder {
        if (error.isNotEmpty()) {
            return this
        }
        resetTextState()
        val span = StringPool.Span(name, textBuilder.length, -1)
        spans.add(span)
        spanStack.add(span)

        return this
    }

    fun endSpan(): XmlStringBuilder {
        if (error.isNotEmpty()) {
            return this
        }
        if (spanStack.isEmpty()) {
            error = "Unmatched span end."
            return this
        }
        resetTextState()
        val lastSpan = spanStack.removeAt(spanStack.lastIndex)
        lastSpan.lastChar = textBuilder.length - 1

        return this
    }

    fun startUntranslatable(): XmlStringBuilder {
        if (error.isNotEmpty()) {
            return this
        }
        if (inUntranslatable) {
            error = "Attempt to nest untranslatable sections in flattened xml, which is not allowed."
            return this
        }

        val section = UntranslatableSection(textBuilder.length)
        untranslatableSections.add(section)
        inUntranslatable = true

        return this
    }

    fun endUntranslatable(): XmlStringBuilder {
        if (error.isNotEmpty()) {
            return this
        }
        if (!inUntranslatable) {
            error = "Attempting to end untranslatable section while not in an untranslatable sections."
            return this
        }

        untranslatableSections.last().endIndex = textBuilder.length
        inUntranslatable = false

        return this
    }

    fun getFlattenedXml() : FlattenedXml {
        if (inUntranslatable) {
            error = "Attempting to flatten xml with unfinished untranslatable section."
        }
        if (spanStack.isNotEmpty()) {
            error = "Attempting to flatten xml with unfinished spans."
        }
        var untranslatables: List<UntranslatableSection> = untranslatableSections
        var text = textBuilder.toString()
        if (spans.isEmpty()) {

            var firstCharIdx = getFirstChar()
            if (untranslatableSections.isNotEmpty()) {
                firstCharIdx = min(firstCharIdx, untranslatableSections.first().startIndex)
            }

            var lastCharIdx = getLastChar(text)
            if (untranslatableSections.isNotEmpty()) {
                lastCharIdx = max(lastCharIdx, untranslatableSections.last().endIndex)
            }
            untranslatables = untranslatableSections.map {
                it.shift(-firstCharIdx)
            }

            text = if (firstCharIdx >= lastCharIdx) "" else textBuilder.substring(firstCharIdx, lastCharIdx)
        }

        val raw = rawStringBuilder.toString()
        return FlattenedXml(
            raw, StyleString(text, spans.toList()), untranslatables.toList(), error.isEmpty())
    }

    private fun getFirstChar(): Int {
        if (firstQuote == -1) {
            if (firstChar != -1) {
                return firstChar
            }
            return textBuilder.length
        }
        if (firstChar != -1 && firstChar < firstQuote)
            return 0
        return firstQuote
    }

    private fun getLastChar(text: String): Int {
        if (lastQuote == -1) {
            if (lastChar != -1) {
                return lastChar
            }
            return 0
        }
        if (lastChar != -1 && lastChar > lastQuote)
            return text.length
        return lastQuote
    }

    private fun appendUnicodeEscapeSequence(iter : Iterator<Int>, text: StringBuilder) : Boolean {
        var code = 0
        for (i in 1..4) {
            if (!iter.hasNext()) {
                break
            }
            val codePoint = iter.next()
            val value = parseHex(codePoint)
            if (value == -1) return false

            code = (code shl 4) or value
        }
        if (!Character.isValidCodePoint(code)) {
            return false
        }
        text.appendCodePoint(code)
        return true
    }

    private fun resetTextState() {
        inQuote = preserveSpaces
        lastCodepointWasSpace = false
        lastCodepointWasBackslash = false
    }

}
