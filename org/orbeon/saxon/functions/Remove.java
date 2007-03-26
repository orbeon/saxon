package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.IntegerValue;
import org.orbeon.saxon.value.NumericValue;

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
            TailExpression t = new TailExpression(argument[0], 2);
            t.setLocationId(getLocationId());
            t.setParentExpression(getParentExpression());
            return t;
        } else {
            return this;
        }
    }

    /**
    * Determine the data type of the items in the sequence
    * @return the type of the input sequence
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return argument[0].getItemType(th);
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

    /**
     * An implementation of SequenceIterator that returns all items except the one
     * at a specified position.
     */

    private class RemoveIterator implements SequenceIterator, LastPositionFinder {

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
            if (current == null) {
                position = -1;
            } else {
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

        /**
         * Get the last position (that is, the number of items in the sequence). This method is
         * non-destructive: it does not change the state of the iterator.
         * The result is undefined if the next() method of the iterator has already returned null.
         */

        public int getLastPosition() throws XPathException {
            if (base instanceof LastPositionFinder) {
                int x = ((LastPositionFinder)base).getLastPosition();
                if (removePosition >= 1 && removePosition <= x) {
                    return x - 1;
                } else {
                    return x;
                }
            } else {
                // This shouldn't happen, because this iterator only has the LAST_POSITION_FINDER property
                // if the base iterator has the LAST_POSITION_FINDER property
                throw new AssertionError("base of removeIterator is not a LastPositionFinder");
            }
        }

        public SequenceIterator getAnother() throws XPathException {
            return new RemoveIterator(  base.getAnother(),
                                        removePosition);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link GROUNDED}, {@link LAST_POSITION_FINDER},
         *         and {@link LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return base.getProperties() & LAST_POSITION_FINDER;
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
