package net.sf.saxon.expr;

import net.sf.saxon.om.ArrayIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.xpath.XPathException;

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
    }

    public Expression analyze(StaticContext env, ItemType contextItemType) {
        // by the time we get here, the analysis has all been done
        return this;
    }

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                base = base.promote(offer);
            }
            return this;
        }
    }

    public int computeSpecialProperties() {
        return base.getSpecialProperties();
    }

    public ItemType getItemType() {
        return base.getItemType();
    }

    public int computeCardinality() {
        return base.getCardinality() | StaticProperty.ALLOWS_ZERO;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(base);
    }

    public Expression getBaseExpression() {
        return base;
    }

    public int getStart() {
        return start;
    }

    public boolean equals(Object other) {
        return other instanceof TailExpression &&
                base.equals(((TailExpression)other).base);
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

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "tail " + start);
        base.display(level+1, pool, out);
    }

    public static class TailIterator implements SequenceIterator {

        private SequenceIterator base;
        private int start;

        public TailIterator(SequenceIterator base, int start) throws XPathException {
            this.base = base;
            this.start = start;

            // discard the first n-1 items from the underlying iterator
            for (int i=0; i < start-1; i++) {
                base.next();
            }
        }

//        public boolean hasNext() throws XPathException {
//            return base.hasNext();
//        }

        public Item next() throws XPathException {
            return base.next();
        }

        public Item current() {
            return base.current();
        }

        public int position() {
            return base.position() - start + 1;
        }

        public SequenceIterator getAnother() throws XPathException {
            return new TailIterator(base.getAnother(), start);
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