package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.sort.GlobalOrderComparer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.SequenceType;


/**
* IdentityComparison: a boolean expression that compares two nodes
* for equals, not-equals, greater-than or less-than based on identity and
* document ordering
*/

public final class IdentityComparison extends BinaryExpression {

    private boolean generateIdEmulation = false;
        // this flag is set if an "X is Y" or "X isnot Y" comparison is being used
        // to emulate generate-id(X) = / != generate-id(Y). The handling of an empty
        // sequence in the two cases is different.

    /**
    * Create an identity comparison identifying the two operands and the operator
    * @param p1 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
    * @param p2 the right-hand operand
    */

    public IdentityComparison(Expression p1, int op, Expression p2) {
        super(p1, op, p2);
    }

    /**
     * Set flag to indicate different empty-sequence behavior when emulating
     * comparison of two generate-id's
     * @param flag true if this function is being used to compare generate-id() output
     */

    public void setGenerateIdEmulation(boolean flag) {
        generateIdEmulation = flag;
    }

    /**
     * Test the flag that indicates different empty-sequence behavior when emulating
     * comparison of two generate-id's
     * @return true if this function is being used to compare generate-id() output
     */

    public boolean isGenerateIdEmulation() {
        return generateIdEmulation;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);

        if (!generateIdEmulation) {
            if (Literal.isEmptySequence(operand0) || Literal.isEmptySequence(operand1)) {
                return Literal.makeEmptySequence();
            }
        }

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(
                operand0, SequenceType.OPTIONAL_NODE, false, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(
                operand1, SequenceType.OPTIONAL_NODE, false, role1, visitor);
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression r = super.optimize(visitor, contextItemType);
        if (r != this) {
            if (!generateIdEmulation) {
                if (Literal.isEmptySequence(operand0) || Literal.isEmptySequence(operand1)) {
                    return Literal.makeEmptySequence();
                }
            }
        }
        return r;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new IdentityComparison(operand0.copy(), operator, operand1.copy());
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo node1 = getNode(operand0, context);
        if (node1==null) {
            if (generateIdEmulation) {
                return BooleanValue.get(getNode(operand1, context)==null);
            }
            return null;
        }

        NodeInfo node2 = getNode(operand1, context);
        if (node2==null) {
            if (generateIdEmulation) {
                return BooleanValue.FALSE;
            }
            return null;
        }

        return BooleanValue.get(compareIdentity(node1, node2));
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        NodeInfo node1 = getNode(operand0, context);
        if (node1==null) {
            return generateIdEmulation && getNode(operand1, context) == null;
        }

        NodeInfo node2 = getNode(operand1, context);
        return node2 != null && compareIdentity(node1, node2);

    }

    private boolean compareIdentity(NodeInfo node1, NodeInfo node2) {

        switch (operator) {
        case Token.IS:
            return node1.isSameNodeInfo(node2);
        case Token.PRECEDES:
            return GlobalOrderComparer.getInstance().compare(node1, node2) < 0;
        case Token.FOLLOWS:
            return GlobalOrderComparer.getInstance().compare(node1, node2) > 0;
        default:
            throw new UnsupportedOperationException("Unknown node identity test");
        }
    }

    private NodeInfo getNode(Expression exp, XPathContext c) throws XPathException {
        return (NodeInfo)exp.evaluateItem(c);
    }


    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
