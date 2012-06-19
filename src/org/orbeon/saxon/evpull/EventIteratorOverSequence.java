package org.orbeon.saxon.evpull;

import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

/**
 * This class maps a SequenceIterator to an EventIterator, by simply returning the items in the sequence
 * as PullEvents.
 */
public class EventIteratorOverSequence implements EventIterator {

    SequenceIterator base;

    /**
     * Create an EventIterator that wraps a given SequenceIterator
     * @param base the SequenceIterator to be wrapped
     */

    public EventIteratorOverSequence(SequenceIterator base) {
        this.base = base;
    }

    /**
     * Get the next PullEvent in the sequence
     * @return the next PullEvent
     * @throws XPathException in case of a dynamic error
     */

    public PullEvent next() throws XPathException {
        return base.next();
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

