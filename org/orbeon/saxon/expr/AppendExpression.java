package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.xpath.StaticError;
import org.orbeon.saxon.xpath.XPathException;


/**
* An expression representing a sequence that is the concatenation of two given sequences.
* This implements the "comma" operator in XPath sequence expressions.
*/

public final class AppendExpression extends BinaryExpression {

    /**
    * Constructor
    * @param p1 the left-hand operand
    * @param op the operator (always ",")
    * @param p2 the right-hand operand
    */

    public AppendExpression(Expression p1, int op, Expression p2) {
        super(p1, op, p2);
    }

    /**
    * Determine the data type of the items returned by this expression
    * @return the data type
    */

    public final ItemType getItemType() {
        ItemType t1 = operand0.getItemType();
        ItemType t2 = operand1.getItemType();
        return Type.getCommonSuperType(t1, t2);
    }

    /**
    * Determine the static cardinality of the expression
    */

    public final int computeCardinality() {
        int c1 = operand0.getCardinality();
        int c2 = operand1.getCardinality();

        if (operand0 instanceof EmptySequence) return c2;
        if (operand1 instanceof EmptySequence) return c1;

        if (((c1 & StaticProperty.ALLOWS_ZERO) != 0) && ((c2 & StaticProperty.ALLOWS_ZERO) != 0)) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return StaticProperty.ALLOWS_ONE_OR_MORE;
        }
    }

    /**
    * Simplify the expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {

        operand0 = operand0.simplify(env);
        operand1 = operand1.simplify(env);

        if (operand0 instanceof EmptySequence) return operand1;
        if (operand1 instanceof EmptySequence) return operand0;

        // For lists consisting entirely of constant atomic values, build a SequenceExtent at compile time

        if (isAtomicSequence()) {
            try {
                return new SequenceExtent(iterate(null));
            } catch (XPathException err) {
                throw new StaticError(err);
                // Can't happen
            }
        }

        // An expression such as (1,2,$x) will be parsed as (1, (2, $x)). This can be
        // simplified to ((1,2), $x), reducing the number of iterators needed to evaluate it

        if (operand0 instanceof Value &&
                operand1 instanceof AppendExpression &&
                ((AppendExpression)operand1).operand0 instanceof Value) {
            return new AppendExpression(
                        new AppendExpression(operand0, operator, ((AppendExpression)operand1).operand0),
                        operator,
                        ((AppendExpression)operand1).operand1).simplify(env);
        }

        return this;
    }

    /**
    * An AppendExpression is classified as an atomic sequence if both operands are atomic values or
    * atomic sequences
    */

    private boolean isAtomicSequence() {
        return isAtomic(operand0) && isAtomic(operand1);
    }

    private boolean isAtomic(Expression exp) {
        return (exp instanceof AtomicValue) ||
                (exp instanceof SequenceExtent);
    }

    /**
    * Iterate over the value of the expression.
    * @param c The context for evaluation
    * @return a SequenceIterator representing the concatenation of the two operands
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        return new AppendIterator(operand0.iterate(c), operand1, c);
    }

    /**
    * Iterator that concatenates the results of two supplied iterators
    */

    public static class AppendIterator implements SequenceIterator {

        private SequenceIterator first;
        private Expression second;
        private XPathContext context;
        private SequenceIterator currentIterator;

        /**
         * This form of constructor is designed to delay getting an iterator for the second
         * expression until it is actually needed. This gives savings in cases where the
         * iteration is aborted prematurely.
         * @param first Iterator over the first operand
         * @param second The second operand
         * @param context The dynamic context for evaluation of the second operand
         */

        public AppendIterator(SequenceIterator first, Expression second, XPathContext context) {
            this.first = first;
            this.second = second;
            this.context = context;
            this.currentIterator = first;
        }

        public Item next() throws XPathException {
            Item n = currentIterator.next();
            if (n == null && currentIterator==first) {
                currentIterator = second.iterate(context);
                return currentIterator.next();
            }
            return n;
        }

        public Item current() {
            return currentIterator.current();
        }

        public int position() {
            if (currentIterator == first) {
                return first.position();
            } else {
                return first.position() + currentIterator.position();
            }
        }

        public SequenceIterator getAnother() throws XPathException {
            return new AppendIterator(first.getAnother(), second, context);
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
