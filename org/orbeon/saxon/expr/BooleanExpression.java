package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;


/**
* Boolean expression: two truth values combined using AND or OR.
*/

class BooleanExpression extends BinaryExpression {

//    public BooleanExpression() {}

    public BooleanExpression(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Type-check the expression. Default implementation for binary operators that accept
    * any kind of operand
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression exp = super.analyze(env, contextItemType);
        if (exp instanceof BooleanExpression) {
            ((BooleanExpression)exp).operand0 = ExpressionTool.unsortedIfHomogeneous(operand0, false);
            ((BooleanExpression)exp).operand1 = ExpressionTool.unsortedIfHomogeneous(operand1, false);
        }
        return exp;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate as a boolean.
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        switch(operator) {
            case Token.AND:
                return operand0.effectiveBooleanValue(c) && operand1.effectiveBooleanValue(c);

            case Token.OR:
                return operand0.effectiveBooleanValue(c) || operand1.effectiveBooleanValue(c);

            default:
                throw new UnsupportedOperationException("Unknown operator in boolean expression");
        }
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
    */

    public ItemType getItemType() {
        return Type.BOOLEAN_TYPE;
    }

    /**
    * Perform a partial evaluation of the expression, by eliminating specified dependencies
    * on the context.
    * @param dependencies The dependencies to be removed
    * @param context The context to be used for the partial evaluation
    * @return a new expression that does not have any of the specified
    * dependencies
    */

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
