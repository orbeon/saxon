package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.NumericValue;

import java.io.PrintStream;

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

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        // Already done, we only get one of these expressions after the operands
        // have been analyzed
        return this;
    }

    /**
    * Get the data type of the items returned
    */

    public ItemType getItemType() {
        return Type.BOOLEAN_TYPE;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
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

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "rangeTest min<value<max");
        min.display(level+1, pool, out);
        value.display(level+1, pool, out);
        max.display(level+1, pool, out);
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
