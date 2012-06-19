package org.orbeon.saxon.evpull;

import org.orbeon.saxon.trans.XPathException;

/**
 * This class is a filter for a sequence of pull events; it returns the input sequence unchanged,
 * but traces execution to System.err
 */
public class TracingEventIterator implements EventIterator {

    private EventIterator base;

    /**
     * Create an event iterator that traces all events received from the base sequence, and returns
     * them unchanged
     * @param base the iterator over the base sequence
     */

    public TracingEventIterator(EventIterator base) {
        this.base = base;
    }


    /**
     * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
     * nested event iterators
     *
     * @return true if the next() method is guaranteed never to return an EventIterator
     */

    public boolean isFlatSequence() {
        return base.isFlatSequence();
    }

    /**
     * Get the next event in the sequence
     *
     * @return the next event, or null when the sequence is exhausted. Note that since an EventIterator is
     *         itself a PullEvent, this method may return a nested iterator.
     * @throws org.orbeon.saxon.trans.XPathException
     *          if a dynamic evaluation error occurs
     */

    public PullEvent next() throws XPathException {
        PullEvent next = base.next();
        if (next == null) {
            System.err.println("EVPULL end-of-sequence");
        } else {
            System.err.println("EVPULL " + next.getClass().getName());
        }
        return next;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

