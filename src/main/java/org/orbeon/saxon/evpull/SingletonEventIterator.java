package org.orbeon.saxon.evpull;

import org.orbeon.saxon.trans.XPathException;

/**
 * This class represents an EventIterator over a sequence containing a single pull event.
 */
public class SingletonEventIterator implements EventIterator {

    private PullEvent event;

    /**
     * Create an iterator over a sequence containing a single pull event
     * @param event the single event. This must not be an EventIterator
     */

    public SingletonEventIterator(PullEvent event) {
        this.event = event;
        if (event instanceof EventIterator) {
            throw new IllegalArgumentException("event");
        }
    }

    /**
     * Get the next event in the sequence
     *
     * @return the next event, or null when the sequence is exhausted
     * @throws XPathException if a dynamic evaluation error occurs
     */

    public PullEvent next() throws XPathException {
        PullEvent next = event;
        event = null;
        return next;
    }


    /**
     * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
     * nested event iterators
     *
     * @return true if the next() method is guaranteed never to return an EventIterator
     */

    public boolean isFlatSequence() {
        return true;
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

