package net.sf.saxon.pattern;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;

import java.util.Set;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A SubstitutionGroupTest matches element nodes whose name is one of
  * a given set of names: it is used for KindTests of the form element(N) where all
  * elements in a substitution group are to be matched.
  *
  * @author Michael H. Kay
  */

public class SubstitutionGroupTest extends NodeTest {

	private Set group;
    //private NamePool namePool;

    /**
     * Constructor
     * @param group A HashSet containing Integer values representing the fingerprints
     * of element names included in the substitution group
     */

	public SubstitutionGroupTest(Set group, NamePool namePool) {
		this.group = group;
        //this.namePool = namePool;
	}

   /**
    * Test whether this node test is satisfied by a given node
    * @param nodeType The type of node to be matched
    * @param nameCode identifies the expanded name of the node to be matched
    */

    public boolean matches(int nodeType, int nameCode, int annotation) {
        return nodeType == Type.ELEMENT &&
                group.contains(new Integer(nameCode & 0xfffff));
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return node.getNodeKind() == Type.ELEMENT &&
                group.contains(new Integer(node.getFingerprint()));
    }
    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return 0.0;
    }

	/**
	* Get the fingerprint required
	*/

	public int getFingerprint() {
		return -1;
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
     * Indicate whether this NodeTest is capable of matching text nodes
     */

    public boolean allowsTextNodes() {
        return false;
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

    public Set getRequiredNodeNames() {
        return group;
    }

    public String toString() {
        return "element(NAME)";
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return 0;
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
