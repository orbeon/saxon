package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Cardinality;

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
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        if (!Cardinality.allowsMany(operand.getCardinality())) {
            return operand;
        }
        return this;
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
                operand = operand.promote(offer);
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
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return operand.iterate(context).next();
    }

    /**
    * Diagnostic print of expression structure
    */

    public String displayOperator(NamePool pool) {
        return "first item of";
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
