package org.orbeon.saxon.pattern;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A NamespaceTest matches the node type and the namespace URI.
  *
  * @author Michael H. Kay
  */

public final class NamespaceTest extends NodeTest {

	private NamePool namePool;
	private int type;
	private short uriCode;
    private String uri;

	public NamespaceTest(NamePool pool, int nodeType, String uri) {
	    namePool = pool;
		type = nodeType;
        this.uri = uri;
		this.uriCode = pool.allocateCodeForURI(uri);
	}

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeType The type of node to be matched
    * @param fingerprint identifies the expanded name of the node to be matched
    */

    public boolean matches(int nodeType, int fingerprint, int annotation) {
        if (fingerprint == -1) return false;
        if (nodeType != type) return false;
        return uriCode == namePool.getURICode(fingerprint);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return node.getNodeKind()==type && node.getURI().equals(uri);
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.25;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Type.NODE
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return type;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<type;
    }

    public String toString() {
        return "{" + namePool.getURIFromURICode(uriCode) + "}:*";
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return uriCode << 5 + type;
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
