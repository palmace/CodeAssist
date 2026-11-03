package org.openjdk.com.sun.org.apache.xpath.internal.jaxp;

import org.openjdk.javax.xml.xpath.XPathFactory;
import org.openjdk.javax.xml.xpath.XPathFactoryConfigurationException;
import org.openjdk.javax.xml.xpath.XPath;

public class XPathFactoryImpl extends XPathFactory {

    public XPathFactoryImpl() {}

    @Override
    public boolean isObjectModelSupported(String objectModel) {
        return objectModel.equals(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
    }

    @Override
    public XPath newXPath() {
        return null; 
    }

    @Override
    public void setFeature(String name, boolean value) throws XPathFactoryConfigurationException {}

    @Override
    public boolean getFeature(String name) throws XPathFactoryConfigurationException {
        return false;
    }

    @Override
    public void setXPathFunctionResolver(org.openjdk.javax.xml.xpath.XPathFunctionResolver resolver) {
    }

    @Override
    public void setXPathVariableResolver(org.openjdk.javax.xml.xpath.XPathVariableResolver resolver) {
    }
}