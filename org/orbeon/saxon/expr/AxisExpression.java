package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.DocumentNodeTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.StaticError;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;

/**
 * An AxisExpression is always obtained by simplifying a PathExpression.
 * It represents a PathExpression that starts at the context node, and uses
 * a simple node-test with no filters. For example "*", "title", "./item",
 * "@*", or "ancestor::chapter*".
 *
 * <p>An AxisExpression delivers nodes in axis order (not in document order).
 * To get nodes in document order, in the case of a reverse axis, the expression
 * should be wrapped in a Reverser.</p>
*/

public final class AxisExpression extends ComputedExpression {

    private byte axis;
    private NodeTest test;

    /**
    * Constructor
    * @param axis       The axis to be used in this AxisExpression: relevant constants are defined
     *                  in class org.orbeon.saxon.om.Axis.
    * @param nodeTest   The conditions to be satisfied by selected nodes. May be null,
     *                  indicating that any node on the axis is acceptable
     * @see org.orbeon.saxon.om.Axis
    */

    public AxisExpression(byte axis, NodeTest nodeTest) {
        this.axis = axis;
        this.test = nodeTest;
        if (nodeTest instanceof DocumentNodeTest) {
            throw new UnsupportedOperationException("A document-node() test within a path expression is not yet supported");
            // TODO: implement this
        }
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    */

    public Expression simplify(StaticContext env) {

        Expression exp = this;
        if (axis == Axis.PARENT && (test==null || test instanceof AnyNodeTest)) {
            exp = new ParentNodeExpression();
            ExpressionTool.copyLocationInfo(this, exp);
        }

        return exp;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (contextItemType == null) {
            StaticError err = new StaticError("Cannot use an axis here: the context item is undefined");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        if (contextItemType instanceof AtomicType) {
            StaticError err = new StaticError("Cannot use an axis here: the context item is an atomic value");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        return this;
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        if (!(other instanceof AxisExpression)) {
            return false;
        }
        if (axis != ((AxisExpression)other).axis) {
            return false;
        }
        if (test==null) {
            return ((AxisExpression)other).test==null;
        }
        return test.toString().equals(((AxisExpression)other).test.toString());
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        // generate an arbitrary hash code that depends on the axis and the node test
        int h = 9375162 + axis<<20;
        if (test != null) {
            h ^= test.getPrimitiveType()<<16;
            h ^= test.getFingerprint();
        }
        return h;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
    * XPathContext.CURRENT_NODE
    */

    public int getIntrinsicDependencies() {
	    return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return StaticProperty.CONTEXT_DOCUMENT_NODESET |
               (Axis.isForwards[axis] ? StaticProperty.ORDERED_NODESET  : StaticProperty.REVERSE_DOCUMENT_ORDER) |
               (Axis.isPeerAxis[axis] ? StaticProperty.PEER_NODESET : 0) |
               (Axis.isSubtreeAxis[axis] ? StaticProperty.SUBTREE_NODESET : 0) |
               ((axis==Axis.ATTRIBUTE || axis==Axis.NAMESPACE) ? StaticProperty.ATTRIBUTE_NS_NODESET : 0);
    }

    /**
    * Determine the data type of the items returned by this expression
    * @return Type.NODE or a subtype, based on the NodeTest in the axis step
    */

    public final ItemType getItemType() {
        int p = Axis.principalNodeType[axis];
        switch (p) {
        case Type.ATTRIBUTE:
        case Type.NAMESPACE:
            return NodeKindTest.makeNodeKindTest(p);
        default:
            if (test==null) {
                return AnyNodeTest.getInstance();
            } else {
                return NodeKindTest.makeNodeKindTest(test.getPrimitiveType());
            }
        }
    }

    /**
    * Specify that the expression returns a singleton
    */

    public final int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
        // the singleton axes aren't handled by this class
    }

    /**
    * Get the axis
    */

    public byte getAxis() {
        return axis;
    }

    /**
    * Get the NodeTest. Returns null if the AxisExpression can return any node.
    */

    public NodeTest getNodeTest() {
        return test;
    }

    /**
    * Evaluate the path-expression in a given context to return a NodeSet
    * @param context the evaluation context
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item==null) {
            dynamicError("The context item for an axis step is not set", "XP0018", context);
        }
        if (!(item instanceof NodeInfo)) {
            DynamicError err = new DynamicError("The context item for an axis step is not a node");
            err.setErrorCode("XP0020");
            err.setXPathContext(context);
            err.setIsTypeError(true);
            throw err;
        }
        try {
            if (test==null) {
                return ((NodeInfo)item).iterateAxis(axis);
            } else {
                return ((NodeInfo)item).iterateAxis(axis, test);
            }
        } catch (UnsupportedOperationException err) {
            // the namespace axis is not supported for all tree implementations
            dynamicError(err.getMessage(), context);
            return null;
        }
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + Axis.axisName[axis] +
                "::" +
                (test==null ? "node()" : test.toString()));
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
