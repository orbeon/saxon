package org.orbeon.saxon.pattern;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.tinytree.TinyTree;
import org.orbeon.saxon.type.Type;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A SubstitutionGroupTest matches element nodes whose name is one of
  * a given set of names: it is used for KindTests of the form schema-element(N) where all
  * elements in a substitution group are to be matched.
  *
  * <p>Note that the SubstitutionGroupTest only tests whether the element name is in the
  * required set of names. It does not test that the content type matches. For this reason,
  * it is always used as part of a CombinedNodeTest that also tests the type annotation.</p>
  *
  * @author Michael H. Kay
  */

public class SubstitutionGroupTest extends NodeTest {

	private int head;
    private IntHashSet group;

    /**
     * Constructor
     * @param head The name of the head element of the substitution group
     * @param group An IntSet containing Integer values representing the fingerprints
     * of element names included in the substitution group
     */

	public SubstitutionGroupTest(int head, IntHashSet group) {
		this.group = group;
        this.head = head;
	}

    /**
     * Constructor
     * @param head The name of the head element of the substitution group
     * @param members An array containing integer values representing the fingerprints
     * of element names included in the substitution group
     */

	public SubstitutionGroupTest(int head, int[] members) {
        //(used from XQuery compiled code)
        group = new IntHashSet(members.length);
        for (int i=0; i<members.length; i++) {
            group.add(members[i]);
        }
        this.head = head;
	}


   /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
    * @param nameCode identifies the expanded name of the node to be matched
    */

    public boolean matches(int nodeKind, int nameCode, int annotation) {
        return nodeKind == Type.ELEMENT &&
                group.contains(nameCode & NamePool.FP_MASK);
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
        return tree.getNodeKind(nodeNr) == Type.ELEMENT &&
                group.contains(tree.getNameCode(nodeNr) & NamePool.FP_MASK);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return node.getNodeKind() == Type.ELEMENT &&
                group.contains(node.getFingerprint());
    }
    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return 0.0;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Type.NODE
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return Type.ELEMENT;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<Type.ELEMENT;
    }

    /**
     * Get the set of node names allowed by this NodeTest. This is returned as a set of Integer fingerprints.
     * A null value indicates that all names are permitted (i.e. that there are no constraints on the node name.
     * The default implementation returns null.
     */

    public IntHashSet getRequiredNodeNames() {
        return group;
    }

    /**
     * Get the fingerprint of the head of the substitution group
     * @return the fingerprint of the head of the substitution group
     */

    public int getHeadFingerprint() {
        return head;
    }

    public String toString(NamePool pool) {
        return "schema-element(" + pool.getDisplayName(head) + ')';
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return head;
     }

    public boolean equals(Object other) {
        return other instanceof SubstitutionGroupTest &&
                ((SubstitutionGroupTest)other).head == head;
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
