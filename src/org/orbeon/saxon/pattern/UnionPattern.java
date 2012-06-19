package org.orbeon.saxon.pattern;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/**
 * A pattern formed as the union (or) of two other patterns
 */

public class UnionPattern extends Pattern {

    protected Pattern p1, p2;
    private int nodeType = Type.NODE;

    /**
     * Constructor
     *
     * @param p1 the left-hand operand
     * @param p2 the right-hand operand
     */

    public UnionPattern(Pattern p1, Pattern p2) {
        this.p1 = p1;
        this.p2 = p2;
        if (p1.getNodeKind() == p2.getNodeKind()) {
            nodeType = p1.getNodeKind();
        }
    }


    /**
     * Set the executable containing this pattern
     *
     * @param executable the executable
     */

    public void setExecutable(Executable executable) {
        p1.setExecutable(executable);
        p2.setExecutable(executable);
        super.setExecutable(executable);
    }

    /**
     * Simplify the pattern: perform any context-independent optimisations
     *
     * @param visitor an expression visitor
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {
        p1 = p1.simplify(visitor);
        p2 = p2.simplify(visitor);
        return this;
    }

    /**
     * Type-check the pattern.
     * This is only needed for patterns that contain variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        p1 = p1.analyze(visitor, contextItemType);
        p2 = p2.analyze(visitor, contextItemType);
        return this;
    }

    /**
     * Set the original text
     */

    public void setOriginalText(String pattern) {
        super.setOriginalText(pattern);
        p1.setOriginalText(pattern);
        p2.setOriginalText(pattern);
    }

    public void promote(PromotionOffer offer) throws XPathException {
        p1.promote(offer);
        p2.promote(offer);
    }

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        return p1.replaceSubExpression(original, replacement) ||
                p2.replaceSubExpression(original, replacement);
    }    

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param env      the static context in the XSLT stylesheet
     * @param slotManager
     *@param nextFree the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(StaticContext env, SlotManager slotManager, int nextFree) {
        nextFree = p1.allocateSlots(env, slotManager, nextFree);
        nextFree = p2.allocateSlots(env, slotManager, nextFree);
        return nextFree;
    }

    /**
     * Gather the component (non-union) patterns of this union pattern
     * @param set the set into which the components will be added
     */

    public void gatherComponentPatterns(Set set) {
        if (p1 instanceof UnionPattern) {
            ((UnionPattern)p1).gatherComponentPatterns(set);
        } else {
            set.add(p1);
        }
        if (p2 instanceof UnionPattern) {
            ((UnionPattern)p2).gatherComponentPatterns(set);
        } else {
            set.add(p2);
        }
    }

    /**
     * Determine if the supplied node matches the pattern
     *
     * @param e the node to be compared
     * @return true if the node matches either of the operand patterns
     */

    public boolean matches(NodeInfo e, XPathContext context) throws XPathException {
        return p1.matches(e, context) || p2.matches(e, context);
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Node.NODE
     *
     * @return the type of node matched by this pattern. e.g. Node.ELEMENT or Node.TEXT
     */

    public int getNodeKind() {
        return nodeType;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public NodeTest getNodeTest() {
        if (nodeType == Type.NODE) {
            return AnyNodeTest.getInstance();
        } else {
            return NodeKindTest.makeNodeKindTest(nodeType);
        }
    }


    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     *
     * @return the dependencies, as a bit-significant mask
     */

    public int getDependencies() {
        return p1.getDependencies() | p2.getDependencies();
    }

    /**
     * Iterate over the subexpressions within this pattern
     * @return an iterator over the subexpressions. 
     */

    public Iterator iterateSubExpressions() {
        return new MultiIterator(new Iterator[]{p1.iterateSubExpressions(), p2.iterateSubExpressions()});
    }

    /**
     * Get the LHS of the union
     *
     * @return the first operand of the union
     */

    public Pattern getLHS() {
        return p1;
    }

    /**
     * Get the RHS of the union
     *
     * @return the second operand of the union
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

    /**
     * Determine whether this pattern is the same as another pattern
     * @param other the other object
     */

    public boolean equals(Object other) {
        if (other instanceof UnionPattern) {
            Set s0 = new HashSet(10);
            gatherComponentPatterns(s0);
            Set s1 = new HashSet(10);
            ((UnionPattern)other).gatherComponentPatterns(s1);
            return s0.equals(s1);
        } else {
            return false;
        }
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x9bd723a6 ^ p1.hashCode() ^ p2.hashCode();
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
