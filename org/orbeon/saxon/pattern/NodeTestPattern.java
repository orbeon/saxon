package org.orbeon.saxon.pattern;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.NodeInfo;

/**
  * A NodeTestPattern is a pattern that consists simply of a NodeTest.
  * @author Michael H. Kay
  */

public class NodeTestPattern extends Pattern {

    private NodeTest nodeTest;

    public NodeTestPattern() {}

    public NodeTestPattern(NodeTest test) {
        nodeTest = test;
    }

    public void setNodeTest(NodeTest test) {
        nodeTest = test;
    }

    /**
    * Determine whether this Pattern matches the given Node. This is the main external interface
    * for matching patterns: it sets current() to the node being tested
    * @param node The NodeInfo representing the Element or other node to be tested against the Pattern
    * @param context The context in which the match is to take place. Only relevant if the pattern
    * uses variables, or contains calls on functions such as document() or key(). Not used (and can be
     * set to null) in the case of patterns that are NodeTests
    * @return true if the node matches the Pattern, false otherwise
    */

    public boolean matches(NodeInfo node, XPathContext context) {
        return nodeTest.matches(node);
    }

    /**
    * Get a NodeTest that all the nodes matching this pattern must satisfy
    */

    public NodeTest getNodeTest() {
        return nodeTest;
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return nodeTest.getDefaultPriority();
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Type.NODE
     *
     * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
     */

    public int getNodeKind() {
        return nodeTest.getPrimitiveType();
    }

    /**
     * Determine the name fingerprint of nodes to which this pattern applies. Used for
     * optimisation.
     *
     * @return A fingerprint that the nodes must match, or -1 if it can match multiple fingerprints
     */

    public int getFingerprint() {
        return nodeTest.getFingerprint();   
    }

    /**
     * Display the pattern for diagnostics
     */

    public String toString() {
        return nodeTest.toString();
    }

    /**
     * Determine whether this pattern is the same as another pattern
     * @param other the other object
     */

    public boolean equals(Object other) {
        return (other instanceof NodeTestPattern) &&
                ((NodeTestPattern)other).nodeTest.equals(nodeTest);
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x7aeffea8 ^ nodeTest.hashCode();
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
