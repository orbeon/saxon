package net.sf.saxon.om;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
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
    
    public void setAttribute(String name, Object value) {
        throw new IllegalArgumentException("Unrecognized attribute name: " + name);
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