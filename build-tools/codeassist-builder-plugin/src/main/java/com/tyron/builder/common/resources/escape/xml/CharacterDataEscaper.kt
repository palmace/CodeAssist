package com.tyron.builder.common.resources.escape.xml

import com.google.common.escape.Escapers
import com.tyron.builder.common.resources.escape.xml.StringResourceContentHandler.STRING_ELEMENT_NAME
// Importamos explícitamente para evitar ambigüedades con org.openjdk
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.ContentHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import java.io.IOException
import java.io.StringReader
import java.util.regex.Pattern

/**
 * Static singleton responsible for escaping character data in XML.
 */
object CharacterDataEscaper {
  private const val DECIMAL_ESCAPE = "___D"
  private const val HEXADECIMAL_ESCAPE = "___X"
  private val DECIMAL_REFERENCE = Pattern.compile("""&#(\d+);""")
  private val HEXADECIMAL_REFERENCE = Pattern.compile("""&#x(\p{XDigit}+);""")
  private val ESCAPED_DECIMAL_REFERENCE = Pattern.compile("""$DECIMAL_ESCAPE(\d+);""")
  private val ESCAPED_HEXADECIMAL_REFERENCE =
      Pattern.compile("""$HEXADECIMAL_ESCAPE(\p{XDigit}+);""")
  
  // Forzamos el uso de la factory estándar
  private val saxParserFactory: SAXParserFactory = SAXParserFactory.newInstance().apply {
      isNamespaceAware = false
  }

  private val escapedCharReplacements =
      mapOf(
          ' ' to ' ',
          '"' to '"',
          '\'' to '\'',
          '\\' to '\\',
          'n' to '\n',
          't' to '\t',
      )

  @JvmStatic
  fun escape(string: String): String {
    if (string.isEmpty()) return ""
    val xml = string.escapeCharacterReferences()
    val builder: StringBuilder = StringBuilder(xml.length * 3 / 2)
    if (xml.startsWith("?") || xml.startsWith("@")) builder.append("""\""")
    createEscapingContentHandler(builder, !xml.startsOrEndsWithSpace()).parse(xml)
    val s = builder.toString().unescapeCharacterReferences()
    return if (s.startsOrEndsWithSpace()) """"$s"""" else s
  }

  @JvmStatic
  fun unescape(string: String): String {
    if (string.isEmpty()) return ""
    val xml = string.escapeCharacterReferences().unescapeLeadingQuestionMarkOrAtSign()
    val builder: StringBuilder = StringBuilder(xml.length)
    createUnescapingContentHandler(builder).parse(xml)
    return builder.toString().unescapeCharacterReferences()
  }

  @Suppress("UnstableApiUsage")
  private fun createEscapingContentHandler(
      stringBuilder: StringBuilder,
      escapeApostrophes: Boolean
  ): ContentHandler {
    val builder =
        Escapers.builder()
            .addEscape('"', """\"""")
            .addEscape('\\', """\\""")
            .addEscape('\n', """\n""")
            .addEscape('\t', """\t""")
    if (escapeApostrophes) builder.addEscape('\'', """\'""")
    val escaper = builder.build()
    return StringResourceContentHandler(stringBuilder) { stringBuilder.append(escaper.escape(it)) }
  }

  private fun createUnescapingContentHandler(stringBuilder: StringBuilder): ContentHandler =
      StringResourceContentHandler(stringBuilder) {
        stringBuilder.append(it.stripUnescapedQuotes().unescapeChars())
      }

  private fun Char.getReplacement(): Char =
      escapedCharReplacements[this] ?: throw IllegalArgumentException(toString())

  private fun String.stripUnescapedQuotes(): String = filterIndexed { i, c ->
    c != '"' || containsEscapedCharAt(i)
  }

  private fun String.unescapeChars(): String =
      mapIndexedNotNull { i, c ->
            when {
              i < length - 1 && shouldUnescapeCharAt(i + 1) -> null
              shouldUnescapeCharAt(i) -> c.getReplacement()
              else -> c
            }
          }
          .joinToString("")

  private fun String.startsOrEndsWithSpace(): Boolean = startsWith(" ") || endsWith(" ")

  private fun String.shouldUnescapeCharAt(index: Int): Boolean =
      get(index) in escapedCharReplacements && containsEscapedCharAt(index)

  private tailrec fun String.containsEscapedCharAt(index: Int, negated: Boolean = false): Boolean {
    if (index in 1..length && get(index - 1) == '\\') {
      return containsEscapedCharAt(index - 1, !negated)
    }
    return negated
  }

  private fun String.unescapeLeadingQuestionMarkOrAtSign(): String =
      if (startsWith("""\?""") || startsWith("""\@""")) substring(1) else this

  private fun String.escapeCharacterReferences(): String {
    val s = DECIMAL_REFERENCE.matcher(this).replaceAll("$DECIMAL_ESCAPE$1;")
    return HEXADECIMAL_REFERENCE.matcher(s).replaceAll("$HEXADECIMAL_ESCAPE$1;")
  }

  private fun String.unescapeCharacterReferences(): String {
    val s = ESCAPED_DECIMAL_REFERENCE.matcher(this).replaceAll("&#$1;")
    return ESCAPED_HEXADECIMAL_REFERENCE.matcher(s).replaceAll("&#x$1;")
  }

  private fun ContentHandler.parse(string: String) {
    try {
      val reader: XMLReader = saxParserFactory.newSAXParser().xmlReader
      reader.contentHandler = this
      val xml = "<$STRING_ELEMENT_NAME>$string</$STRING_ELEMENT_NAME>"
      reader.parse(InputSource(StringReader(xml)))
    } catch (exception: Exception) {
      throw RuntimeException(exception)
    }
  }
}
