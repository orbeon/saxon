package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;

/**
* The XPath 2.0 insert-before() function
*/


public class Insert extends SystemFunction {

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue n0 = (AtomicValue)argument[1].evaluateItem(context);
        NumericValue n = (NumericValue)n0.getPrimitiveValue();
        int pos = (int)n.longValue();
        SequenceIterator ins = argument[2].iterate(context);
        return new InsertIterator(seq, ins, pos);
    }

    public static class InsertIterator implements SequenceIterator {

        private SequenceIterator base;
        private SequenceIterator insert;
        private int insertPosition;
        private int position = 0;
        private Item current = null;
        private boolean inserting = false;

        public InsertIterator(SequenceIterator base, SequenceIterator insert, int insertPosition) {
            this.base = base;
            this.insert = insert;
            this.insertPosition = (insertPosition<1 ? 1 : insertPosition);
            this.inserting = (insertPosition==1);
        }

        public Item next() throws XPathException {
            Item nextItem;
            if (inserting) {
                nextItem = insert.next();
                if (nextItem == null) {
                    inserting = false;
                    nextItem = base.next();
                }
            } else {
                if (position == insertPosition-1) {
                    nextItem = insert.next();
                    if (nextItem == null) {
                        nextItem = base.next();
                    } else {
                        inserting = true;
                    }
                } else {
                    nextItem = base.next();
                    if (nextItem==null && position < insertPosition-1) {
                        inserting = true;
                        nextItem = insert.next();
                    }
                }
            }
            if (nextItem == null) {
                return null;
            } else {
                current = nextItem;
                position++;
                return current;
            }
        }

        public Item current() {
            return current;
        }

        public int position() {
            return position;
        }

        public SequenceIterator getAnother() throws XPathException {
            return new InsertIterator(  base.getAnother(),
                                        insert.getAnother(),
                                        insertPosition);
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
// Contributor(s): none.
//
