package net.sf.saxon.dom;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
* Implementation of JAXP 1.1 DocumentBuilderFactory. To build a Document using
* Saxon, set the system property javax.xml.parsers.DocumentBuilderFactory to
* "net.sf.saxon.om.DocumentBuilderFactoryImpl" and then call
* DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource);
*/

public class DocumentBuilderFactoryImpl extends DocumentBuilderFactory {
    
    public DocumentBuilderFactoryImpl() {
        setCoalescing(true);
        setExpandEntityReferences(true);
        setIgnoringComments(false);
        setIgnoringElementContentWhitespace(false);
        setNamespaceAware(true);
        setValidating(false);
    }

    public void setAttribute(String name, Object value) {
        throw new IllegalArgumentException("Unrecognized attribute name: " + name);
    }

    public Object getAttribute(String name) {
        throw new IllegalArgumentException("Unrecognized attribute name: " + name);
    }
    
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {

        // Check that configuration options are all available

        if (!isExpandEntityReferences()) {
            throw new ParserConfigurationException(
                "Saxon parser always expands entity references");
        }
        if (isIgnoringComments()) {
            throw new ParserConfigurationException(
                "Saxon parser does not allow comments to be ignored");
        }        
        if (isIgnoringElementContentWhitespace()) {
            throw new ParserConfigurationException(
                "Saxon parser does not allow whitespace in element content to be ignored");
        }        
        if (!isNamespaceAware()) {
            throw new ParserConfigurationException(
                "Saxon parser is always namespace aware");
        } 
        if (isValidating()) {
            throw new ParserConfigurationException(
                "Saxon parser is non-validating");
        }

        return new DocumentBuilderImpl();
    }

    /**
     * <p>Set a feature for this <code>DocumentBuilderFactory</code> and <code>DocumentBuilder</code>s created by this factory.</p>
     * <p/>
     * <p/>
     * Feature names are fully qualified {@link java.net.URI}s.
     * Implementations may define their own features.
     * An {@link javax.xml.parsers.ParserConfigurationException} is thrown if this <code>DocumentBuilderFactory</code> or the
     * <code>DocumentBuilder</code>s it creates cannot support the feature.
     * It is possible for an <code>DocumentBuilderFactory</code> to expose a feature value but be unable to change its state.
     * </p>
     * <p/>
     * <p/>
     * All implementations are required to support the {@link javax.xml.XMLConstants#FEATURE_SECURE_PROCESSING} feature.
     * When the feature is:</p>
     * <ul>
     * <li>
     * <code>true</code>: the implementation will limit XML processing to conform to implementation limits.
     * Examples include entity expansion limits and XML Schema constructs that would consume large amounts of resources.
     * If XML processing is limited for security reasons, it will be reported via a call to the registered
     * {@link org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException exception)}.
     * See {@link  javax.xml.parsers.DocumentBuilder#setErrorHandler(org.xml.sax.ErrorHandler errorHandler)}.
     * </li>
     * <li>
     * <code>false</code>: the implementation will processing XML according to the XML specifications without
     * regard to possible implementation limits.
     * </li>
     * </ul>
     *
     * @param name  Feature name.
     * @param value Is feature state <code>true</code> or <code>false</code>.
     * @throws javax.xml.parsers.ParserConfigurationException
     *                              if this <code>DocumentBuilderFactory</code> or the <code>DocumentBuilder</code>s
     *                              it creates cannot support this feature.
     * @throws NullPointerException If the <code>name</code> parameter is null.
     */
    public void setFeature(String name, boolean value) throws ParserConfigurationException {
        if (name.equals(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING) && !value) {
            // no action
        } else {
            throw new ParserConfigurationException("Unsupported feature or value: " + name);
        }
    }

    /**
     * <p>Get the state of the named feature.</p>
     * <p/>
     * <p/>
     * Feature names are fully qualified {@link java.net.URI}s.
     * Implementations may define their own features.
     * An {@link javax.xml.parsers.ParserConfigurationException} is thrown if this <code>DocumentBuilderFactory</code> or the
     * <code>DocumentBuilder</code>s it creates cannot support the feature.
     * It is possible for an <code>DocumentBuilderFactory</code> to expose a feature value but be unable to change its state.
     * </p>
     *
     * @param name Feature name.
     * @return State of the named feature.
     * @throws javax.xml.parsers.ParserConfigurationException
     *          if this <code>DocumentBuilderFactory</code>
     *          or the <code>DocumentBuilder</code>s it creates cannot support this feature.
     */
    public boolean getFeature(String name) throws ParserConfigurationException {
        if (name.equals(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING)) {
            return false;
        } else {
            throw new ParserConfigurationException("Unsupported feature: " + name);
        }
    }

}         


//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//