package net.sf.saxon.expr;
import net.sf.saxon.om.ArrayIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;


/**
* A PositionIterator selects a subsequence of a sequence
*/

public class PositionIterator implements SequenceIterator, LookaheadIterator {

    /**
    * Static factory method. Creates a PositionIterator, unless the base Iterator is an
    * ArrayIterator, in which case it optimizes by creating a new ArrayIterator directly over the
    * underlying array. This optimization is important when doing recursion over a node-set using
    * repeated calls of $nodes[position()>1]
    * @param base   An iteration of the items to be filtered
    * @param min    The position of the first item to be included (base 1)
    * @param max    The position of the last item to be included (base 1)
    */

    public static SequenceIterator make(SequenceIterator base, int min, int max) throws XPathException {
        // System.err.println("PositionIterator.make base=" + base.getClass() + " min=" + min + " max=" + max);
        if (base instanceof ArrayIterator) {
            return ((ArrayIterator)base).makeSliceIterator(min, max);
        } else {
            return new PositionIterator(base, min, max);
        }
    }



    private SequenceIterator base;
    private int position = 0;
    private int min = 1;
    private int max = Integer.MAX_VALUE;
    private Item nextItem = null;
    private Item current = null;

    /**
    * Private Constructor: use the factory method instead!
    * @param base   An iteration of the items to be filtered
    * @param min    The position of the first item to be included
    * @param max    The position of the last item to be included
    */

    private PositionIterator(SequenceIterator base, int min, int max) throws XPathException {
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
    * Test whether there are any more items available in the enumeration
    */

    public boolean hasNext() {
        return nextItem != null;
    }

    /**
    * Get the next item if there is one
    */

    public Item next() throws XPathException {
        if (nextItem == null) {
            return null;
        }
        current = nextItem;
        position++;
        if (base.position() < max) {
            nextItem = base.next();
        } else {
            nextItem = null;
        }
        return current;
    }


    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    /**
    * Get another iterator to return the same nodes
    */

    public SequenceIterator getAnother() throws XPathException {
        return new PositionIterator(base.getAnother(), min, max);
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
