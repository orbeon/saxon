package net.sf.saxon.om;
import net.sf.saxon.expr.LastPositionFinder;

import java.util.List;

/**
* Class ListIterator, iterates over a sequence of items held in a Java ArrayList,
* or indeed in any other kind of List
*/

public final class ListIterator
        implements AxisIterator, LastPositionFinder, LookaheadIterator {

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

    public boolean hasNext() {
        return index<length;
    }

    public Item next() {
        if (index >= length) return null;
        current = (Item)list.get(index++);
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return index;
    }

    public int getLastPosition() {
        return length;
    }

    public SequenceIterator getAnother() {
        return new ListIterator(list);
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

