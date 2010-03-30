package org.orbeon.saxon.om;

import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Stack;

/**
 *
 */
public class PrefixNormalizer extends XMLFilterImpl {

    private HashMap uriToPrefix = new HashMap();    // contains the preferred prefix for each URI
    private Stack prefixes = new Stack();
    private Stack uris = new Stack();


    /**
     * Filter a start Namespace prefix mapping event.
     *
     * @param prefix The Namespace prefix.
     * @param uri    The Namespace URI.
     * @throws org.xml.sax.SAXException The client may throw
     *                                  an exception during processing.
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        super.startPrefixMapping(prefix, uri);
        prefixes.push(prefix);
        uris.push(uri);
        if (uriToPrefix.get(uri) == null) {
            uriToPrefix.put(uri, prefix);
        }
    }

    /**
     * Filter a start element event.
     *
     * @param uri       The element's Namespace URI, or the empty string.
     * @param localName The element's local name, or the empty string.
     * @param qName     The element's qualified (prefixed) name, or the empty
     *                  string.
     * @param atts      The element's attributes.
     * @throws org.xml.sax.SAXException The client may throw
     *                                  an exception during processing.
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        String newQName = qName;
        if (uri.length() != 0) {
            String preferredPrefix = (String)uriToPrefix.get(uri);
            if (!qName.startsWith(preferredPrefix)) {
                newQName = preferredPrefix + ':' + localName;
            }
        }
        int alen = atts.getLength();
        for (int a=0; a<alen; a++) {
            
        }
        super.startElement(uri, localName, qName, atts);    //AUTO
    }


    /**
     * Filter an end Namespace prefix mapping event.
     *
     * @param prefix The Namespace prefix.
     * @throws org.xml.sax.SAXException The client may throw
     *                                  an exception during processing.
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        super.endPrefixMapping(prefix);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

