package org.orbeon.saxon.event;

import org.orbeon.saxon.om.AttributeCollectionImpl;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.transform.OutputKeys;
import java.util.Properties;

/**
 * The MetaTagAdjuster adds a meta element to the content of the head element, indicating
 * the required content type and encoding; it also removes any existing meta element
 * containing this information
 */

public class MetaTagAdjuster extends ProxyReceiver {

    boolean seekingHead = true;
    int droppingMetaTags = -1;
    boolean inMetaTag = false;
    boolean foundHead = false;
    String headPrefix = null;
    int metaCode;
    short requiredURICode = 0;
    AttributeCollectionImpl attributes;
    String encoding;
    String mediaType;
    int level = 0;
    boolean isXHTML = false;

    /**
     * Create a new MetaTagAdjuster
     */

    public MetaTagAdjuster() {
    }

    /**
    * Set output properties
    */

    public void setOutputProperties(Properties details) {
        encoding = details.getProperty(OutputKeys.ENCODING);
        if (encoding == null) {
            encoding = "UTF-8";
        }
        mediaType = details.getProperty(OutputKeys.MEDIA_TYPE);
        if (mediaType == null) {
            mediaType = "text/html";
        }
    }

    /**
     * Indicate whether we're handling HTML or XHTML
     */

    public void setIsXHTML(boolean xhtml) {
        isXHTML = xhtml;
        if (xhtml) {
            requiredURICode = getNamePool().getCodeForURI(NamespaceConstant.XHTML);
        } else {
            requiredURICode = 0;
        }
    }

    /**
     * Compare a name: case-blindly in the case of HTML, case-sensitive for XHTML
     */

    private boolean comparesEqual(String name1, String name2) {
        if (isXHTML) {
            return name1.equals(name2);
        } else {
            return name1.equalsIgnoreCase(name2);
        }

    }

    /**
     * Notify the start of an element
     *
     * @param nameCode   integer code identifying the name of the element within the name pool.
     * @param typeCode   integer code identifying the element's type within the name pool.
     * @param properties properties of the element node
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (droppingMetaTags == level) {
            metaCode = nameCode;
            int uriCode = getNamePool().getURICode(nameCode);
            String localName = getNamePool().getLocalName(nameCode);
            if (uriCode == requiredURICode && comparesEqual(localName, "meta")) {
                inMetaTag = true;
                attributes.clear();
                return;
            }
        }
        level++;
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
        if (seekingHead) {
            NamePool namePool = getNamePool();
            int uriCode = namePool.getURICode(nameCode);
            String localName = namePool.getLocalName(nameCode);
            if (uriCode == requiredURICode && comparesEqual(localName, "head")) {
                foundHead = true;
                headPrefix = namePool.getPrefix(nameCode);
            }
        }

    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        if (inMetaTag) {
            attributes.addAttribute(nameCode, typeCode, value.toString(), locationId, properties);
        } else {
            nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
        }
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        if (foundHead) {
            foundHead = false;
            NamePool namePool = getNamePool();
            nextReceiver.startContent();
            int metaCode = namePool.allocate(headPrefix, requiredURICode, "meta");
            nextReceiver.startElement(metaCode, StandardNames.XS_UNTYPED, 0, 0);
            int httpEquivCode = namePool.allocate("", "", "http-equiv");
            nextReceiver.attribute(httpEquivCode, StandardNames.XS_UNTYPED_ATOMIC, "Content-Type", 0, 0);
            int contentCode = namePool.allocate("", "", "content");
            nextReceiver.attribute(contentCode, StandardNames.XS_UNTYPED_ATOMIC, mediaType + "; charset=" + encoding, 0, 0);
            nextReceiver.startContent();
            droppingMetaTags = level;
            seekingHead = false;
            attributes = new AttributeCollectionImpl(getConfiguration());
            nextReceiver.endElement();
        }
        if (!inMetaTag) {
            nextReceiver.startContent();
        }
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        if (inMetaTag) {
            inMetaTag = false;
            // if there was an http-equiv="ContentType" attribute, discard the meta element entirely
            boolean found = false;
            for (int i=0; i<attributes.getLength(); i++) {
                String name = attributes.getLocalName(i);
                if (comparesEqual(name, "http-equiv")) {
                    String value = Whitespace.trim(attributes.getValue(i));
                    if (value.equalsIgnoreCase("Content-Type")) {
                        // case-blind comparison even for XHTML
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                // this was a meta element, but not one of the kind that we discard
                nextReceiver.startElement(metaCode, StandardNames.XS_UNTYPED, 0, 0);
                for (int i=0; i<attributes.getLength(); i++) {
                    int nameCode = attributes.getNameCode(i);
                    int typeCode = attributes.getTypeAnnotation(i);
                    String value = attributes.getValue(i);
                    int locationId = attributes.getLocationId(i);
                    int properties = attributes.getProperties(i);
                    nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
                }
                nextReceiver.startContent();
                nextReceiver.endElement();
            }
        } else {
            level--;
            if (droppingMetaTags == level+1) {
                droppingMetaTags = -1;
            }
            nextReceiver.endElement();
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

