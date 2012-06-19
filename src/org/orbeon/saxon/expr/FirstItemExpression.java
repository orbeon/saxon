package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;

/**
* A FirstItemExpression returns the first item in the sequence returned by a given
* base expression
*/

public final class FirstItemExpression extends UnaryExpression {

    /**
    * Constructor
    * @param base A sequence expression denoting sequence whose first item is to be returned
    */

    public FirstItemExpression(Expression base) {
        super(base);
        computeStaticProperties();
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expresion visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        // don't remove this expression just because the operand is known to be a singleton.
        // It might be that this expression was added to give early exit from evaluating the underlying expression
//        if (!Cardinality.allowsMany(operand.getCardinality())) {
//            ComputedExpression.setParentExpression(operand, getParentExpression());
//            return operand;
//        }
        if (operand instanceof FirstItemExpression) {
            return operand;
        }
        return super.optimize(visitor, contextItemType);
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                // we can't push the UNORDERED property down to the operand, because order is significant
                operand = doPromotion(operand, offer);
            }
            return this;
        }
    }

    /**
    * Get the static cardinality
    */

    public int computeCardinality() {
        return operand.getCardinality() & ~StaticProperty.ALLOWS_MANY;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new FirstItemExpression(getBaseExpression().copy());
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        Item result = iter.next();
        iter.close();
        return result;
    }

   
    public String displayExpressionName() {
        return "firstItem";
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
