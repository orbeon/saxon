package org.orbeon.saxon.pattern;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.tinytree.TinyTree;

/**
* An AnyChildNodePattern is the pattern node(), which matches any node except a root node,
* an attribute node, or a namespace node: in other words, any node that is the child of another
* node.
*/

public final class AnyChildNodePattern extends NodeTest {

    private final static AnyChildNodePattern THE_INSTANCE = new AnyChildNodePattern();

    /**
     * Get the singular instance of this class
     * @return the singular instance
     */

    public static AnyChildNodePattern getInstance() {
        return THE_INSTANCE;
    }

    private AnyChildNodePattern() {}

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeKind, int fingerprint, int annotation) {
        return (nodeKind == Type.ELEMENT ||
                nodeKind == Type.TEXT ||
                nodeKind == Type.COMMENT ||
                nodeKind == Type.PROCESSING_INSTRUCTION);
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
        int nodeKind = tree.nodeKind[nodeNr];
        return (nodeKind == Type.ELEMENT ||
                nodeKind == Type.TEXT ||
                nodeKind == Type.WHITESPACE_TEXT ||
                nodeKind == Type.COMMENT ||
                nodeKind == Type.PROCESSING_INSTRUCTION);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        int nodeKind = node.getNodeKind();
        return (nodeKind == Type.ELEMENT ||
                nodeKind == Type.TEXT ||
                nodeKind == Type.COMMENT ||
                nodeKind == Type.PROCESSING_INSTRUCTION);
    }

    /**
    * Determine the default priority to use if this pattern appears as a match pattern
    * for a template with no explicit priority attribute.
    */

    public double getDefaultPriority() {
        return -0.5;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<Type.ELEMENT | 1<<Type.TEXT | 1<<Type.COMMENT | 1<<Type.PROCESSING_INSTRUCTION;
    }

    public String toString() {
        return "node()";
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return "AnyChildNodePattern".hashCode();
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
