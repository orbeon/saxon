package org.orbeon.saxon.pattern;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.tinytree.TinyTree;

/**
  * A DocumentNodeTest implements the test document-node(element(~,~))
  */

// This is messy because the standard interface for a NodeTest does not allow
// any navigation from the node in question - it only tests for the node kind,
// node name, and type annotation of the node.

public class DocumentNodeTest extends NodeTest {


	private NodeTest elementTest;

	public DocumentNodeTest(NodeTest elementTest) {
        this.elementTest = elementTest;
	}

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeKind, int fingerprint, int annotation) {
        throw new UnsupportedOperationException("DocumentNodeTest doesn't support this method");
    }

    /**
     * Test whether this node test is satisfied by a given node on a TinyTree. The node
     * must be a document, element, text, comment, or processing instruction node.
     * This method is provided
     * so that when navigating a TinyTree a node can be rejected without
     * actually instantiating a NodeInfo object.
     *
     * @param tree   the TinyTree containing the node
     * @param nodeNr the number of the node within the TinyTree
     * @return true if the node matches the NodeTest, otherwise false
     */

    public boolean matches(TinyTree tree, int nodeNr) {
        if (tree.getNodeKind(nodeNr) != Type.DOCUMENT) {
            return false;
        }
        return matches(tree.getNode(nodeNr));
    }

    /**
    * Determine whether this Pattern matches the given Node.
    * @param node The NodeInfo representing the Element or other node to be tested against the Pattern
    * uses variables, or contains calls on functions such as document() or key().
    * @return true if the node matches the Pattern, false otherwise
    */

    public boolean matches(NodeInfo node) {
        if (node.getNodeKind() != Type.DOCUMENT) {
            return false;
        }
        AxisIterator iter = node.iterateAxis(Axis.CHILD);
        // The match is true if there is exactly one element node child, no text node
        // children, and the element node matches the element test.
        boolean found = false;
        while (true) {
            NodeInfo n = (NodeInfo)iter.next();
            if (n==null) {
                return found;
            }
            int kind = n.getNodeKind();
            if (kind==Type.TEXT) {
                return false;
            } else if (kind==Type.ELEMENT) {
                if (found) {
                    return false;
                }
                if (elementTest.matches(n)) {
                    found = true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return elementTest.getDefaultPriority();
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return Type.DOCUMENT;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<Type.DOCUMENT;
    }

    /**
     * Get the element test contained within this document test
     * @return the contained element test
     */

    public NodeTest getElementTest() {
        return elementTest;
    }

    public String toString(NamePool pool) {
        return "document-node(" + elementTest.toString(pool) + ')';
    }

    public String toString() {
        return "document-node(" + elementTest.toString() + ')';
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return elementTest.hashCode()^12345;
     }

     public boolean equals(Object other) {
         return other instanceof DocumentNodeTest &&
                 ((DocumentNodeTest)other).elementTest.equals(elementTest);
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
