package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.AtomicValue;

/**
* This iterator returns a sequence of atomic values, the result of atomizing the sequence
 * of nodes returned by an underlying AxisIterator.
*/

public final class AxisAtomizingIterator implements SequenceIterator {

    private AxisIterator base;
    private SequenceIterator results = null;
    private AtomicValue current = null;
    private int position = 0;

    /**
    * Construct an atomizing iterator
    * @param base the base iterator (whose nodes are to be atomized)
     */

    public AxisAtomizingIterator(AxisIterator base) {
        this.base = base;
    }

    public Item next() throws XPathException {
        AtomicValue nextItem;
        while (true) {
            if (results != null) {
                nextItem = (AtomicValue)results.next();
                if (nextItem != null) {
                    break;
                } else {
                    results = null;
                }
            }
            // Avoid calling next() to materialize the NodeInfo object
            if (base.moveNext()) {
                Value atomized = base.atomize();
                if (atomized instanceof AtomicValue) {
                    // common case (the atomized value of the node is a single atomic value)
                    results = null;
                    nextItem = (AtomicValue)atomized;
                    break;
                } else {
                    results = atomized.iterate();
                    nextItem = (AtomicValue)results.next();
                    if (nextItem == null) {
                        results = null;
                    } else {
                        break;
                    }
                }
                // now go round the loop to get the next item from the base sequence
            } else {
                results = null;
                current = null;
                position = -1;
                return null;
            }
        }

        current = nextItem;
        position++;
        return nextItem;
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

    public SequenceIterator getAnother() {
        // System.err.println(this + " getAnother() ");
        AxisIterator newBase = (AxisIterator)base.getAnother();
        return new AxisAtomizingIterator(newBase);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
