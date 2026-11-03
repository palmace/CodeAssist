package com.tyron.builder.common.utils;

import com.android.ide.common.blame.SourceFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

public class XmlUtilsWorkaround {

    public static SAXParserFactory configureSaxFactory(
            SAXParserFactory factory,
            boolean namespaceAware,
            boolean checkDtd
    ) {
        factory.setNamespaceAware(namespaceAware);
        factory.setValidating(checkDtd);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
        } catch (Exception ignored) {
        }
        return factory;
    }

    public static SAXParser createSaxParser(SAXParserFactory factory) throws ParserConfigurationException, SAXException {
        return factory.newSAXParser();
    }

    public static String toXml(Node node) {
        return toXml(node, null);
    }

    public static String toXml(Node node, Map<?, ?> blame) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Document parseUtfXmlFile(File file, boolean namespaceAware) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            return builder.parse(new InputSource(is));
        }
    }

    public static boolean hasElementChildren(Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
            child = child.getNextSibling();
        }
        return false;
    }

    public static void attachSourceFile(Node node, SourceFile sourceFile) {
        // En un entorno limitado, esta funcionalidad de blame puede ser simplificada o ignorada
        // si no es crítica para la generación del APK.
        node.setUserData("sourcefile", sourceFile, null);
    }

    public static void appendXmlAttributeValue(StringBuilder sb, String value) {
        for (int i = 0, n = value.length(); i < n; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;"); break;
                case '<':  sb.append("&lt;"); break;
                case '>':  sb.append("&gt;"); break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:   sb.append(c);
            }
        }
    }

    public static void appendXmlTextValue(StringBuilder sb, String value) {
        for (int i = 0, n = value.length(); i < n; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;"); break;
                case '<':  sb.append("&lt;"); break;
                case '>':  sb.append("&gt;"); break;
                default:   sb.append(c);
            }
        }
    }
}
