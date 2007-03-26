package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.sort.AtomicComparer;
import org.orbeon.saxon.sort.AtomicSortComparer;
import org.orbeon.saxon.sort.ComparisonKey;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;

import java.util.Comparator;
import java.util.HashSet;

/**
* The XPath 2.0 distinct-values() function
*/

public class DistinctValues extends CollatingFunction {

    private transient AtomicComparer sortComparer;

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        if (collation != null) {
            int type = argument[0].getItemType(env.getConfiguration().getTypeHierarchy()).getPrimitiveType();
            sortComparer = AtomicSortComparer.makeSortComparer(
                    collation, type, env.makeEarlyEvaluationContext());
        }
    }

    /**
    * Evaluate the function to return an iteration of selected values or nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        AtomicComparer comp = sortComparer;
        if (comp == null) {
            int type = argument[0].getItemType(context.getConfiguration().getTypeHierarchy()).getPrimitiveType();
            comp = getAtomicSortComparer(type, context);
        }
        SequenceIterator iter = argument[0].iterate(context);
        return new DistinctIterator(iter, comp);
    }

    /**
    * Get a SortComparer that can be used to compare values
    * @param type the static item type of the first argument
    * @param context The dynamic evaluation context.
    */

    private AtomicComparer getAtomicSortComparer(int type, XPathContext context) throws XPathException {
        final Comparator collator = getCollator(1, context);
        AtomicComparer asc = AtomicSortComparer.makeSortComparer(collator, type, context);
        return asc;
    }

    /**
     * Iterator class to return the distinct values in a sequence
     */

    public static class DistinctIterator implements SequenceIterator {

        private SequenceIterator base;
        private AtomicComparer comparer;
        private int position;
        private AtomicValue current;
        private HashSet lookup = new HashSet(40);

        /**
         * Create an iterator over the distinct values in a sequence
         * @param base the input sequence. This must return atomic values only.
         * @param comparer The comparer used to obtain comparison keys from each value;
         * these comparison keys are themselves compared using equals().
         */

        public DistinctIterator(SequenceIterator base, AtomicComparer comparer) {
            this.base = base;
            this.comparer = comparer;
            position = 0;
        }

        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next item, or null if there are no more items.
         * @throws org.orbeon.saxon.trans.XPathException
         *          if an error occurs retrieving the next item
         */

        public Item next() throws XPathException {
            while (true) {
                AtomicValue nextBase = (AtomicValue)base.next();
                if (nextBase==null) {
                    current = null;
                    position = -1;
                    return null;
                }
                ComparisonKey key = comparer.getComparisonKey(nextBase);
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
         * @throws org.orbeon.saxon.trans.XPathException
         *          if any error occurs
         */

        public SequenceIterator getAnother() throws XPathException {
            return new DistinctIterator(base.getAnother(), comparer);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
         *         and {@link #LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return 0;
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
