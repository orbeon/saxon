package org.orbeon.saxon.evpull;

import org.orbeon.saxon.trans.XPathException;

import java.util.Stack;

/**
 * An EventStackIterator is an EventIterator that delivers a flat sequence of PullEvents
 * containing no nested EventIterators
 */
public class EventStackIterator implements EventIterator {

    private Stack eventStack = new Stack();

    /**
     * Factory method to create an iterator that flattens the sequence of PullEvents received
     * from a base iterator, that is, it returns an EventIterator that will never return any
     * nested iterators.
     * @param base the base iterator. Any nested EventIterator returned by the base iterator
     * will be flattened, recursively.
     */

    public static EventIterator flatten(EventIterator base) {
        if (base.isFlatSequence()) {
            return base;
        }
        return new EventStackIterator(base);
    }

    /**
     * Create a EventStackIterator that flattens the sequence of PullEvents received
     * from a base iterator
     * @param base the base iterator. Any nested EventIterator returned by the base iterator
     * will be flattened, recursively.
     */

    private EventStackIterator(EventIterator base) {
        eventStack.push(base);
    }

    /**
     * Get the next event in the sequence. This will never be an EventIterator.
     *
     * @return the next event, or null when the sequence is exhausted
     * @throws org.orbeon.saxon.trans.XPathException
     *          if a dynamic evaluation error occurs
     */

    public PullEvent next() throws XPathException {
        if (eventStack.isEmpty()) {
            return null;
        }
        EventIterator iter = (EventIterator)eventStack.peek();
        PullEvent next = iter.next();
        if (next == null) {
            eventStack.pop();
            return next();
        } else if (next instanceof EventIterator) {
            eventStack.push(next);
            return next();
        } else {
            return next;
        }
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

