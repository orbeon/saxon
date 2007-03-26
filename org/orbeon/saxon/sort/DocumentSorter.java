package org.orbeon.saxon.sort;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;

/**
 * A DocumentSorter is an expression that sorts a sequence of nodes into
 * document order.
 */
public class DocumentSorter extends UnaryExpression {

    private NodeOrderComparer comparer;

    public DocumentSorter(Expression base) {
        super(base);
        int props = base.getSpecialProperties();
        if (((props & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) ||
                (props & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) {
            comparer = LocalOrderComparer.getInstance();
        } else {
            comparer = GlobalOrderComparer.getInstance();
        }
    }

    public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if ((operand.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
            // this can happen as a result of further simplification
            ComputedExpression.setParentExpression(operand, getParentExpression());
            return operand;
        }
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.optimize(opt, env, contextItemType);
        if ((operand.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
            // this can happen as a result of further simplification
            ComputedExpression.setParentExpression(operand, getParentExpression());
            return operand;
        }
        return this;
    }


    public int computeSpecialProperties() {
        return operand.getSpecialProperties() | StaticProperty.ORDERED_NODESET;
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            operand = doPromotion(operand, offer);
            return this;
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return new DocumentOrderIterator(operand.iterate(context), comparer);
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return operand.effectiveBooleanValue(context);
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        if (comparer instanceof LocalOrderComparer) {
            return "intra-document sort and deduplicate";
        } else {
            return "sort and deduplicate";
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
// Contributor(s): none
//