package org.orbeon.saxon.event;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.tinytree.TinyDocumentImpl;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.xpath.XPathException;

import java.util.ArrayList;
import java.util.List;


/**
 * This outputter is used when writing a sequence of atomic values and nodes, that
 * is, when xsl:variable is used with content and an "as" attribute. The outputter
 * builds the sequence and provides access to it. (It isn't really an outputter at all,
 * it doesn't pass the events to anyone, it merely constructs the sequence in memory
 * and provides access to it). Note that the event sequence can include calls such as
 * startElement and endElement that require trees to be built. If nodes such as attributes
 * and text nodes are received while an element is being constructed, the nodes are added
 * to the tree. Otherwise, "orphan" nodes (nodes with no parent) are created and added
 * directly to the sequence.
 *
 * <p>This class is not used to build temporary trees. For that, the ComplexContentOutputter
 * is used.</p>
 *
 *
 * @author Michael H. Kay
 */

public final class SequenceOutputter extends SequenceReceiver {

    private List list;
    private Configuration config;
    private NamePool namePool;
    private String systemId;
    private Receiver tree = null;
    private TinyBuilder builder = null;
    private int elementLevel = 0;
    private int documentLevel = 0;
    private boolean inStartTag = false;

    /**
    * Create a new SequenceOutputter
    */

	public SequenceOutputter() {
	    //System.err.println("new SequenceOutputter");
	    this.list = new ArrayList();
	}

    /**
    * Set the name pool. This method must be called before startDocument() is called.
    * @param config the configuration. The name pool held by this configuration
    * must contain all the name codes that are passed across this interface.
    */

    public void setConfiguration(Configuration config) {
        this.config = config;
        namePool = config.getNamePool();
    }

    /**
    * Get the name pool
    * @return the Name Pool that was supplied using the setConfiguration() method
    */

    public NamePool getNamePool() {
        return namePool;
    }

    public Configuration getConfiguration() {
        return config;
    }

    /**
    * Set the system ID
    * @param systemId the URI used to identify the tree being passed across this interface
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Get the system ID
    * @return the system ID that was supplied using the setSystemId() method
    */

    public String getSystemId() {
        return systemId;
    }

    /**
    * Notify the start of the document. This event is notified once, before any other events.
    * This implementation does nothing.
    */

    public final void open() throws XPathException {
    }

    private static final int[] treeSizeParameters = {50, 10, 5, 200};

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {
        if (tree==null) {
            builder = new TinyBuilder();
            builder.setConfiguration(config);
            builder.setSizeParameters(treeSizeParameters);

            NamespaceReducer reducer = new NamespaceReducer();
            reducer.setUnderlyingReceiver(builder);
            reducer.setConfiguration(config);

            ComplexContentOutputter complex = new ComplexContentOutputter();
            complex.setConfiguration(config);
            complex.setReceiver(reducer);
            complex.setDocumentLocator(locator);
            tree = complex;

            tree.setSystemId(systemId);
            tree.setConfiguration(config);
            tree.open();
            tree.startDocument(properties);
        } else {
            // ignore the event, other than to make a note that the corresponding endDocument should also
            // be ignored
        }
        documentLevel++;

    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        documentLevel--;
        if (documentLevel==0) {
            tree.endDocument();
            tree.close();
            DocumentInfo doc = builder.getCurrentDocument();
            tree = null;
            builder = null;
            // add the constructed document to the result sequence
            append(doc, 0);
        }
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

        if (tree==null) {

            documentLevel = 0;
            builder = new TinyBuilder();
            builder.setConfiguration(config);
            builder.setSizeParameters(treeSizeParameters);
            builder.setDocumentLocator(locator);

            NamespaceReducer reducer = new NamespaceReducer();
            reducer.setUnderlyingReceiver(builder);
            reducer.setConfiguration(config);
            reducer.setDocumentLocator(locator);

            ComplexContentOutputter complex = new ComplexContentOutputter();
            complex.setConfiguration(config);
            complex.setReceiver(reducer);
            complex.setDocumentLocator(locator);
            tree = complex;

            tree.setSystemId(systemId);
            tree.setConfiguration(config);
            tree.open();
            tree.startElement(nameCode, typeCode, locationId, properties);
        } else {
            tree.startElement(nameCode, typeCode, locationId, properties);
        }
        elementLevel++;
        //System.err.println("level = " + level);
        inStartTag = true;
        previousAtomic = false;
    }

    /**
    * Output an element end tag.
    */

    public void endElement() throws XPathException {
        if (inStartTag) startContent();
        elementLevel--;
        //System.err.println("level = " + level);
        if (elementLevel==0 && documentLevel==0) {
            tree.endElement();
            tree.close();
            DocumentInfo doc = builder.getCurrentDocument();
            tree = null;
            builder = null;
            SequenceIterator iter = doc.iterateAxis(Axis.CHILD);
            //if (iter.hasNext()) {
                // always true
            NodeInfo element = (NodeInfo)iter.next();
            // mark the element as the effective root of the tree
            ((TinyDocumentImpl)doc).setRootNode(element);
            // add the constructed element to the result sequence
            append(element, 0);
            //}
        } else {
            tree.endElement();
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
    * @throws XPathException if there is no start tag to write to (created using writeStartTag),
    * or if character content has been written since the start tag was written.
    */

    public void namespace(int nscode, int properties)
    throws XPathException {
        if (tree == null) {
            Orphan o = new Orphan(namePool);
            o.setNodeKind(Type.NAMESPACE);
            o.setNameCode(namePool.allocate("", "", namePool.getPrefixFromNamespaceCode(nscode)));
            o.setStringValue(namePool.getURIFromNamespaceCode(nscode));
            append(o, 0);
        } else {
            tree.namespace(nscode, properties);
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
    * @throws XPathException if there is no start tag to write to (created using writeStartTag),
    * or if character content has been written since the start tag was written.
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        //System.err.println("Attribute " + value + " tree=" + tree + " namePool =" + namePool );
        if (tree == null) {
            Orphan o = new Orphan(namePool);
            o.setNodeKind(Type.ATTRIBUTE);
            o.setNameCode(nameCode);
            o.setStringValue(value);
            o.setTypeAnnotation(typeCode);
            append(o, locationId);
        } else {
            tree.attribute(nameCode, typeCode, value, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
    * The startContent() event is notified after all namespaces and attributes of an element
    * have been notified, and before any child nodes are notified.
    * @throws XPathException for any failure
    */

    public void startContent() throws XPathException {
        inStartTag = false;
        tree.startContent();
        previousAtomic = false;
    }

    /**
    * Produce text content output. <BR>
    * @param s The String to be output
    * @param properties bit-significant flags for extra information, e.g. disable-output-escaping
    * @throws XPathException for any failure
    */

    public void characters(CharSequence s, int locationId, int properties) throws XPathException {
        //System.err.println("Characters " + s + " tree=" + tree);
        if (inStartTag) {
            startContent();
        }
        if (tree == null) {
            Orphan o = new Orphan(namePool);
            o.setNodeKind(Type.TEXT);
            o.setStringValue(s);
            append(o, locationId);
        } else {
            tree.characters(s, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
    * Write a comment.
    */

    public void comment(CharSequence comment, int locationId, int properties) throws XPathException {
        if (inStartTag) startContent();
        if (tree == null) {
            Orphan o = new Orphan(namePool);
            o.setNodeKind(Type.COMMENT);
            o.setStringValue(comment);
            append(o, locationId);
        } else {
            tree.comment(comment, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
    * Write a processing instruction
    * No-op in this implementation
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (inStartTag) startContent();
        if (tree == null) {
            Orphan o = new Orphan(namePool);
            o.setNameCode(namePool.allocate("", "", target));
            o.setNodeKind(Type.PROCESSING_INSTRUCTION);
            o.setStringValue(data);
            append(o, locationId);
        } else {
            tree.processingInstruction(target, data, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
    * Close the output
    */

    public void close() throws XPathException {
        previousAtomic = false;
    }

    /**
    * Append an item to the sequence, performing any necessary type-checking and conversion
    */

    public void append(Item item, int locationId) throws XPathException {

        if (item==null) {
            return;
        }

        // If an atomic value is written to a tree, and the previous item was also
        // an atomic value, then add a single space to separate them

        if (tree!=null && previousAtomic && item instanceof AtomicValue) {
            tree.characters(" ", 0, 0);
        }
        previousAtomic = (item instanceof AtomicValue);

        if (tree==null) {
            list.add(item);
        } else {
            if (item instanceof AtomicValue) {
                tree.characters(item.getStringValue(), 0, 0);
            } else {
                ((NodeInfo)item).copy(tree, NodeInfo.ALL_NAMESPACES, true, locationId);
            }
        }
    }

    /**
    * Get the sequence that has been built
    */

    public Value getSequence() {
        switch (list.size()) {
            case 0:
                return EmptySequence.getInstance();
            case 1:
                Item item = (Item)list.get(0);
                return Value.asValue(item);
            default:
                return new SequenceExtent(list);
        }
    }

    /**
     * Get the first item in the sequence that has been built
     */

    public Item getFirstItem() {
        if (list.size() == 0) {
            return null;
        } else {
            return (Item)list.get(0);
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
