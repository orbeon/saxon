package org.orbeon.saxon.om;

import org.orbeon.saxon.expr.LastPositionFinder;
import org.orbeon.saxon.expr.ReversibleIterator;


/**
  * ReverseArrayIterator is used to enumerate items held in an array in reverse order.
  * @author Michael H. Kay
  */


public final class ReverseArrayIterator implements AxisIterator,
                                                   ReversibleIterator,
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

    public Item next() {
        if (index >= start) {
            current = items[index--];
            return current;
        } else {
            return null;
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        return end - 1 - index;
    }

    public int getLastPosition() {
        return end - start;
    }

    public SequenceIterator getAnother() {
        return new ReverseArrayIterator(items, start, end);
    }

    /**
     * Indicate that any nodes returned in the sequence will be atomized. This
     * means that if it wishes to do so, the implementation can return the typed
     * values of the nodes rather than the nodes themselves. The implementation
     * is free to ignore this hint.
     * @param atomizing true if the caller of this iterator will atomize any
     * nodes that are returned, and is therefore willing to accept the typed
     * value of the nodes instead of the nodes themselves.
     */

    //public void setIsAtomizing(boolean atomizing) {}

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
