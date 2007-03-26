package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.LookaheadIterator;
import org.orbeon.saxon.om.GroundedIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.value.IntegerValue;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.IntegerRange;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.trans.XPathException;

/**
* Iterator that produces numeric values in a monotonic sequence,
* ascending or descending. Although a range expression (N to M) is always
 * in ascending order, applying the reverse() function will produce
 * a RangeIterator that works in descending order.
*/

public class RangeIterator implements SequenceIterator,
                                      ReversibleIterator,
                                      LastPositionFinder,
        LookaheadIterator,
        GroundedIterator  {

    long start;
    long currentValue;
    int increment;   // always +1 or -1
    long limit;

    public RangeIterator(long start, long end) {
        this.start = start;
        increment = (start <= end ? +1 : -1);
        currentValue = start;
        limit = end;
    }

    public boolean hasNext() {
        if (increment>0) {
            return currentValue <= limit;
        } else if (increment<0) {
            return currentValue >= limit;
        } else {
            return false;
        }
    }

    public Item next() {
        if (!hasNext()) {
            increment = 0;
            return null;
        }
        long d = currentValue;
        currentValue += increment;
        return new IntegerValue(d);
    }

    public Item current() {
        if (increment == 0) {
            return null;
        } else {
            return new IntegerValue(currentValue - increment);
        }
    }

    public int position() {
        if (increment > 0) {
            return (int)(currentValue - start);
        } else if (increment < 0) {
            return (int)(start - currentValue);
        } else {
            return -1;
        }
    }

    public int getLastPosition() {
        return (int)((limit - start) * increment + 1);
    }

    public SequenceIterator getAnother() throws XPathException {
        return new RangeIterator(start, limit);
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
        int props = LOOKAHEAD | LAST_POSITION_FINDER;
        if (increment == 1) {
            props |= GROUNDED;
        }
        return props;
    }

    public SequenceIterator getReverseIterator() {
        return new RangeIterator(limit, start);
    }

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator. This should be an "in-memory" value, not a Closure.
     *
     * @return the corresponding Value
     */

    public Value materialize() throws XPathException {
        if (increment == 1) {
            return new IntegerRange(start, limit);
        } else {
            return new SequenceExtent(getAnother());
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
