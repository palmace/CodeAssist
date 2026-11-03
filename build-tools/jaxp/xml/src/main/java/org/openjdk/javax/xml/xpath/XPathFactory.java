package org.openjdk.javax.xml.xpath;

import org.openjdk.javax.xml.XMLConstants;
import org.openjdk.javax.xml.namespace.QName;
import org.xml.sax.InputSource;

public abstract class XPathFactory {

    public static final String DEFAULT_PROPERTY_NAME = "javax.xml.xpath.XPathFactory";
    public static final String DEFAULT_OBJECT_MODEL_URI = "http://java.sun.com/jaxp/xpath/dom";

    protected XPathFactory() {}

    public static XPathFactory newInstance() {
        try {
            return newInstance(DEFAULT_OBJECT_MODEL_URI);
        } catch (XPathFactoryConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static XPathFactory newInstance(final String uri) throws XPathFactoryConfigurationException {
        if (uri == null) throw new NullPointerException("uri == null");
        if (uri.length() == 0) throw new IllegalArgumentException("uri is empty");

        if (DEFAULT_OBJECT_MODEL_URI.equals(uri)) {
            try {
                // Carga dinámica para evitar dependencia circular con jaxp-internal
                return (XPathFactory) Class.forName("org.openjdk.com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl").newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        XPathFactory f = new XPathFactoryFinder(null).newFactory(uri);
        if (f == null) throw new XPathFactoryConfigurationException("No factory found");
        return f;
    }

    public static XPathFactory newInstance(String uri, String factoryClassName, ClassLoader classLoader)
        throws XPathFactoryConfigurationException {
        XPathFactory f = new XPathFactoryFinder(classLoader).createInstance(factoryClassName);
        if (f == null || !f.isObjectModelSupported(uri)) throw new XPathFactoryConfigurationException("Factory error");
        return f;
    }

    public abstract boolean isObjectModelSupported(String objectModel);
    public abstract void setFeature(String name, boolean value) throws XPathFactoryConfigurationException;
    public abstract boolean getFeature(String name) throws XPathFactoryConfigurationException;
    public abstract void setXPathVariableResolver(XPathVariableResolver resolver);
    public abstract void setXPathFunctionResolver(XPathFunctionResolver resolver);
    public abstract XPath newXPath();
}