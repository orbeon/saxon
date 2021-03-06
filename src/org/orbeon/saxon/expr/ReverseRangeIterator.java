package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.LookaheadIterator;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Int64Value;

/**
* Iterator that produces numeric values in a monotonic sequence,
* ascending or descending. Although a range expression (N to M) is always
 * in ascending order, applying the reverse() function will produce
 * a RangeIterator that works in descending order.
*/

public class ReverseRangeIterator implements SequenceIterator,
        ReversibleIterator, LastPositionFinder, LookaheadIterator {

    long start;
    long currentValue;
    long limit;

    /**
     * Create an iterator over a range of integers in monotonic descending order
     * @param start the first integer to be delivered (the highest in the range)
     * @param end the last integer to be delivered (the lowest in the range). Must be <= start
     */

    public ReverseRangeIterator(long start, long end) {
        this.start = start;
        currentValue = start + 1;
        limit = end;
    }

    public boolean hasNext() {
        return currentValue > limit;
    }

    public Item next() {
        if (--currentValue < limit) {
            return null;
        }
        return Int64Value.makeIntegerValue(currentValue);
    }

    public Item current() {
        if (currentValue < limit) {
            return null;
        } else {
            return Int64Value.makeIntegerValue(currentValue);
        }
    }

    public int position() {
        if (currentValue < limit) {
            return -1;
        } else {
            return (int)(start - currentValue + 1);
        }
    }

    public void close() {
    }

    public int getLastPosition() {
        return (int)((start - limit) + 1);
    }

    public SequenceIterator getAnother() throws XPathException {
        return new ReverseRangeIterator(start, limit);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link org.orbeon.saxon.om.SequenceIterator#GROUNDED}, {@link org.orbeon.saxon.om.SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link org.orbeon.saxon.om.SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return LOOKAHEAD | LAST_POSITION_FINDER;
    }

    public SequenceIterator getReverseIterator() {
        return new RangeIterator(limit, start);
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
