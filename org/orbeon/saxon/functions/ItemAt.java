package org.orbeon.saxon.functions;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.GroundedIterator;
import org.orbeon.saxon.om.GroundedValue;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.type.ItemType;

/**
 * Implements the saxon:item-at() function. This is handled specially because it is generated
 * by the optimizer.
 *
 * <p>The function takes two arguments: the first is an arbitrary sequence, the second is optional numeric.
 * The function returns the same result as let $N := NUMBER return SEQUENCE[$N], including cases where the
 * numeric argument is not a whole number.</p>
*/


public class ItemAt extends SystemFunction {


    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        if (argument[1] instanceof Literal) {
            NumericValue val = (NumericValue)((Literal)argument[1]).getValue();
            if (val.compareTo(1) < 0 || val.compareTo(Integer.MAX_VALUE) > 0 || !val.isWholeNumber()) {
                return new Literal(EmptySequence.getInstance());
            }
        }
        return this;
    }

    /**
    * Evaluate the function to return the selected item.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        NumericValue index = (NumericValue)argument[1].evaluateItem(context);
        if (index == null) {
            return null;
        }
        if (index.compareTo(Integer.MAX_VALUE) <= 0 && index.isWholeNumber()) {
            int intindex = (int)index.longValue();
            if (intindex < 1) {
                return null;
            }
            SequenceIterator base = argument[0].iterate(context);
            if (intindex == 1) {
                return base.next();
            } else if (base instanceof GroundedIterator) {
                GroundedValue value = ((GroundedIterator)base).materialize();
                return value.itemAt(intindex-1);
            } else {
                SequenceIterator tail = TailIterator.make(base, intindex);
                return tail.next();
            }
        } else {
            // there is no item at the required position
            return null;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

