package net.sf.saxon.expr;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.SequenceType;

/**
* A RangeExpression is an expression that represents an integer sequence as
* a pair of end-points (for example "x to y").
* If the end-points are equal, the sequence is of length one.
 * <p>From Saxon 7.8, the sequence must be ascending; if the end-point is less
 * than the start-point, an empty sequence is returned. This is to allow
 * expressions of the form "for $i in 1 to count($seq) return ...." </p>
*/

public class RangeExpression extends BinaryExpression {

    /**
    * Construct a RangeExpression
    */

    public RangeExpression(Expression start, int op, Expression end) {
        super(start, op, end);
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand0 = operand0.analyze(env, contextItemType);
        operand1 = operand1.analyze(env, contextItemType);

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 0, null);
        operand0 = TypeChecker.staticTypeCheck(
                operand0, SequenceType.OPTIONAL_INTEGER, false, role0, env);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 1, null);
        operand1 = TypeChecker.staticTypeCheck(
                operand1, SequenceType.OPTIONAL_INTEGER, false, role1, env);
        return super.simplify(env);
    }

    /**
    * Get the data type of the items returned
    */

    public ItemType getItemType() {
        return Type.INTEGER_TYPE;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
    * Return an iteration over the sequence
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
        if (av1 == null) {
            return new EmptyIterator();
        }
        NumericValue v1 = (NumericValue)av1.getPrimitiveValue();

        AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
        if (av2 == null) {
            return new EmptyIterator();
        }
        NumericValue v2 = (NumericValue)av2.getPrimitiveValue();

        if (v1.compareTo(v2) > 0) {
            return new EmptyIterator();
        }
        return new RangeIterator(v1.longValue(), v2.longValue());
    }

    /**
    * Iterator that produces numeric values in a monotonic sequence,
    * ascending or descending
    */

    public static class RangeIterator implements SequenceIterator,
                                                  ReversibleIterator,
                                                  LastPositionFinder,
                                                  LookaheadIterator {

        long start;
        long currentValue;
        int increment;
        long limit;

        public RangeIterator(long start, long end) {
            this.start = start;
            increment = (start <= end ? +1 : -1);
            currentValue = start;
            limit = end;
        }

        public boolean hasNext() {
            if (increment>0) {
                return currentValue <= limit;
            } else {
                return currentValue >= limit;
            }
        }

        public Item next() {
            if (!hasNext()) {
                return null;
            }
            long d = currentValue;
            currentValue += increment;
            return new IntegerValue(d);
        }

        public Item current() {
            return new IntegerValue(currentValue - increment);
        }

        public int position() {
            if (increment>0) {
                return (int)(currentValue - start);
            } else {
                return (int)(start - currentValue);
            }
        }

        public int getLastPosition() {
            return (int)((limit - start) * increment + 1);
        }

        public SequenceIterator getAnother() throws XPathException {
            return new RangeIterator(start, limit);
        }

        public SequenceIterator getReverseIterator() {
            return new RangeIterator(limit, start);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
