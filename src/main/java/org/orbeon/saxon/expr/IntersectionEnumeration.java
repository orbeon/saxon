package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.sort.NodeOrderComparer;
import org.orbeon.saxon.trans.XPathException;

/**
* An enumeration representing a nodeset that is an intersection of two other NodeSets.
* This implements the XPath 2.0 operator "intersect".
*/


public class IntersectionEnumeration implements SequenceIterator {

    private SequenceIterator e1;
    private SequenceIterator e2;
    private NodeInfo nextNode1 = null;
    private NodeInfo nextNode2 = null;
    private NodeOrderComparer comparer;

    private NodeInfo current = null;
    private int position = 0;

    /**
    * Form an enumeration of the intersection of the nodes in two nodesets
    * @param p1 the first operand: must be in document order
    * @param p2 the second operand: must be in document order
    * @param comparer Comparer to be used for putting nodes in document order
    */

    public IntersectionEnumeration(SequenceIterator p1, SequenceIterator p2,
                                    NodeOrderComparer comparer ) throws XPathException {
        e1 = p1;
        e2 = p2;
        this.comparer = comparer;

        // move to the first node in each input nodeset

        nextNode1 = next(e1);
        nextNode2 = next(e2);
    }

    /**
     * Get the next item from one of the input sequences,
     * checking that it is a node.
     * @param iter the iterator from which the next item is to be taken
     * @return the next value returned by that iterator
    */

    private NodeInfo next(SequenceIterator iter) throws XPathException {
        return (NodeInfo)iter.next();
        // rely on type-checking to prevent a ClassCastException
    }

    public Item next() throws XPathException {
        // main merge loop: iterate whichever sequence has the lower value, returning when a pair
        // is found that match.

        if (nextNode1 == null || nextNode2 == null) {
            current = null;
            position = -1;
            return null;
        }

        while (nextNode1 != null && nextNode2 != null) {
            int c = comparer.compare(nextNode1, nextNode2);
            if (c<0) {
                nextNode1 = next(e1);
            } else if (c>0) {
                nextNode2 = next(e2);
            } else {            // keys are equal
                current = nextNode2;    // which is the same as nextNode1
                nextNode2 = next(e2);
                nextNode1 = next(e1);
                position++;
                return current;
            }
        }
        return null;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        e1.close();
        e2.close();
    }

    public SequenceIterator getAnother() throws XPathException {
        return new IntersectionEnumeration(e1.getAnother(), e2.getAnother(), comparer);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link SequenceIterator#GROUNDED}, {@link SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
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
