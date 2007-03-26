package org.orbeon.saxon.pattern;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.tinytree.TinyTree;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A LocalNameTest matches the node type and the local name,
  * it represents an XPath 2.0 test of the form *:name.
  *
  * @author Michael H. Kay
  */

public final class LocalNameTest extends NodeTest {

	private NamePool namePool;
	private int nodeKind;
	private String localName;

	public LocalNameTest(NamePool pool, int nodeKind, String localName) {
	    namePool = pool;
		this.nodeKind = nodeKind;
		this.localName = localName;
	}

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeType The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeType, int fingerprint, int annotation) {
        if (fingerprint == -1) return false;
        if (nodeType != nodeKind) return false;
        return localName.equals(namePool.getLocalName(fingerprint));
    }

    /**
     * Test whether this node test is satisfied by a given node on a TinyTree. The node
     * must be a document, element, text, comment, or processing instruction node.
     * This method is provided so that when navigating a TinyTree a node can be rejected without
     * actually instantiating a NodeInfo object.
     *
     * @param tree   the TinyTree containing the node
     * @param nodeNr the number of the node within the TinyTree
     * @return true if the node matches the NodeTest, otherwise false
     */

    public boolean matches(TinyTree tree, int nodeNr) {
        int fingerprint = tree.getNameCode(nodeNr) & NamePool.FP_MASK;
        if (fingerprint == -1) return false;
        if (tree.getNodeKind(nodeNr) != nodeKind) return false;
        return localName.equals(namePool.getLocalName(fingerprint));
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return localName.equals(node.getLocalPart());
    }


    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.25;
    }

    /**
     * Get the local name used in this LocalNameTest
     */

    public String getLocalName() {
        return localName;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Type.NODE
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return nodeKind;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<nodeKind;
    }

    public String toString() {
        return "*:" + localName;
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return nodeKind<<20 ^ localName.hashCode();
     }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof LocalNameTest &&
                ((LocalNameTest)other).namePool == namePool &&
                ((LocalNameTest)other).nodeKind == nodeKind &&
                ((LocalNameTest)other).localName.equals(localName);
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
