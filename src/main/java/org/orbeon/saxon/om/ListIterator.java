package org.orbeon.saxon.om;
import org.orbeon.saxon.expr.LastPositionFinder;
import org.orbeon.saxon.value.SequenceExtent;

import java.util.List;

/**
* Class ListIterator, iterates over a sequence of items held in a Java ArrayList,
* or indeed in any other kind of List
*/

public class ListIterator
        implements UnfailingIterator, LastPositionFinder, LookaheadIterator, GroundedIterator {

    int index=0;
    int length;
    Item current = null;
    List list = null;

    /**
     * Create a ListIterator over a given List
     * @param list the list: all objects in the list must be instances of {@link Item}
     */

    public ListIterator(List list) {
        index = 0;
        this.list = list;
        this.length = list.size();
    }

   /**
     * Create a ListIterator over the leading part of a given List
     * @param list the list: all objects in the list must be instances of {@link Item}
     * @param length the number of items to be included
     */

    public ListIterator(List list, int length) {
        index = 0;
        this.list = list;
        this.length = length;
    }

    public boolean hasNext() {
        return index<length;
    }

    public Item next() {
        if (index >= length) {
            current = null;
            index = -1;
            length = -1;
            return null;
        }
        current = (Item)list.get(index++);
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return index;
    }

    public void close() {
    }

    public int getLastPosition() {
        return length;
    }

    public SequenceIterator getAnother() {
        return new ListIterator(list);
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
        return GROUNDED | LAST_POSITION_FINDER | LOOKAHEAD;
    }

    /**
     * Return a SequenceValue containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding SequenceValue
     */

    public GroundedValue materialize() {
        return new SequenceExtent(list);
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

