package org.orbeon.saxon.event;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.Configuration;

/**
 * DocumentValidator checks that a document is well-formed: specifically, that it contains a single element
 * node child and no text node children.
 */

public class DocumentValidator extends ProxyReceiver
{
    boolean foundElement = false;
    int level = 0;

    public void setPipelineConfiguration(PipelineConfiguration config) {
        super.setPipelineConfiguration(config);
    }

    /**
     * Start of an element
     * @param nameCode
     * @param typeCode
     * @param locationId
     * @param properties
     * @throws XPathException
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (foundElement && level==0) {
            DynamicError de = new DynamicError("A valid document must have only one child element");
            if (getPipelineConfiguration().getHostLanguage() == Configuration.XSLT) {
                de.setErrorCode("XTTE1550");
            } else {
                de.setErrorCode("XQDY0061");
            }
            throw de;
        }
        foundElement = true;
        level++;
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (level == 0) {
            if (Whitespace.isWhite(chars)) {
                return; // ignore whitespace outside the outermost element
            }
            DynamicError de = new DynamicError("A valid document must contain no text outside the outermost element");
            if (getPipelineConfiguration().getHostLanguage() == Configuration.XSLT) {
                de.setErrorCode("XTTE1550");
            } else {
                de.setErrorCode("XQDY0061");
            }
            throw de;
        }
        nextReceiver.characters(chars, locationId, properties);
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        level--;
        nextReceiver.endElement();
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (level==0) {
            if (!foundElement) {
                DynamicError de = new DynamicError("A valid document must have a child element");
                if (getPipelineConfiguration().getHostLanguage() == Configuration.XSLT) {
                    de.setErrorCode("XTTE1550");
                } else {
                    de.setErrorCode("XQDY0061");
                }
                throw de;
            }
            foundElement = false;
            nextReceiver.endDocument();
            level = -1;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
