package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.trace.ExpressionPresenter;

import java.util.Iterator;

/**
 * A TailExpression represents a FilterExpression of the form EXPR[position() > n]
 * Here n is usually 2, but we allow other values
 */
public class TailExpression extends Expression {

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

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        base = visitor.typeCheck(base, contextItemType);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        base = visitor.optimize(base, contextItemType);
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

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new TailExpression(base.copy(), start);
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

    /**
     * Get the base expression (of which this expression returns the tail part of the value)
     * @return the base expression
     */

    public Expression getBaseExpression() {
        return base;
    }

    /**
     * Get the start offset
     * @return the one-based start offset (returns 2 if all but the first item is being selected)
     */

    public int getStart() {
        return start;
    }

    /**
     * Compare two expressions to see if they are equal
     * @param other the other expression
     * @return true if the expressions are equivalent
     */

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
        if ((baseIter.getProperties() & SequenceIterator.GROUNDED) != 0) {
            return new ValueTailIterator(((GroundedIterator)baseIter).materialize(), start - 1);
        } else {
            return TailIterator.make(baseIter, start);
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("tail");
        destination.emitAttribute("start", start+"");
        base.explain(destination);
        destination.endElement();
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