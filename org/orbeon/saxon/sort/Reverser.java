package net.sf.saxon.sort;

import net.sf.saxon.expr.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;

/**
 * A Reverser is an expression that reverses the order of a sequence of items.
 */

public class Reverser extends UnaryExpression {

    public Reverser(Expression base) {
        super(base);
    }



    public int computeSpecialProperties() {
        int baseProps = operand.getSpecialProperties();
        if ((baseProps & StaticProperty.REVERSE_DOCUMENT_ORDER) != 0) {
            return (baseProps &
                (~StaticProperty.REVERSE_DOCUMENT_ORDER)) |
                StaticProperty.ORDERED_NODESET;
        } else if ((baseProps & StaticProperty.ORDERED_NODESET) != 0) {
            return (baseProps &
                (~StaticProperty.ORDERED_NODESET)) |
                StaticProperty.REVERSE_DOCUMENT_ORDER;
        } else {
            return baseProps;
        }
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            operand = operand.promote(offer);
            return this;
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator forwards = operand.iterate(context);
        if (forwards instanceof ReversibleIterator) {
            return ((ReversibleIterator)forwards).getReverseIterator();
        } else {
            SequenceExtent extent = new SequenceExtent(forwards);
            return extent.reverseIterate();
        }
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return operand.effectiveBooleanValue(context);
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "reverse order";
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