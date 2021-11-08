package org.orbeon.saxon.tree;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.om.AttributeCollectionImpl;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;

import java.util.ArrayList;


/**
  * The TreeBuilder class is responsible for taking a stream of Receiver events and constructing
  * a Document tree using the linked tree implementation.
  * @author Michael H. Kay
  */

public class TreeBuilder extends Builder

{
    private static AttributeCollectionImpl emptyAttributeCollection =
    				new AttributeCollectionImpl(null);

    private ParentNodeImpl currentNode;

    private NodeFactory nodeFactory;
    private int[] size = new int[100];          // stack of number of children for each open node
    private int depth = 0;
    private ArrayList arrays = new ArrayList(20);       // reusable arrays for creating nodes
    private int elementNameCode;
    private int elementTypeCode;
    private int pendingLocationId;
    private AttributeCollectionImpl attributes;
    private int[] namespaces;
    private int namespacesUsed;
    private boolean allocateSequenceNumbers = true;
    private int nextNodeNumber = 1;
    private static final int[] EMPTY_ARRAY_OF_INT = new int[0];

    /**
    * create a Builder and initialise variables
    */

    public TreeBuilder() {
        nodeFactory = new DefaultNodeFactory();
        // System.err.println("new TreeBuilder " + this);
    }

    /**
     * Get the current root node. This will normally be a document node, but if the root of the tree
     * is an element node, it can be an element.
     * @return the root of the tree that is currently being built, or that has been most recently built
     *         using this builder
     */

    public NodeInfo getCurrentRoot() {
        NodeInfo physicalRoot = currentRoot;
        if (physicalRoot instanceof DocumentImpl && ((DocumentImpl)physicalRoot).isImaginary()) {
            return ((DocumentImpl)physicalRoot).getDocumentElement();
        } else {
            return physicalRoot;
        }
    }

    public void reset() {
        super.reset();
        currentNode = null;
        nodeFactory = null;
        depth = 0;
        allocateSequenceNumbers = true;
        nextNodeNumber = 1;
    }

    /**
     * Set whether the builder should allocate sequence numbers to elements as they are added to the
     * tree. This is normally done, because it provides a quick way of comparing document order. But
     * nodes added using XQuery update are not sequence-numbered.
     * @param allocate true if sequence numbers are to be allocated
     */

    public void setAllocateSequenceNumbers(boolean allocate) {
        allocateSequenceNumbers = allocate;
    }

    /**
     * Set the Node Factory to use. If none is specified, the Builder uses its own.
     * @param factory the node factory to be used. This allows custom objects to be used to represent
     * the elements in the tree.
     */

    public void setNodeFactory(NodeFactory factory) {
        nodeFactory = factory;
    }

    /**
    * Open the stream of Receiver events
    */

    public void open () throws XPathException {
        started = true;
        depth = 0;
        size[depth] = 0;
        super.open();
    }


    /**
     * Start of a document node.
     * This event is ignored: we simply add the contained elements to the current document
     */

    public void startDocument(int properties) throws XPathException {
        DocumentImpl doc = new DocumentImpl();
        currentRoot = doc;
        doc.setSystemId(getSystemId());
        doc.setBaseURI(getBaseURI());
        doc.setConfiguration(config);
        currentNode = doc;
        depth = 0;
        size[depth] = 0;
        doc.sequence = 0;
        if (lineNumbering) {
            doc.setLineNumbering();
        }
    }

    /**
     * Notify the end of the document
     */

     public void endDocument() throws XPathException {
         // System.err.println("End document depth=" + depth);
         currentNode.compact(size[depth]);
     }



    /**
    * Close the stream of Receiver events
    */

    public void close () throws XPathException {
        // System.err.println("TreeBuilder: " + this + " End document");
        if (currentNode==null) {
            return;	// can be called twice on an error path
        }
        currentNode.compact(size[depth]);
        currentNode = null;

        // we're not going to use this Builder again so give the garbage collector
        // something to play with
        arrays = null;

        super.close();
        nodeFactory = null;
    }

    /**
    * Notify the start of an element
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        // System.err.println("TreeBuilder: " + this + " Start element depth=" + depth);
        if (currentNode == null) {
            startDocument(0);
            ((DocumentImpl)currentRoot).setImaginary(true);
        }
        elementNameCode = nameCode;
        elementTypeCode = typeCode;
        pendingLocationId = locationId;
        namespacesUsed = 0;
        attributes = null;
    }

    public void namespace (int namespaceCode, int properties) {
        if (namespaces==null) {
            namespaces = new int[5];
        }
        if (namespacesUsed == namespaces.length) {
            int[] ns2 = new int[namespaces.length * 2];
            System.arraycopy(namespaces, 0, ns2, 0, namespacesUsed);
            namespaces = ns2;
        }
        namespaces[namespacesUsed++] = namespaceCode;
    }

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        properties &= ~ReceiverOptions.DISABLE_ESCAPING;

        if (attributes==null) {
            attributes = new AttributeCollectionImpl(config);
        }
        attributes.addAttribute(nameCode, typeCode, value.toString(), locationId, properties);
    }

    public void startContent() throws XPathException {
        // System.err.println("TreeBuilder: " + this + " startContent()");
        if (attributes == null) {
            attributes = emptyAttributeCollection;
        } else {
            attributes.compact();
        }

        int[] nslist = namespaces;
        if (nslist == null || namespacesUsed == 0) {
            nslist = EMPTY_ARRAY_OF_INT;
        }

        ElementImpl elem = nodeFactory.makeElementNode( 
                currentNode, elementNameCode, elementTypeCode,
                attributes, nslist, namespacesUsed,
                pipe.getLocationProvider(),
                pendingLocationId, (allocateSequenceNumbers ? nextNodeNumber++ : -1));

        namespacesUsed = 0;
        attributes = null;

        // the initial array used for pointing to children will be discarded when the exact number
        // of children in known. Therefore, it can be reused. So we allocate an initial array from
        // a pool of reusable arrays. A nesting depth of >20 is so rare that we don't bother.

        while (depth >= arrays.size()) {
            arrays.add(new NodeImpl[20]);
        }
        elem.useChildrenArray((NodeImpl[])arrays.get(depth));

        currentNode.addChild(elem, size[depth]++);
        if (depth >= size.length - 1) {
            int[] newsize = new int[size.length * 2];
            System.arraycopy(size, 0, newsize, 0, size.length);
            size = newsize;
        }
        size[++depth] = 0;
        namespacesUsed = 0;

    	if (currentNode instanceof DocumentInfo) {
    	    ((DocumentImpl)currentNode).setDocumentElement(elem);
    	}

        currentNode = elem;
    }

    /**
    * Notify the end of an element
    */

    public void endElement () throws XPathException {
        // System.err.println("End element depth=" + depth);
        currentNode.compact(size[depth]);
        depth--;
        currentNode = (ParentNodeImpl)currentNode.getParent();
    }

    /**
    * Notify a text node. Adjacent text nodes must have already been merged
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException {
        // System.err.println("Characters: " + chars.toString() + " depth=" + depth);
        if (chars.length()>0) {
			// we rely on adjacent chunks of text having already been merged
            //TextImpl n = new TextImpl(currentNode, bufferStart, length);
			TextImpl n = new TextImpl(currentNode, chars.toString());
            currentNode.addChild(n, size[depth]++);
        }
    }

    /**
    * Notify a processing instruction
    */

    public void processingInstruction (String name, CharSequence remainder, int locationId, int properties)
    {
    	int nameCode = namePool.allocate("", "", name);
        ProcInstImpl pi = new ProcInstImpl(nameCode, remainder.toString());
        currentNode.addChild(pi, size[depth]++);
        LocationProvider locator = pipe.getLocationProvider();
        if (locator!=null) {
            pi.setLocation(locator.getSystemId(locationId),
                           locator.getLineNumber(locationId));
        }
    }

    /**
    * Notify a comment
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException {
        CommentImpl comment = new CommentImpl(chars.toString());
        currentNode.addChild(comment, size[depth]++);
    }


    /**
     * graftElement() allows an element node to be transferred from one tree to another.
     * This is a dangerous internal interface which is used only to contruct a stylesheet
     * tree from a stylesheet using the "literal result element as stylesheet" syntax.
     * The supplied element is grafted onto the current element as its only child.
     * @param element the element to be grafted in as a new child.
    */

    public void graftElement(ElementImpl element) throws XPathException {
        currentNode.addChild(element, size[depth]++);
    }

    /**
    * Set an unparsed entity URI for the document
    */

    public void setUnparsedEntity(String name, String uri, String publicId) {
        ((DocumentImpl)currentRoot).setUnparsedEntity(name, uri, publicId);
    }


    //////////////////////////////////////////////////////////////////////////////
    // Inner class DefaultNodeFactory. This creates the nodes in the tree.
    // It can be overridden, e.g. when building the stylesheet tree
    //////////////////////////////////////////////////////////////////////////////

    private static class DefaultNodeFactory implements NodeFactory {

        public ElementImpl makeElementNode(
                NodeInfo parent,
                int nameCode,
                int typeCode, AttributeCollectionImpl attlist,
                int[] namespaces,
                int namespacesUsed,
                LocationProvider locator,
                int locationId,
                int sequenceNumber)

        {
            ElementImpl e = new ElementImpl();
            if (namespacesUsed > 0) {
                e.setNamespaceDeclarations(namespaces, namespacesUsed);
            }
//            String baseURI = null;
//            int lineNumber = -1;
//
//            if (locator!=null) {
//                baseURI = locator.getSystemId(locationId);
//                lineNumber = locator.getLineNumber(locationId);
//            }

            e.initialise(nameCode, typeCode, attlist, parent, sequenceNumber);
            if (locator!=null) {
                String baseURI = locator.getSystemId(locationId);
                int lineNumber = locator.getLineNumber(locationId);
                int columnNumber = locator.getColumnNumber(locationId);
                e.setLocation(baseURI, lineNumber, columnNumber);
            }
            return e;
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
