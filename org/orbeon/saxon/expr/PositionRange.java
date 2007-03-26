package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.IntegerValue;
import org.orbeon.saxon.value.NumericValue;

import java.io.PrintStream;
import java.util.Iterator;

/**
* PositionRange: a boolean expression that tests whether the position() is
* within a certain range. This expression can occur in any context but it is
* optimized when it appears as a predicate (see FilterIterator)
*/

public final class PositionRange extends ComputedExpression {

    private Expression minPosition;
    private Expression maxPosition; // may be null to indicate an open range
    boolean maxSameAsMin = false;

    /**
    * Create a position range
    */

    public PositionRange(Expression min, Expression max) {
        minPosition = min;
        maxPosition = max;
        adoptChildExpression(min);
        adoptChildExpression(max);
    }

    /**
     * Create a position "range" for an exact position
     */

     public PositionRange(Expression pos) {
        minPosition = pos;
        adoptChildExpression(pos);
        maxSameAsMin = true;
    }

    /**
    * Create a constant position range
    */

    public PositionRange(int min, int max) {
        minPosition = new IntegerValue(min);
        if (max == Integer.MAX_VALUE) {
            maxPosition = null;
        } else if (max == min) {
            maxPosition = null;
            maxSameAsMin = true;
        } else {
            maxPosition = new IntegerValue(max);
        }
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        minPosition = minPosition.simplify(env);
        if (maxPosition != null) {
            maxPosition = maxPosition.simplify(env);
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        minPosition = minPosition.typeCheck(env, contextItemType);
        if (maxPosition != null) {
            maxPosition = maxPosition.typeCheck(env, contextItemType);
        }
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
        minPosition = minPosition.optimize(opt, env, contextItemType);
        if (maxPosition != null) {
            maxPosition = maxPosition.optimize(opt, env, contextItemType);
        }
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
        if (maxPosition == null) {
            NumericValue min = (NumericValue)minPosition.evaluateItem(c);
            if (min == null) {
                return BooleanValue.FALSE;
            }
            return BooleanValue.get(maxSameAsMin ? p == min.longValue() : p >= min.longValue());
        } else {
            NumericValue min = (NumericValue)minPosition.evaluateItem(c);
            if (min == null) {
                return BooleanValue.FALSE;
            }
            NumericValue max = (NumericValue)maxPosition.evaluateItem(c);
            if (max == null) {
                return BooleanValue.FALSE;
            }
            return BooleanValue.get(p >= min.longValue() && p <= max.longValue());
        }
    }

    /**
     * Make an iterator over a range of a sequence determined by this position range
     */

    public SequenceIterator makePositionIterator(SequenceIterator base, XPathContext c) throws XPathException {
        int low, high;
        NumericValue min = (NumericValue)minPosition.evaluateItem(c);
        low = (int)min.longValue();
        if (maxPosition == null) {
            high = (maxSameAsMin ? low : Integer.MAX_VALUE);
        } else {
            NumericValue max = (NumericValue)maxPosition.evaluateItem(c);
            high = (int)max.longValue();
        }
        return PositionIterator.make(base, low, high);
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
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
    * Get the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_POSITION;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        if (maxPosition == null) {
            return new MonoIterator(minPosition);
        } else {
            return new PairIterator(minPosition, maxPosition);
        }
    }

   /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (minPosition == original) {
            minPosition = replacement;
            found = true;
        }
        if (maxPosition == original) {
            maxPosition = replacement;
            found = true;
        }
                return found;
    }

    /**
     * Test if the first and last position are both constant 1
     */

    public boolean isFirstPositionOnly() {
        try {
            return (minPosition instanceof NumericValue && ((NumericValue)minPosition).longValue() == 1) &&
                    (maxSameAsMin ||
                    (maxPosition instanceof NumericValue && ((NumericValue)maxPosition).longValue() == 1));
        } catch (XPathException e) {
            return false;
        }
    }

    /**
     * Test whether the range is focus-dependent. An example of a focus-dependent range is
     * (1 to position()). We could treat last() specially but we don't.
     */

    public boolean hasFocusDependentRange() {
        return ((minPosition.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0) ||
                (maxPosition != null && (maxPosition.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0);
    }

    /**
     * Test if the position range matches at most one item
     */

    public boolean matchesAtMostOneItem() {
        return maxSameAsMin ||
                (maxPosition != null && minPosition.equals(maxPosition) && !hasFocusDependentRange());
    }

    /**
     * If this is an open-ended range with a constant start position, make a TailExpression.
     * Otherwise return null
     */

    public TailExpression makeTailExpression(Expression start) {
        if (maxPosition == null && minPosition instanceof IntegerValue && !maxSameAsMin) {
            return new TailExpression(start, (int)((IntegerValue)minPosition).longValue());
        } else {
            return null;
        }

    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "positionRange");
        minPosition.display(level+1, out, config);
        if (maxPosition == null) {
            if (maxSameAsMin) {
                out.println(ExpressionTool.indent(level+1) + "(one item only)");
            } else {
                out.println(ExpressionTool.indent(level+1) + "(end)");
            }
        } else {
            maxPosition.display(level+1, out, config);
        }
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
