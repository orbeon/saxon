package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.DoubleValue;

/**
 * Negate Expression: implements the unary minus operator.
 * This expression is initially created as an ArithmeticExpression (or in backwards
 * compatibility mode, an ArithmeticExpression10) to take advantage of the type checking code.
 * So we don't need to worry about type checking or argument conversion.
 */

public class NegateExpression extends UnaryExpression {

    private boolean backwardsCompatible;

    /**
     * Create a NegateExpression
     * @param base the expression that computes the value whose sign is to be reversed
     */

    public NegateExpression(Expression base) {
        super(base);
    }

    /**
     * Set whether the expression is to be evaluated in XPath 1.0 compatibility mode
     * @param compatible true if XPath 1.0 compatibility mode is enabled
     */

    public void setBackwardsCompatible(boolean compatible) {
        backwardsCompatible = compatible;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression oldop = operand;
        RoleLocator role = new RoleLocator(RoleLocator.UNARY_EXPR, "-", 0);
        operand = TypeChecker.staticTypeCheck(operand, SequenceType.OPTIONAL_NUMERIC, backwardsCompatible,
                role, visitor);
        operand = visitor.typeCheck(operand, contextItemType);
        if (operand != oldop) {
            adoptChildExpression(operand);
        }
        return this;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return operand.getItemType(th);
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        NumericValue v1 = (NumericValue) operand.evaluateItem(context);
        if (v1 == null) {
            return backwardsCompatible ? DoubleValue.NaN : null;
        }
        return v1.negate();
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new NegateExpression(getBaseExpression().copy());
    }

    protected String displayOperator(Configuration config) {
        return "-";
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
