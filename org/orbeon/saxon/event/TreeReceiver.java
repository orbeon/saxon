package org.orbeon.saxon.event;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.XPathException;

/**
 * A TreeReceiver acts as a bridge between a SequenceReceiver, which can receive
 * events for constructing any kind of sequence, and an ordinary Receiver, which
 * only handles events relating to the building of trees. To do this, it has to
 * process any items added to the sequence using the append() interface; all other
 * events are passed through unchanged.
 */

public class TreeReceiver extends SequenceReceiver {
    private Receiver baseReceiver;
    private Configuration config;
    private String systemId;

    public TreeReceiver(Receiver nextInChain) {
        baseReceiver = nextInChain;
        previousAtomic = false;
        locator = nextInChain.getDocumentLocator();
    }

    public void setSystemId(String systemId) {
        if (systemId != this.systemId) {
            this.systemId = systemId;
            if (baseReceiver != null) {
                baseReceiver.setSystemId(systemId);
            }
        }
    }

    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the underlying Receiver (that is, the next one in the pipeline)
     */

    public Receiver getUnderlyingReceiver() {
        return baseReceiver;
    }


    /**
     * Set the configuration to be used (providing access to a NamePool)
     */

    public void setConfiguration(Configuration config) {
        if (this.config != config) {
            this.config = config;
            if (baseReceiver != null) {
                baseReceiver.setConfiguration(config);
            }
        }
    }

    /**
     * Get the configuration to be used (providing access to a NamePool)
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the Document Locator
     */

    public void setDocumentLocator(LocationProvider locator) {
        this.locator = locator;
        if (baseReceiver != null) {
            baseReceiver.setDocumentLocator(locator);
        }
    }

    /**
     * Start of document
     */

    public void open() throws XPathException {
        if (baseReceiver == null) {
            throw new IllegalStateException("TreeReceiver.open(): no underlying receiver provided");
        }
        baseReceiver.open();
        previousAtomic = false;
    }

    /**
     * End of document
     */

    public void close() throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.close();
        }
        previousAtomic = false;
    }

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.startDocument(properties);
        }
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.endDocument();
        }
    }

    /**
     * Notify the start of an element
     * @param nameCode integer code identifying the name of the element within the name pool.
     * @param typeCode integer code identifying the element's type within the name pool.
     * @param properties: for future use. Should be set to zero.
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.startElement(nameCode, typeCode, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     * @param namespaceCode an integer: the top half is a prefix code, the bottom half a URI code.
     * These may be translated into an actual prefix and URI using the name pool. A prefix code of
     * zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
     * a URI of "", that is, a namespace undeclaration.
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     * start tag
     */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.namespace(namespaceCode, properties);
        }
        previousAtomic = false;
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     * @param nameCode The name of the attribute, as held in the name pool
     * @param typeCode The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *        <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *        <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     * start tag
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.attribute(nameCode, typeCode, value, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.startContent();
        }
        previousAtomic = false;
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.endElement();
        }
        previousAtomic = false;
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.characters(chars, locationId, properties);
        }
        previousAtomic = false;
    }


    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.processingInstruction(target, data, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.comment(chars, locationId, properties);
        }
        previousAtomic = false;
    }


    /**
     * Set the URI for an unparsed entity in the document.
     */

    public void setUnparsedEntity(String name, String uri, String publicId) throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.setUnparsedEntity(name, uri, publicId);
        }
    }

    /**
     * Append an arbitrary item (node or atomic value) to the output
     */

    public void append(Item item, int locationId) throws XPathException {
        if (item instanceof AtomicValue) {
            if (previousAtomic) {
                characters(" ", locationId, 0);
            }
            characters(item.getStringValue(), locationId, 0);
            previousAtomic = true;
        } else if (item instanceof DocumentInfo) {
            SequenceIterator iter = ((DocumentInfo)item).iterateAxis(Axis.CHILD);
            while (true) {
                Item it = iter.next();
                if (it == null) break;
                append(it, locationId);
            }
        } else {
            ((NodeInfo)item).copy(this, NodeInfo.ALL_NAMESPACES, true, locationId);
            previousAtomic = false;
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
// Contributor(s): none.
//
