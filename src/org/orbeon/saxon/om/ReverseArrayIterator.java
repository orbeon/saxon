package org.orbeon.saxon.om;

import org.orbeon.saxon.expr.LastPositionFinder;


/**
  * ReverseArrayIterator is used to enumerate items held in an array in reverse order.
  * @author Michael H. Kay
  */


public class ReverseArrayIterator implements UnfailingIterator,
        org.orbeon.saxon.expr.ReversibleIterator,
                                                   LookaheadIterator,
                                                   LastPositionFinder {

    Item[] items;
    int index = 0;
    int start;
    int end;         // item after the last to be output
    Item current = null;

    /**
     * Create an iterator a slice of an array
     * @param items The array of items
     * @param start The first item in the array to be be used (this will be the last
     * one in the resulting iteration). Zero-based.
     * @param end The item after the last one in the array to be used (this will be the
     * first one to be returned by the iterator). Zero-based.
    */

    public ReverseArrayIterator(Item[] items, int start, int end) {
        this.items = items;
        this.end = end;
        this.start = start;
        index = end - 1;
    }

    /**
     * Determine whether there are more items to come. Note that this operation
     * is stateless and it is not necessary (or usual) to call it before calling
     * next(). It is used only when there is an explicit need to tell if we
     * are at the last element.
     *
     * @return true if there are more items in the sequence
     */

    public boolean hasNext() {
        return index >= start;
    }

    public Item next() {
        if (index >= start) {
            current = items[index--];
            return current;
        } else {
            current = null;
            return null;
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        if (index < start-1) {
            return -1;  // position() returns -1 after next() returns null
        }
        return end - 1 - index;
    }

    public int getLastPosition() {
        return end - start;
    }

    public void close() {
    }

    public SequenceIterator getAnother() {
        return new ReverseArrayIterator(items, start, end);
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
        return LAST_POSITION_FINDER;
    }

    /**
     * Get an iterator that processes the same items in reverse order.
     * Since this iterator is processing the items backwards, this method
     * returns an ArrayIterator that processes them forwards.
     * @return a new ArrayIterator
     */

    public SequenceIterator getReverseIterator() {
        return new ArrayIterator(items, start, end);
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
