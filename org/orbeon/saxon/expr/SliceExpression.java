package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * A SliceExpression represents a FilterExpression of the form EXPR[position() > n and position() < m],
 * where n and m are not necessarily constants
 */
public class SliceExpression extends ComputedExpression {

    Expression base;
    PositionRange range;

    /**
     * Construct a SliceExpression
     */

    public SliceExpression(Expression base, PositionRange range) {
        this.base = base;
        this.range = range;
        adoptChildExpression(base);
        adoptChildExpression(range);
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.typeCheck(env, contextItemType);
        range = (PositionRange)range.typeCheck(env, contextItemType);
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.optimize(opt, env, contextItemType);
        range = (PositionRange)range.optimize(opt, env, contextItemType);
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
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
                range = (PositionRange)doPromotion(range, offer);
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
        return new PairIterator(base, range);
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
        if (range == original) {
            range = (PositionRange)replacement; // fail if it's not a PositionRange!
            found = true;
        }
        return found;
    }

    public Expression getBaseExpression() {
        return base;
    }

    public boolean equals(Object other) {
        return other instanceof SliceExpression &&
                base.equals(((SliceExpression)other).base) &&
                range.equals(((SliceExpression)other).range);
    }

    public int hashCode() {
        return base.hashCode();
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator baseIter = base.iterate(context);
        return range.makePositionIterator(baseIter, context);
    }

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "slice");
        base.display(level+1, out, config);
        range.display(level+1, out, config);
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