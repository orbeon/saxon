package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.AtomicSortComparer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

import java.util.HashSet;

/**
* The XPath 2.0 distinct-values() function
*/

public class DistinctValues extends CollatingFunction {

    /**
    * Evaluate the function to return an iteration of selected values or nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator iter = argument[0].iterate(context);
        return new DistinctIterator(iter, getAtomicSortComparer(1, context));
    }

    /**
    * Get a AtomicSortComparer that can be used to compare values
    * @param arg the position of the argument (starting at 0) containing the collation name.
    * If this argument was not supplied, the default collation is used
    * @param context The dynamic evaluation context.
    */

    protected AtomicSortComparer getAtomicSortComparer(int arg, XPathContext context) throws XPathException {
        return new AtomicSortComparer(getCollator(arg, context, true));
    }

    /**
     * Iterator class to return the distinct values in a sequence
     */

    public static class DistinctIterator implements SequenceIterator {

        private SequenceIterator base;
        private AtomicSortComparer comparer;
        private int position;
        private AtomicValue current;
        private HashSet lookup = new HashSet(40);

        /**
         * Create an iterator over the distinct values in a sequence
         * @param base the input sequence. This must return atomic values only.
         * @param comparer The comparer used to obtain comparison keys from each value;
         * these comparison keys are themselves compared using equals().
         */

        public DistinctIterator(SequenceIterator base, AtomicSortComparer comparer) {
            this.base = base;
            this.comparer = comparer;
            position = 0;
        }

        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next item, or null if there are no more items.
         * @throws net.sf.saxon.trans.XPathException
         *          if an error occurs retrieving the next item
         */

        public Item next() throws XPathException {
            while (true) {
                AtomicValue nextBase = (AtomicValue)base.next();
                if (nextBase==null) {
                    current = null;
                    return null;
                }
                AtomicSortComparer.ComparisonKey key = comparer.getComparisonKey(nextBase);
                if (lookup.contains(key)) {
                    continue;
                } else {
                    lookup.add(key);
                    current = nextBase;
                    position++;
                    return nextBase;
                }
            }
        }

        /**
         * Get the current value in the sequence (the one returned by the
         * most recent call on next()). This will be null before the first
         * call of next().
         *
         * @return the current item, the one most recently returned by a call on
         *         next(); or null, if next() has not been called, or if the end
         *         of the sequence has been reached.
         */

        public Item current() {
            return current;
        }

        /**
         * Get the current position. This will be zero before the first call
         * on next(), otherwise it will be the number of times that next() has
         * been called.
         *
         * @return the current position, the position of the item returned by the
         *         most recent call of next()
         */

        public int position() {
            return position;
        }

        /**
         * Get another SequenceIterator that iterates over the same items as the original,
         * but which is repositioned at the start of the sequence.
         *
         * @return a SequenceIterator that iterates over the same items,
         *         positioned before the first item
         * @throws net.sf.saxon.trans.XPathException
         *          if any error occurs
         */

        public SequenceIterator getAnother() throws XPathException {
            return new DistinctIterator(base.getAnother(), comparer);
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
