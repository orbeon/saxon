package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.Value;


/**
 * Class to handle many-to-many A less-than B comparisons by evaluating min(A) &lt; max(B), and
 * similarly for greater-than, etc. This expression is used only where it is known that the
 * comparisons will all be numeric: that is, where at least one of the operands has a static
 * type that is a numeric sequence.
 */

public class MinimaxComparison extends BinaryExpression {

    public MinimaxComparison(Expression p0, int operator, Expression p1) {
        super(p0, operator, p1);
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Type-check the expression.
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression exp = super.analyze(env, contextItemType);
        if (exp != this) {
            return exp;
        }

        // if either operand is a statically-known list of values, we only need
        // to retain the minimum or maximum value, depending on the operator.

        if (operand0 instanceof Value) {
            NumericValue[] range = getRange(operand0.iterate(null));
            if (range==null) {
                return BooleanValue.FALSE;
            }
            if (operator == Token.LT || operator == Token.LE) {
                operand0 = range[0];
            } else {
                operand0 = range[1];
            }
        }
        if (operand1 instanceof Value) {
            NumericValue[] range = getRange(operand1.iterate(null));
            if (range==null) {
                return BooleanValue.FALSE;
            }
            if (operator == Token.GT || operator == Token.GE) {
                operand1 = range[0];
            } else {
                operand1 = range[1];
            }
        }
        return this;
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
    */

    public ItemType getItemType() {
        return Type.BOOLEAN_TYPE;
    }

    /**
    * Evaluate the expression in a given context
    * @param context the given context for evaluation
    * @return a BooleanValue representing the result of the numeric comparison of the two operands
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the expression in a boolean context
    * @param context the given context for evaluation
    * @return a boolean representing the result of the numeric comparison of the two operands
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {

        NumericValue[] range1 = getRange(operand0.iterate(context));
        NumericValue[] range2 = getRange(operand1.iterate(context));

        if (range1==null) return false;
        if (range2==null) return false;

        // Now test how the min of one sequence compares to the max of the other
        switch(operator) {
            case Token.LT:
                return range1[0].compareTo( range2[1] ) < 0;
            case Token.LE:
                return range1[0].compareTo( range2[1] ) <= 0;
            case Token.GT:
                return range1[1].compareTo( range2[0] ) > 0;
            case Token.GE:
                return range1[1].compareTo( range2[0] ) >= 0;
            default:
                throw new UnsupportedOperationException("Unknown operator " + operator);
        }
    }

    /**
    * Get the value range of a sequence, in the form of an array of two SimpleValues, these
    * being the minimum and maximum respectively. NaN values are ignored.
    * @return null if the sequence is empty or if all values are NaN
    */

    private static NumericValue[] getRange(SequenceIterator iter) throws XPathException {
        NumericValue[] range = null;
        while (true) {
            NumericValue val = (NumericValue)iter.next();
            if (val == null) break;

            if (val.isNaN()) continue;

            if (range == null) {
                range = new NumericValue[2];
                range[0] = val;
                range[1] = val;
            } else {
                if (val.compareTo(range[0]) < 0) {
                    range[0] = val;
                }
                if (val.compareTo(range[1]) > 0) {
                    range[1] = val;
                }
            }
        }
        return range;
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
