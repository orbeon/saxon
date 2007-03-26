package org.orbeon.saxon.pattern;
import org.orbeon.saxon.om.ExtendedNodeInfo;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.tinytree.TinyTree;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;

/**
 * NodeTest is an interface that enables a test of whether a node matches particular
 * conditions. IdrefTest is a test that cannot be represented directly in XPath or
 * XSLT patterns, but which is used internally for matching IDREF nodes: it tests
 * whether the node has the is-idref property
 * <p>
 * Beware: The TypeHierarchy.computeRelationship() method does not work with this kind of ItemType.
 * </p>
  *
  * @author Michael H. Kay
  */

public class IdrefTest extends NodeTest {

	private int kind;          // element or attribute

    /**
     * Create a IdrefTest
     * @param nodeKind the kind of nodes to be matched: always elements or attributes
     */

	public IdrefTest(int nodeKind) {
		this.kind = nodeKind;
	}

    public ItemType getSuperType(TypeHierarchy th) {
        return NodeKindTest.makeNodeKindTest(kind);
    }

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     * @param annotation The actual content type of the node
     */

    public boolean matches(int nodeKind, int fingerprint, int annotation) {
        // used only while evaluating path expressions, so not applicable
        return false;
    }

    /**
     * Test whether this node test is satisfied by a given node on a TinyTree. The node
     * must be a document, element, text, comment, or processing instruction node.
     * This method is provided
     * so that when navigating a TinyTree a node can be rejected without
     * actually instantiating a NodeInfo object.
     *
     * @param tree   the TinyTree containing the node
     * @param nodeNr the number of the node within the TinyTree (never an attribute)
     * @return true if the node matches the NodeTest, otherwise false
     */

    public boolean matches(TinyTree tree, int nodeNr) {
        if (tree.getNodeKind(nodeNr) != kind) {
            return false;
        }
        return tree.isIdrefElement(nodeNr);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        if (node instanceof ExtendedNodeInfo) {
            return ((ExtendedNodeInfo)node).isIdref();
        }
        return false;
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return 0;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return kind;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<kind;
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized (assuming that atomization succeeds)
     */

    public AtomicType getAtomizedItemType() {
        return Type.ANY_ATOMIC_TYPE;
    }

    public String toString() {
        return (kind == Type.ELEMENT ? "is-idref(element)" : "is-idref(attribute)");
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return kind<<20 ^ 836283;
     }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof IdrefTest &&
                ((IdrefTest)other).kind == kind;
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
