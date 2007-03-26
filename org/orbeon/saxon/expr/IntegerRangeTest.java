package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.NumericValue;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

/**
* An IntegerRangeTest is an expression of the form
 * E = N to M
 * where E, N, and M are all expressions of type integer.
*/

public class IntegerRangeTest extends ComputedExpression {

    Expression value;
    Expression min;
    Expression max;

    /**
    * Construct a IntegerRangeTest
    */

    public IntegerRangeTest(Expression value, Expression min, Expression max) {
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        // Already done, we only get one of these expressions after the operands have been analyzed
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
    * Get the data type of the items returned
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.BOOLEAN_TYPE;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        Expression[] e = {value, min, max};
        return Arrays.asList(e).iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (value == original) {
            value = replacement;
            found = true;
        }
        if (min == original) {
            min = replacement;
            found = true;
        }
        if (max == original) {
            max = replacement;
            found = true;
        }
                return found;
    }


    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue av = (AtomicValue)value.evaluateItem(c);
        if (av==null) {
            return BooleanValue.FALSE;
        }
        NumericValue v = (NumericValue)av.getPrimitiveValue();

        AtomicValue av2 = (AtomicValue)min.evaluateItem(c);
        NumericValue v2 = (NumericValue)av2.getPrimitiveValue();

        if (v.compareTo(v2) < 0) {
            return BooleanValue.FALSE;
        }
        AtomicValue av3 = (AtomicValue)max.evaluateItem(c);
        NumericValue v3 = (NumericValue)av3.getPrimitiveValue();

        return BooleanValue.get(v.compareTo(v3) <= 0);
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "rangeTest min<value<max");
        min.display(level+1, out, config);
        value.display(level+1, out, config);
        max.display(level+1, out, config);
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
