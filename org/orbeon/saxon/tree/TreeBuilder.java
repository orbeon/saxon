package net.sf.saxon.tree;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.om.AttributeCollectionImpl;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import java.util.ArrayList;


/**
  * The Builder class is responsible for taking a stream of SAX events and constructing
  * a Document tree.
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
    private int pendingElement;
    private int pendingLocationId;
    private AttributeCollectionImpl attributes;
    private int[] namespaces;
    private int namespacesUsed;

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
    * Set the Node Factory to use. If none is specified, the Builder uses its own.
    */

    public void setNodeFactory(NodeFactory factory) {
        nodeFactory = factory;
    }

  ////////////////////////////////////////////////////////////////////////////////////////
  // Implement the org.xml.sax.ContentHandler interface.
  ////////////////////////////////////////////////////////////////////////////////////////

    /**
    * Callback interface for SAX: not for application use
    */

    public void open () throws XPathException
    {
        // System.err.println("TreeBuilder: " + this + " Start document depth=" + depth);
        //failed = false;
        started = true;

        DocumentImpl doc;
        if (currentRoot==null) {
            // normal case
            doc = new DocumentImpl();
            currentRoot = doc;
        } else {
            // document node supplied by user
            if (!(currentRoot instanceof DocumentImpl)) {
                throw new DynamicError("Document node supplied is of wrong kind");
            }
            doc = (DocumentImpl)currentRoot;
            if (doc.getFirstChild()!=null) {
                throw new DynamicError("Supplied document is not empty");
            }

        }

        doc.setSystemId(getSystemId());
        doc.setConfiguration(config);
        currentNode = doc;
        depth = 0;
        size[depth] = 0;
        doc.sequence = 0;
        //charBuffer = new StringBuffer(4096);
        //doc.setCharacterBuffer(charBuffer);
        if (lineNumbering) {
            doc.setLineNumbering();
        }
                                                                     
        super.open();
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void close () throws XPathException
    {
        // System.err.println("TreeBuilder: " + this + " End document");
        if (currentNode==null) return;	// can be called twice on an error path
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

        pendingElement = nameCode;
        pendingLocationId = locationId;
        namespaces = null;
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

        if ((properties & ReceiverOptions.DISABLE_ESCAPING) != 0) {
            throw new DynamicError("Cannot disable output escaping when writing a tree");
        }

        if (attributes==null) {
            attributes = new AttributeCollectionImpl(namePool);
        }
//        String attType = "CDATA";
//        if (typeCode == Type.ID || (properties & ReceiverOptions.DTD_ID_ATTRIBUTE) != 0) {
//            attType = "ID";
//        }
        attributes.addAttribute(nameCode, typeCode, value.toString(), locationId, properties);
    }

    public void startContent() throws XPathException {
        // System.err.println("TreeBuilder: " + this + " startContent()");
        if (attributes == null) {
            attributes = emptyAttributeCollection;
        } else {
            attributes.compact();
        }

        if (namespaces == null) {
            namespaces = EMPTY_ARRAY_OF_INT;
        }

        ElementImpl elem = nodeFactory.makeElementNode( currentNode,
                                                        pendingElement,
                                                        attributes,
                                                        namespaces,
                                                        namespacesUsed,
                                                        pipe.getLocationProvider(),
                                                        pendingLocationId,
                                                        nextNodeNumber++);

        namespaces = null;
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

    public void endElement () throws XPathException
    {
        // System.err.println("End element depth=" + depth);
        currentNode.compact(size[depth]);
        depth--;
        currentNode = (ParentNodeImpl)currentNode.getParent();
    }

    /**
    * Notify a text node. Adjacent text nodes must have already been merged
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException
    {
        // System.err.println("Characters: " + chars.toString() + " depth=" + depth);
        if (chars.length()>0) {

            if ((properties & ReceiverOptions.DISABLE_ESCAPING) != 0) {
                throw new DynamicError("Cannot disable output escaping when writing a tree");
            }

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
                AttributeCollectionImpl attlist,
                int[] namespaces,
                int namespacesUsed,
                LocationProvider locator,
                int locationId,
                int sequenceNumber)

        {
            ElementImpl e;
            if (attlist.getLength()==0 && namespacesUsed==0) {
                // for economy, use a simple ElementImpl node
                e = new ElementImpl();
            } else {
                e = new ElementWithAttributes();
                if (namespacesUsed > 0) {
                    ((ElementWithAttributes)e).setNamespaceDeclarations(namespaces, namespacesUsed);
                }
            }
            String baseURI = null;
            int lineNumber = -1;

            if (locator!=null) {
                baseURI = locator.getSystemId(locationId);
                lineNumber = locator.getLineNumber(locationId);
            }

            e.initialise(nameCode, attlist, parent, baseURI, lineNumber, sequenceNumber);
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
