package net.sf.saxon.pattern;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;

/**
* A pattern formed as the union (or) of two other patterns
*/

public class UnionPattern extends Pattern {

    protected Pattern p1, p2;
    private int nodeType = Type.NODE;

    /**
    * Constructor
    * @param p1 the left-hand operand
    * @param p2 the right-hand operand
    */

    public UnionPattern(Pattern p1, Pattern p2) {
        this.p1 = p1;
        this.p2 = p2;
        if (p1.getNodeKind()==p2.getNodeKind()) nodeType = p1.getNodeKind();
    }

    /**
    * Simplify the pattern: perform any context-independent optimisations
    */

    public Pattern simplify(StaticContext env) throws XPathException {
        return new UnionPattern(p1.simplify(env), p2.simplify(env));
    }

    /**
    * Type-check the pattern.
    * This is only needed for patterns that contain variable references or function calls.
    * @return the optimised Pattern
    */

    public Pattern analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        return new UnionPattern(p1.analyze(env, contextItemType), p2.analyze(env, contextItemType));
    }

	/**
	* Set the original text
	*/

	public void setOriginalText(String pattern) {
		super.setOriginalText(pattern);
        p1.setOriginalText(pattern);
        p2.setOriginalText(pattern);
	}

    /**
    * Determine if the supplied node matches the pattern
    * @param e the node to be compared
    * @return true if the node matches either of the operand patterns
    */

    public boolean matches(NodeInfo e, XPathContext context) throws XPathException {
        return p1.matches(e, context) || p2.matches(e, context);
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Node.NODE
    * @return the type of node matched by this pattern. e.g. Node.ELEMENT or Node.TEXT
    */

    public int getNodeKind() {
        return nodeType;
    }

    /**
    * Get a NodeTest that all the nodes matching this pattern must satisfy
    */

    public NodeTest getNodeTest() {
        if (nodeType==Type.NODE) {
            return AnyNodeTest.getInstance();
        } else {
            return NodeKindTest.makeNodeKindTest(nodeType);
        }
    }

    /**
    * Get the LHS of the union
    */

    public Pattern getLHS() {
        return p1;
    }

    /**
    * Get the RHS of the union
    */

    public Pattern getRHS() {
        return p2;
    }

    /**
     * Override method to set the system ID, so it's set on both halves
     */

    public void setSystemId(String systemId) {
        super.setSystemId(systemId);
        p1.setSystemId(systemId);
        p2.setSystemId(systemId);
    }

    /**
     * Override method to set the system ID, so it's set on both halves
     */

    public void setLineNumber(int lineNumber) {
        super.setLineNumber(lineNumber);
        p1.setLineNumber(lineNumber);
        p2.setLineNumber(lineNumber);
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
