package net.sf.saxon.pattern;
import net.sf.saxon.om.FingerprintedNode;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;

import java.util.HashSet;
import java.util.Set;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A NameTest matches the node kind and the namespace URI and the local
  * name.
  *
  * @author Michael H. Kay
  */

public class NameTest extends NodeTest {

	private int nodeKind;
	private int fingerprint;
    private NamePool namePool;
    private String uri = null;
    private String localName = null;

	public NameTest(int nodeType, int nameCode, NamePool namePool) {
		this.nodeKind = nodeType;
		this.fingerprint = nameCode & 0xfffff;
        this.namePool = namePool;
	}

	/**
	* Create a NameTest for nodes of the same type and name as a given node
	*/

	public NameTest(NodeInfo node) {
		this.nodeKind = node.getNodeKind();
		this.fingerprint = node.getFingerprint();
        this.namePool = node.getNamePool();
	}

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
    * @param nameCode identifies the expanded name of the node to be matched
    */

    public boolean matches(int nodeKind, int nameCode, int annotation) {
        // System.err.println("Matching node " + nameCode + " against " + this.fingerprint);
        // System.err.println("  " + ((nameCode&0xfffff) == this.fingerprint && nodeType == this.nodeType));
        return ((nameCode&0xfffff) == this.fingerprint && nodeKind == this.nodeKind);
        // deliberately in this order for speed (first test usually fails)
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        if (node.getNodeKind() != nodeKind) {
            return false;
        }

        // Two different algorithms are used for name matching. If the fingerprint of the node is readily
        // available, we use it to do an integer comparison. Otherwise, we do string comparisons on the URI
        // and local name. In practice, Saxon's native node implementations use fingerprint matching, while
        // DOM and JDOM nodes use string comparison of names

        if (node instanceof FingerprintedNode) {
            return node.getFingerprint() == fingerprint;
        } else {
            if (uri == null) {
                uri = namePool.getURI(fingerprint);
            }
            if (localName == null) {
                localName = namePool.getLocalName(fingerprint);
            }
            return uri.equals(node.getURI()) && localName.equals(node.getLocalPart());
        }
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
		return fingerprint;
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
        return 1<<nodeKind;
    }

    /**
     * Get the set of node names allowed by this NodeTest. This is returned as a set of Integer fingerprints.
     * A null value indicates that all names are permitted (i.e. that there are no constraints on the node name.
     * The default implementation returns null.
     */

    public Set getRequiredNodeNames() {
        HashSet s = new HashSet(1);
        s.add(new Integer(fingerprint));
        return s;
    }

    public String toString() {
        return toString(namePool);
    }

    public String toString(NamePool pool) {
        switch (nodeKind) {
            case Type.ELEMENT:
                return "element(" + pool.getDisplayName(fingerprint) + ')';
            case Type.ATTRIBUTE:
                return "attribute(" + pool.getDisplayName(fingerprint) + ')';
            case Type.PROCESSING_INSTRUCTION:
                return "processing-instruction(" + pool.getDisplayName(fingerprint) + ')';
            case Type.NAMESPACE:
                return "namespace(" + pool.getDisplayName(fingerprint) + ')';
        }
        return pool.getDisplayName(fingerprint);
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return nodeKind<<20 ^ fingerprint;
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
