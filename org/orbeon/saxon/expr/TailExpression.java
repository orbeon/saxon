package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.Configuration;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * A TailExpression represents a FilterExpression of the form EXPR[position() > n]
 * Here n is usually 2, but we allow other values
 */
public class TailExpression extends ComputedExpression {

    Expression base;
    int start;      // 1-based offset of first item from base expression
                    // to be included

    /**
     * Construct a TailExpression, representing a filter expression of the form
     * $base[position() >= $start]
     * @param base    the expression to be filtered
     * @param start   the position (1-based) of the first item to be included
     */

    public TailExpression(Expression base, int start) {
        this.base = base;
        this.start = start;
        adoptChildExpression(base);
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.typeCheck(env, contextItemType);
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.optimize(opt, env, contextItemType);
        return this;
    }

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                base = doPromotion(base, offer);
            }
            return this;
        }
    }

    public int computeSpecialProperties() {
        return base.getSpecialProperties();
    }

    public ItemType getItemType(TypeHierarchy th) {
        return base.getItemType(th);
    }

    public int computeCardinality() {
        return base.getCardinality() | StaticProperty.ALLOWS_ZERO;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(base);
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (base == original) {
             base = replacement;
             found = true;
         }
                 return found;
     }


    public Expression getBaseExpression() {
        return base;
    }

    public int getStart() {
        return start;
    }

    public boolean equals(Object other) {
        return other instanceof TailExpression &&
                base.equals(((TailExpression)other).base) &&
                start == ((TailExpression)other).start;
    }

    public int hashCode() {
        return base.hashCode();
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator baseIter = base.iterate(context);
        if (baseIter instanceof ArrayIterator) {
            return ((ArrayIterator)baseIter).makeSliceIterator(start, Integer.MAX_VALUE);
        } else {
            return new TailIterator(baseIter, start);
        }
    }

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "tail " + start);
        base.display(level+1, out, config);
    }

    public static class TailIterator implements SequenceIterator, LastPositionFinder, LookaheadIterator {

        private SequenceIterator base;
        private int start;

        public TailIterator(SequenceIterator base, int start) throws XPathException {
            this.base = base;
            this.start = start;

            // discard the first n-1 items from the underlying iterator
            // TODO: better approaches are possible if the base iterator is grounded
            for (int i=0; i < start-1; i++) {
                Item b = base.next();
                if (b == null) {
                    break;
                }
            }
        }

        public Item next() throws XPathException {
            return base.next();
        }

        public Item current() {
            return base.current();
        }

        public int position() {
            int bp = base.position();
            return (bp > 0 ? (base.position() - start + 1) : bp);
        }

        public boolean hasNext() {
            return ((LookaheadIterator)base).hasNext();
        }

        public int getLastPosition() throws XPathException {
            int bl = ((LastPositionFinder)base).getLastPosition() - start + 1;
            return (bl > 0 ? bl : 0);
        }

        public SequenceIterator getAnother() throws XPathException {
            return new TailIterator(base.getAnother(), start);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
         *         and {@link #LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return base.getProperties() & (LAST_POSITION_FINDER | LOOKAHEAD);
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