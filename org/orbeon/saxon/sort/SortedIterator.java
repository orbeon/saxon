package org.orbeon.saxon.sort;
import org.orbeon.saxon.expr.LastPositionFinder;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.trace.Location;

import java.util.Comparator;

/**
* Class to do a sorted iteration
*/

public class SortedIterator implements SequenceIterator, LastPositionFinder, Sortable {

    // the items to be sorted
    protected SequenceIterator base;

    // the sort key definitions
    protected FixedSortKeyDefinition[] sortkeys;

    // The items and keys are read into an array (nodeKeys) for sorting. This
    // array contains one "record" representing each node: the "record" contains
    // first, the Item itself, then an entry for each of its sort keys, in turn;
    // the last sort key is the position of the Item in the original sequence.
    protected int recordSize;
    protected Object[] nodeKeys;

    // The number of items to be sorted. -1 means not yet known.
    protected int count = -1;

    // The next item to be delivered from the sorted iteration
    protected int index = 0;

    // The context for the evaluation of sort keys
    protected XPathContext context;
    private Comparator[] keyComparers;

    private SortedIterator(){}

    public SortedIterator(XPathContext context, SequenceIterator base,
                                FixedSortKeyDefinition[] sortkeys)
    throws XPathException {
        this.context = context.newMinorContext();
        this.context.setOriginatingConstructType(Location.SORT_KEY);
        this.base = base;
        this.sortkeys = sortkeys;
        recordSize = sortkeys.length + 2;

        keyComparers = new Comparator[sortkeys.length];
        for (int i=0; i<sortkeys.length; i++) {
            keyComparers[i] = sortkeys[i].getComparer(context);
        }

        // Avoid doing the sort until the user wants the first item. This is because
        // sometimes the user only wants to know whether the collection is empty.
    }

    /**
    * Get the next item, in sorted order
    */

    public Item next() throws XPathException {
        if (count<0) {
            doSort();
        }
        if (index < count) {
            return (Item)nodeKeys[(index++)*recordSize];
        } else {
            return null;
        }
    }

    public Item current() {
        return (Item)nodeKeys[(index-1)*recordSize];
    }

    public int position() {
        return index;
    }

    public int getLastPosition() throws XPathException {
        if (count<0) {
            doSort();
        }
        return count;
    }

    public SequenceIterator getAnother() throws XPathException {
        // make sure the sort has been done, so that multiple iterators over the
        // same sorted data only do the sorting once.
        if (count<0) {
            doSort();
        }
        SortedIterator s = new SortedIterator();
        // the new iterator is the same as the old ...
        s.base = base;
        s.sortkeys = sortkeys;
        s.recordSize = recordSize;
        s.nodeKeys = nodeKeys;
        s.count = count;
        s.context = context;
        s.keyComparers = keyComparers;
        // ... except for its start position.
        s.index = 0;
        return s;
    }

    protected void buildArray() throws XPathException {
        int allocated;
        if (base instanceof LastPositionFinder) {
            allocated = ((LastPositionFinder)base).getLastPosition();
        } else {
            allocated = 100;
        }

        nodeKeys = new Object[allocated * recordSize];
        count = 0;

        XPathContext c2 = context.newMinorContext();
        c2.setOriginatingConstructType(Location.SORT_KEY);
        c2.setCurrentIterator(base);

        // initialise the array with data

        while (true) {
            Item item = base.next();
            if (item == null) {
                break;
            }
            if (count==allocated) {
                allocated *= 2;
                Object[] nk2 = new Object[allocated * recordSize];
                System.arraycopy(nodeKeys, 0, nk2, 0, count * recordSize);
                nodeKeys = nk2;
            }
            int k = count*recordSize;
            nodeKeys[k] = item;
            for (int n=0; n<sortkeys.length; n++) {
                nodeKeys[k+n+1] = sortkeys[n].getSortKey().evaluateItem(c2);
            }
            // make the sort stable by adding the record number
            nodeKeys[k+sortkeys.length+1] = new Integer(count);
            count++;
        }
        //diag();
    }

//    private void diag() {
//        System.err.println("Diagnostic print of keys");
//        for (int i=0; i<(count*recordSize); i++) {
//            System.err.println(i + " : " + nodeKeys[i]);
//        }
//    }


    private void doSort() throws XPathException {
        buildArray();
        if (count<2) return;

        // sort the array

        QuickSort.sort(this, 0, count-1);
    }

    /**
    * Compare two items in sorted sequence
    * (needed to implement the Sortable interface)
    */

    public int compare(int a, int b) {
        int a1 = a*recordSize + 1;
        int b1 = b*recordSize + 1;
        for (int i=0; i<sortkeys.length; i++) {
            Comparator comparator = keyComparers[i];
            int comp;
            // System.err.println("Comparing " + nodeKeys[a1+i] + " with " + nodeKeys[b1+i]);
            if (nodeKeys[a1+i] == null) {
                // first sort key value is ()
                comp = (nodeKeys[b1+i]==null ? 0 :
                          (sortkeys[i].getEmptyFirst() ? -1 : +1));
            } else if (nodeKeys[b1+i]==null) {
                // second sort key value is ()
                comp = (sortkeys[i].getEmptyFirst() ? +1 : -1);
            } else {
                comp = comparator.compare(nodeKeys[a1+i], nodeKeys[b1+i]);
            }
            if (comp!=0) return comp;
        }

        // all sort keys equal: return the items in their original order

        return ((Integer)nodeKeys[a1+sortkeys.length]).intValue() -
                ((Integer)nodeKeys[b1+sortkeys.length]).intValue();
    }

    /**
    * Swap two items (needed to implement the Sortable interface)
    */

    public void swap(int a, int b) {
        int a1 = a*recordSize;
        int b1 = b*recordSize;
        for (int i=0; i<recordSize; i++) {
            Object temp = nodeKeys[a1+i];
            nodeKeys[a1+i] = nodeKeys[b1+i];
            nodeKeys[b1+i] = temp;
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
