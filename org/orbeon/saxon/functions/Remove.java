package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.TailExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.NumericValue;

/**
* The XPath 2.0 remove() function
*/


public class Remove extends SystemFunction {

    /**
     * Simplify. Recognize remove(seq, 1) as a TailExpression.
     */

     public Expression simplify(StaticContext env) throws XPathException {
        Expression exp = super.simplify(env);
        if (exp instanceof Remove) {
            return ((Remove)exp).simplifyAsTailExpression();
        } else {
            return exp;
        }
    }

    /**
     * Simplify. Recognize remove(seq, 1) as a TailExpression. This
     * is worth doing because tail expressions used in a recursive call
     * are handled specially.
     */

    private Expression simplifyAsTailExpression() {
        if (argument[1] instanceof IntegerValue &&
                ((IntegerValue)argument[1]).longValue() == 1) {
            return new TailExpression(argument[0], 2);
        } else {
            return this;
        }
    }

    /**
    * Determine the data type of the items in the sequence
    * @return the type of the input sequence
    */

    public ItemType getItemType() {
        return argument[0].getItemType();
    }

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue n0 = (AtomicValue)argument[1].evaluateItem(context);
        NumericValue n = (NumericValue)n0.getPrimitiveValue();
        int pos = (int)n.longValue();
        if (pos < 1) {
            return seq;
        }
        return new RemoveIterator(seq, pos);
    }

    // Note: in Saxon 7.1, we used a FilterIterator. However, a FilterIterator at present requires
    // an XPathContext object so it can't be evaluated statically. The remove() function can.

    private class RemoveIterator implements SequenceIterator {

        SequenceIterator base;
        int removePosition;
        int position = 0;
        Item current = null;

        public RemoveIterator(SequenceIterator base, int removePosition) {
            this.base = base;
            this.removePosition = removePosition;
        }

        public Item next() throws XPathException {
            current = base.next();
            if (current != null && base.position() == removePosition) {
                current = base.next();
            }
            if (current != null) {
                position++;
            }
            return current;
        }

        public Item current() {
            return current;
        }

        public int position() {
            return position;
        }

        public SequenceIterator getAnother() throws XPathException {
            return new RemoveIterator(  base.getAnother(),
                                        removePosition);
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
