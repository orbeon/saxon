package org.orbeon.saxon.event;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.xpath.XPathException;

/**
  * TeeOutputter: a SequenceReceiver that duplicates received events to two different destinations
  */

public class TeeOutputter extends SequenceReceiver {
    
    SequenceReceiver seq1;
    SequenceReceiver seq2;

    public TeeOutputter(SequenceReceiver seq1, SequenceReceiver seq2) {
        this.seq1 = seq1;
        this.seq2 = seq2;
    }

    /**
     * Output an item (atomic value or node) to the sequence
     */

    public void append(Item item, int locationId) throws XPathException {
        seq1.append(item, locationId);
        seq2.append(item, locationId);
    }

    /**
     * Notify the start of a document node
     */

    public void startDocument(int properties) throws XPathException {
        seq1.startDocument(properties);
        seq2.startDocument(properties);
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        seq1.endDocument();
        seq2.endDocument();
    }

    /**
     * Notify the start of an element
     *
     * @param nameCode    integer code identifying the name of the element within the name pool.
     * @param typeCode    integer code identifying the element's type within the name pool. The value -1
     *                    indicates the default type, xdt:untyped.
     * @param locationId  an integer which can be interpreted using a LocationMap to return
     *                    information such as line number and system ID. If no location information is available,
     *                    the value zero is supplied.
     * @param properties  bit-significant properties of the element node. If there are no revelant
     *                    properties, zero is supplied.
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        seq1.startElement(nameCode, typeCode, locationId, properties);
        seq2.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     *
     * @param namespaceCode an integer: the top half is a prefix code, the bottom half a URI code.
     *                      These may be translated into an actual prefix and URI using the name pool. A prefix code of
     *                      zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
     *                      a URI of "", that is, a namespace undeclaration.
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     *                                start tag
     */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        seq1.namespace(namespaceCode, properties);
        seq2.namespace(namespaceCode, properties);
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param locationId an integer which can be interpreted using a LocationMap to return
     *                   information such as line number and system ID. If no location information is available,
     *                   the value zero is supplied.
     * @param properties Bit significant value. The following bits are defined:
     *                   <dt>DISABLE_ESCAPING</dt>    <dd>Disable escaping for this attribute</dd>
     *                   <dt>NO_SPECIAL_CHARACTERS</dt>      <dd>Attribute value contains no special characters</dd>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        seq1.attribute(nameCode, typeCode, value, locationId, properties);
        seq2.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */

    public void startContent() throws XPathException {
        seq1.startContent();
        seq2.startContent();
    }

    /**
     * Notify the end of an element. The receiver must maintain a stack if it needs to know which
     * element is ending.
     */

    public void endElement() throws XPathException {
        seq1.endElement();
        seq2.endElement();
    }

    /**
     * Notify character data. Note that some receivers may require the character data to be
     * sent in a single event, but in general this is not a requirement.
     *
     * @param chars      The characters
     * @param locationId an integer which can be interpreted using a LocationMap to return
     *                   information such as line number and system ID. If no location information is available,
     *                   the value zero is supplied.
     * @param properties Bit significant value. The following bits are defined:
     *                   <dt>DISABLE_ESCAPING</dt>           <dd>Disable escaping for this text node</dd>
     *                   <dt>USE_CDATA</dt>                  <dd>Output as a CDATA section</dd>
     *                   <dt>NO_SPECIAL_CHARACTERS</dt>      <dd>Value contains no special characters</dd>
     *                   <dt>WHITESPACE</dt>                 <dd>Text is all whitespace</dd>
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        seq1.characters(chars, locationId, properties);
        seq2.characters(chars, locationId, properties);
    }

    /**
     * Output a processing instruction
     *
     * @param name       The PI name. This must be a legal name (it will not be checked).
     * @param data       The data portion of the processing instruction
     * @param locationId an integer which can be interpreted using a LocationMap to return
     *                   information such as line number and system ID. If no location information is available,
     *                   the value zero is supplied.
     * @param properties Additional information about the PI. The following bits are
     *                   defined:
     *                   <dt>CHECKED</dt>    <dd>Data is known to be legal (e.g. doesn't contain "?>")</dd>
     * @throws IllegalArgumentException: the content is invalid for an XML processing instruction
     */

    public void processingInstruction(String name, CharSequence data, int locationId, int properties) throws XPathException {
        seq1.processingInstruction(name, data, locationId, properties);
        seq2.processingInstruction(name, data, locationId, properties);
    }

    /**
     * Notify a comment. Comments are only notified if they are outside the DTD.
     *
     * @param content    The content of the comment
     * @param locationId an integer which can be interpreted using a LocationMap to return
     *                   information such as line number and system ID. If no location information is available,
     *                   the value zero is supplied.
     * @param properties Additional information about the comment. The following bits are
     *                   defined:
     *                   <dt>CHECKED</dt>    <dd>Comment is known to be legal (e.g. doesn't contain "--")</dd>
     * @throws IllegalArgumentException: the content is invalid for an XML comment
     */

    public void comment(CharSequence content, int locationId, int properties) throws XPathException {
        seq1.comment(content, locationId, properties);
        seq2.comment(content, locationId, properties);
    }

    /**
     * Notify the end of the event stream
     */

    public void close() throws XPathException {
        seq1.close();
        seq2.close();
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
