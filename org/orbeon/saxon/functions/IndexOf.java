package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.IntegerValue;


/**
* The XPath 2.0 index-of() function
*/


public class IndexOf extends CollatingFunction {

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        AtomicComparer comparer = getAtomicComparer(2, context);
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue val = (AtomicValue)argument[1].evaluateItem(context);
        return new IndexIterator(seq, val, comparer);
    }

    private class IndexIterator implements SequenceIterator {

        SequenceIterator base;
        AtomicValue value;
        AtomicComparer comparer;
        int index = 0;
        int position = 0;
        //Item nextItem = null;
        Item current = null;

        public IndexIterator(SequenceIterator base, AtomicValue value, AtomicComparer comparer)
        {
            this.base = base;
            this.value = value;
            this.comparer = comparer;
        }

        public Item next() throws XPathException {
            while (true) {
                AtomicValue i = (AtomicValue)base.next();
                if (i==null) break;
                index++;
                if (comparer.comparesEqual(i, value)) {
                    current = new IntegerValue(index);
                    position++;
                    return current;
                }
            }
            return null;
        }

        public Item current() {
            return current;
        }

        public int position() {
            return position;
        }

        public SequenceIterator getAnother() throws XPathException {
            return new IndexIterator(base.getAnother(), value, comparer);
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
