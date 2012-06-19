package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Value;


/**
* A SubsequenceIterator selects a subsequence of a sequence
*/

public class SubsequenceIterator implements SequenceIterator, LastPositionFinder, LookaheadIterator {

    private SequenceIterator base;
    private int position = 0;
    private int min;
    private int max;
    private Item nextItem = null;
    private Item current = null;

    /**
    * Private Constructor: use the factory method instead!
    * @param base   An iteration of the items to be filtered
    * @param min    The position of the first item to be included (1-based)
    * @param max    The position of the last item to be included (1-based)
    */

    private SubsequenceIterator(SequenceIterator base, int min, int max) throws XPathException {
        this.base = base;
        this.min = min;
        if (min<1) min=1;
        this.max = max;
        if (max<min) {
            nextItem = null;
            return;
        }
        int i=1;
        while ( i++ <= min ) {
            nextItem = base.next();
            if (nextItem == null) {
                break;
            }
        }
        current = nextItem;
    }

    /**
     * Static factory method. Creates a SubsequenceIterator, unless for example the base Iterator is an
     * ArrayIterator, in which case it optimizes by creating a new ArrayIterator directly over the
     * underlying array. This optimization is important when doing recursion over a node-set using
     * repeated calls of $nodes[position()>1]
     * @param base   An iteration of the items to be filtered
     * @param min    The position of the first item to be included (base 1)
     * @param max    The position of the last item to be included (base 1)
     * @return an iterator over the requested subsequence
    */
    
    public static SequenceIterator make(SequenceIterator base, int min, int max) throws XPathException {
        if (base instanceof ArrayIterator) {
            return ((ArrayIterator)base).makeSliceIterator(min, max);
        } else if (max == Integer.MAX_VALUE) {
            return TailIterator.make(base, min);
        } else if (base instanceof GroundedIterator && min > 4) {
            GroundedValue value = ((GroundedIterator)base).materialize();
            value = value.subsequence(min-1, max-min+1);
            return ((Value)value).iterate();
        } else {
            return new SubsequenceIterator(base, min, max);
        }
    }

    /**
    * Test whether there are any more items available in the sequence
    */

    public boolean hasNext() {
        return nextItem != null;
    }

    /**
    * Get the next item if there is one
    */

    public Item next() throws XPathException {
        if (nextItem == null) {
            current = null;
            position = -1;
            return null;
        }
        current = nextItem;
        position++;
        if (base.position() < max) {
            nextItem = base.next();
        } else {
            nextItem = null;
            base.close();
        }
        return current;
    }


    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        base.close();
    }

    /**
    * Get another iterator to return the same nodes
    */

    public SequenceIterator getAnother() throws XPathException {
        return new SubsequenceIterator(base.getAnother(), min, max);
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
        int p = LOOKAHEAD;
        p |= (base.getProperties() & LAST_POSITION_FINDER);
        return p;
    }

    /**
     * Get the last position (that is, the number of items in the sequence). This method is
     * non-destructive: it does not change the state of the iterator.
     * The result is undefined if the next() method of the iterator has already returned null.
     * This method must not be called unless the result of getProperties() on the iterator
     * includes the bit setting {@link #LAST_POSITION_FINDER}
     */

    public int getLastPosition() throws XPathException {
        int lastBase = ((LastPositionFinder)base).getLastPosition();
        int z = Math.min(lastBase, max);
        return Math.max(z - min + 1, 0);
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
