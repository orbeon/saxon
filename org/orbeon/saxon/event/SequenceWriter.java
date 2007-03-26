package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;

/**
 * This outputter is used when writing a sequence of atomic values and nodes, for
 * example, when xsl:variable is used with content and an "as" attribute. The outputter
 * builds the sequence; the concrete subclass is responsible for deciding what to do with the
 * resulting items.
 *
 * <p>This class is not used to build temporary trees. For that, the ComplexContentOutputter
 * is used.</p>
 *
 *
 * @author Michael H. Kay
 */

public abstract class SequenceWriter extends SequenceReceiver {
    private Receiver outputter = null;
    private TinyBuilder builder = null;
    private int level = 0;
    private boolean inStartTag = false;

    /**
     * Abstract method to be supplied by subclasses: output one item in the sequence.
     */

    public abstract void write(Item item) throws XPathException;

    /**
     * Determine whether there are any open document or element nodes in the output
     */

    public boolean hasOpenNodes() {
        return level != 0;
    }

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {
        if (outputter==null) {
            createTree();
        }
        if (level++ == 0) {
            outputter.startDocument(properties);
        }
    }

    /**
     * Create a TinyTree to hold a document or element node.
     * @throws org.orbeon.saxon.trans.XPathException
     */

    private void createTree() throws XPathException {
        builder = new TinyBuilder();
        builder.setPipelineConfiguration(getPipelineConfiguration());
        builder.setSystemId(getSystemId());

        NamespaceReducer reducer = new NamespaceReducer();
        reducer.setUnderlyingReceiver(builder);
        reducer.setPipelineConfiguration(getPipelineConfiguration());

        ComplexContentOutputter cco = new ComplexContentOutputter();
        cco.setHostLanguage(getPipelineConfiguration().getHostLanguage());
        cco.setPipelineConfiguration(getPipelineConfiguration());
        cco.setReceiver(reducer);
        outputter = cco;

        outputter.setSystemId(systemId);
        outputter.setPipelineConfiguration(getPipelineConfiguration());
        outputter.open();
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (--level == 0) {
            outputter.endDocument();
            DocumentInfo doc = (DocumentInfo)builder.getCurrentRoot();
            // add the constructed document to the result sequence
            append(doc, 0, NodeInfo.ALL_NAMESPACES);
        }
        previousAtomic = false;
    }

    /**
    * Output an element start tag.
    * @param nameCode The element name code - a code held in the Name Pool
    * @param typeCode Integer code identifying the type of this element. Zero identifies the default
    * type, that is xs:anyType
    * @param properties bit-significant flags indicating any special information
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {

        if (inStartTag) {
            startContent();
        }

        if (outputter==null) {
            createTree();
        }

        outputter.startElement(nameCode, typeCode, locationId, properties);
        level++;
        inStartTag = true;
        previousAtomic = false;
    }

    /**
    * Output an element end tag.
    */

    public void endElement() throws XPathException {
        if (inStartTag) {
            startContent();
        }
        outputter.endElement();
        if (--level == 0) {
            NodeInfo element = builder.getCurrentRoot();
            append(element, 0, NodeInfo.ALL_NAMESPACES);
        }
        previousAtomic = false;
    }

    /**
    * Output a namespace declaration. <br>
    * This is added to a list of pending namespaces for the current start tag.
    * If there is already another declaration of the same prefix, this one is
    * ignored.
    * Note that unlike SAX2 startPrefixMapping(), this call is made AFTER writing the start tag.
    * @param nscode The namespace code
    * @param properties Allows special properties to be passed if required
    * @throws org.orbeon.saxon.trans.XPathException if there is no start tag to write to (created using writeStartTag),
    * or if character content has been written since the start tag was written.
    */

    public void namespace(int nscode, int properties)
    throws XPathException {
        if (level == 0) {
            NamePool namePool = getNamePool();
            Orphan o = new Orphan(getConfiguration());
            o.setNodeKind(Type.NAMESPACE);
            o.setNameCode(namePool.allocate("", "", namePool.getPrefixFromNamespaceCode(nscode)));
            o.setStringValue(namePool.getURIFromNamespaceCode(nscode));
            append(o, 0, NodeInfo.ALL_NAMESPACES);
        } else {
            outputter.namespace(nscode, properties);
        }
        previousAtomic = false;
    }

    /**
    * Output an attribute value. <br>
    * @param nameCode An integer code representing the name of the attribute, as held in the Name Pool
    * @param typeCode Integer code identifying the type annotation of the attribute; zero represents
    * the default type (xs:untypedAtomic)
    * @param value The value of the attribute
    * @param properties Bit significant flags for passing extra information to the serializer, e.g.
    * to disable escaping
    * @throws org.orbeon.saxon.trans.XPathException if there is no start tag to write to (created using writeStartTag),
    * or if character content has been written since the start tag was written.
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNodeKind(Type.ATTRIBUTE);
            o.setNameCode(nameCode);
            o.setStringValue(value);
            o.setTypeAnnotation(typeCode);
            append(o, locationId, NodeInfo.ALL_NAMESPACES);
        } else {
            outputter.attribute(nameCode, typeCode, value, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
    * The startContent() event is notified after all namespaces and attributes of an element
    * have been notified, and before any child nodes are notified.
    * @throws org.orbeon.saxon.trans.XPathException for any failure
    */

    public void startContent() throws XPathException {
        inStartTag = false;
        outputter.startContent();
        previousAtomic = false;
    }

    /**
    * Produce text content output. <BR>
    * @param s The String to be output
    * @param properties bit-significant flags for extra information, e.g. disable-output-escaping
    * @throws org.orbeon.saxon.trans.XPathException for any failure
    */

    public void characters(CharSequence s, int locationId, int properties) throws XPathException {
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNodeKind(Type.TEXT);
            o.setStringValue(s);
            append(o, locationId, NodeInfo.ALL_NAMESPACES);
        } else {
            if (s.length() > 0) {
                if (inStartTag) {
                    startContent();
                }
                outputter.characters(s, locationId, properties);
            }
        }
        previousAtomic = false;
    }

    /**
    * Write a comment.
    */

    public void comment(CharSequence comment, int locationId, int properties) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNodeKind(Type.COMMENT);
            o.setStringValue(comment);
            append(o, locationId, NodeInfo.ALL_NAMESPACES);
        } else {
            outputter.comment(comment, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
    * Write a processing instruction
    * No-op in this implementation
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNameCode(getNamePool().allocate("", "", target));
            o.setNodeKind(Type.PROCESSING_INSTRUCTION);
            o.setStringValue(data);
            append(o, locationId, NodeInfo.ALL_NAMESPACES);
        } else {
            outputter.processingInstruction(target, data, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
    * Close the output
    */

    public void close() throws XPathException {
        previousAtomic = false;
        if (outputter != null) {
            outputter.close();
        }
    }

    /**
    * Append an item to the sequence, performing any necessary type-checking and conversion
    */

    public void append(Item item, int locationId, int copyNamespaces) throws XPathException {

        if (item==null) {
            return;
        }

        if (level==0) {
            write(item);
            previousAtomic = false;
        } else {
            if (item instanceof AtomicValue) {
                // If an atomic value is written to a tree, and the previous item was also
                // an atomic value, then add a single space to separate them
                if (previousAtomic) {
                    outputter.characters(" ", 0, 0);
                }
                outputter.characters(item.getStringValueCS(), 0, 0);
                previousAtomic = true;
            } else {
                ((NodeInfo)item).copy(outputter, NodeInfo.ALL_NAMESPACES, true, locationId);
                previousAtomic = false;
            }
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