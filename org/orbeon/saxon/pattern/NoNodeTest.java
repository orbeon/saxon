package org.orbeon.saxon.pattern;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.ItemType;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A NoNodeTest matches no nodes.
  *
  * @author Michael H. Kay
  */

public final class NoNodeTest extends NodeTest {

    private static NoNodeTest instance = new NoNodeTest();

    /**
    * Get a NoNodeTest instance
    */

    public static NoNodeTest getInstance() {
        return instance;
    }

	public final int getPrimitiveType() {
		return Type.EMPTY;
	}

    /**
     * Test whether a given item conforms to this type
     * @param item The item to be tested
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item) {
        return false;
    }


    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeType The type of node to be matched
    * @param fingerprint identifies the expanded name of the node to be matched
    */

    public boolean matches(int nodeType, int fingerprint, int annotation) {
        return false;
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return false;
    }


    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.5;
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
        return 0;
    }

    public String toString() {
        return "empty()";
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return "NoNodeTest".hashCode();
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
