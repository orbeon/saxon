package net.sf.saxon.event;
import net.sf.saxon.om.*;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceExtent;

import java.util.ArrayList;


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

    private ArrayList list;
    private String systemId;
    private Receiver tree = null;
    private TinyBuilder builder = null;
    private int level = 0;
    private boolean inStartTag = false;

    /**
    * Create a new SequenceOutputter
    */

	public SequenceOutputter() {
	    this.list = new ArrayList(50);
	}

	public SequenceOutputter(int estimatedSize) {
	    this.list = new ArrayList(estimatedSize);
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
     * Determine whether there are any open document or element nodes in the output
     */

    public boolean hasOpenNodes() {
        return level != 0;
    }


    private static final int[] treeSizeParameters = {50, 10, 5, 200};

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {
        if (tree==null) {
            createTree();
        }
        if (level++ == 0) {
            tree.startDocument(properties);
        }
    }

    /**
     * Create a TinyTree to hold a document or element node.
     * @throws XPathException
     */

    private void createTree() throws XPathException {
        builder = new TinyBuilder();
        builder.setPipelineConfiguration(getPipelineConfiguration());
        builder.setSizeParameters(treeSizeParameters);

        NamespaceReducer reducer = new NamespaceReducer();
        reducer.setUnderlyingReceiver(builder);
        reducer.setPipelineConfiguration(getPipelineConfiguration());

        ComplexContentOutputter complex = new ComplexContentOutputter();
        complex.setPipelineConfiguration(getPipelineConfiguration());
        complex.setReceiver(reducer);
        tree = complex;

        tree.setSystemId(systemId);
        tree.setPipelineConfiguration(getPipelineConfiguration());
        tree.open();
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (--level == 0) {
            tree.endDocument();
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

        if (tree==null) {
            createTree();
        }

        tree.startElement(nameCode, typeCode, locationId, properties);
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
        tree.endElement();
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
    * @throws XPathException if there is no start tag to write to (created using writeStartTag),
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
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNodeKind(Type.ATTRIBUTE);
            o.setNameCode(nameCode);
            o.setStringValue(value);
            o.setTypeAnnotation(typeCode);
            append(o, locationId, NodeInfo.ALL_NAMESPACES);
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
        if (s.length() > 0) {
            if (inStartTag) {
                startContent();
            }
            if (level == 0) {
                Orphan o = new Orphan(getConfiguration());
                o.setNodeKind(Type.TEXT);
                o.setStringValue(s);
                append(o, locationId, NodeInfo.ALL_NAMESPACES);
            } else {
                tree.characters(s, locationId, properties);
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
            tree.comment(comment, locationId, properties);
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
            tree.processingInstruction(target, data, locationId, properties);
        }
        previousAtomic = false;
    }

    /**
    * Close the output
    */

    public void close() {
        previousAtomic = false;
    }

    /**
    * Append an item to the sequence, performing any necessary type-checking and conversion
    */

    public void append(Item item, int locationId, int copyNamespaces) throws XPathException {

        if (item==null) {
            return;
        }

        if (level==0) {
            list.add(item);
            previousAtomic = false;
        } else {
            if (item instanceof AtomicValue) {
                // If an atomic value is written to a tree, and the previous item was also
                // an atomic value, then add a single space to separate them
                if (previousAtomic) {
                    tree.characters(" ", 0, 0);
                }
                tree.characters(item.getStringValueCS(), 0, 0);
                previousAtomic = true;
            } else {
                ((NodeInfo)item).copy(tree, NodeInfo.ALL_NAMESPACES, true, locationId);
                previousAtomic = false;
            }
        }
    }

    /**
    * Get the sequence that has been built
    */

    public ValueRepresentation getSequence() {
        switch (list.size()) {
            case 0:
                return EmptySequence.getInstance();
            case 1:
                return (Item)list.get(0);
            default:
                return new SequenceExtent(list);
        }
    }

    /**
     * Get an iterator over the sequence of items that has been constructed
     */

    public SequenceIterator iterate() {
        if (list.size() == 0) {
            return EmptyIterator.getInstance();
        } else {
            return new ListIterator(list);
        }
    }

    /**
     * Get the list containing the sequence of items
     */

    public ArrayList getList() {
        return list;
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

    /**
     * Get the last item in the sequence that has been built, and remove it
     */

    public Item popLastItem() {
        if (list.size() == 0) {
            return null;
        } else {
            return (Item)list.remove(list.size()-1);
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
