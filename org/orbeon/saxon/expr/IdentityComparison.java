package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.sort.GlobalOrderComparer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;


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
     */

    public void setGenerateIdEmulation(boolean flag) {
        generateIdEmulation = flag;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {

        operand0 = operand0.analyze(env, contextItemType);
        operand1 = operand1.analyze(env, contextItemType);

        RoleLocator role0 = new RoleLocator(
                RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        operand0 = TypeChecker.staticTypeCheck(
                operand0, SequenceType.OPTIONAL_NODE, false, role0, env);

        RoleLocator role1 = new RoleLocator(
                RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        operand1 = TypeChecker.staticTypeCheck(
                operand1, SequenceType.OPTIONAL_NODE, false, role1, env);
        return this;
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
            if (generateIdEmulation) {
                return getNode(operand1, context) == null;
            }
            return false;
        }

        NodeInfo node2 = getNode(operand1, context);
        if (node2==null) return false;

        return compareIdentity(node1, node2);
    }

    private boolean compareIdentity(NodeInfo node1, NodeInfo node2) {

        switch (operator) {
        case Token.IS:
            return node1.isSameNodeInfo(node2);
//        case Token.ISNOT:
//            return !node1.isSameNode(node2);
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
    */

    public ItemType getItemType() {
        return Type.BOOLEAN_TYPE;
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
