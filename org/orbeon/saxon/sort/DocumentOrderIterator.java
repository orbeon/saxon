package org.orbeon.saxon.sort;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.xpath.XPathException;

/**
* DocumentOrderIterator takes as input an iteration of nodes in any order, and
* returns as output an iteration of the same nodes in document order, eliminating
* any duplicates.
*/

public final class DocumentOrderIterator implements SequenceIterator, Sortable {

    private SequenceIterator iterator;
    private SequenceExtent sequence;
    private NodeOrderComparer comparer;
    private NodeInfo current = null;
    private int position = 0;

    /**
    * Iterate over a sequence in document order.
    */

    public DocumentOrderIterator(SequenceIterator base, NodeOrderComparer comparer) throws XPathException {

        this.comparer = comparer;

        sequence = new SequenceExtent(base);
        // System.err.println("sort into document order: sequence length = " + sequence.getLength());
        if (sequence.getLength()>1) {
            QuickSort.sort(this, 0, sequence.getLength()-1);
        }
        iterator = sequence.iterate(null);
    }

    /**
    * Private constructor used only by getAnother()
    */

    private DocumentOrderIterator() {}

    /**
    * Compare two nodes in document sequence
    * (needed to implement the Sortable interface)
    */

    public int compare(int a, int b) {
        //System.err.println("compare " + a + " with " + b);
        return comparer.compare((NodeInfo)sequence.itemAt(a),
                                (NodeInfo)sequence.itemAt(b));
    }

    /**
    * Swap two nodes (needed to implement the Sortable interface)
    */

    public void swap(int a, int b) {
        sequence.swap(a, b);
    }

    // Implement the SequenceIterator as a wrapper around the underlying iterator
    // over the sequenceExtent, but looking ahead to remove duplicates.

    public Item next() throws XPathException {
        while (true) {
            NodeInfo next = (NodeInfo)iterator.next();
            if (next == null) {
                return null;
            }
            if (current != null && next.isSameNodeInfo(current)) {
                continue;
            } else {
                position++;
                current = next;
                return current;
            }
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        DocumentOrderIterator another = new DocumentOrderIterator();
        another.iterator = iterator.getAnother();    // don't need to sort it again
        return another;
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
