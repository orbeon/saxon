package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;

import java.io.PrintStream;

/**
* PositionRange: a boolean expression that tests whether the position() is
* within a certain range. This expression can occur in any context but it is
* optimized when it appears as a predicate (see FilterIterator)
*/

// TODO: why limit this optimisation to cases where the range is known statically?

public final class PositionRange extends ComputedExpression {

    private int minPosition;
    private int maxPosition;

    /**
    * Create a position range
    */

    public PositionRange(int min, int max) {
        minPosition = min;
        maxPosition = max;
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws StaticError {
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) {
        return this;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        int p = c.getContextPosition();
        return BooleanValue.get(p >= minPosition && p <= maxPosition);
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
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
    * Get the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_POSITION;
    }

    /**
    * Get the minimum position
    */

    public int getMinPosition() {
        return minPosition;
    }

    /**
    * Get the maximum position
    */

    public int getMaxPosition() {
        return maxPosition;
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "positionRange(" + minPosition + "," + maxPosition + ")");
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
