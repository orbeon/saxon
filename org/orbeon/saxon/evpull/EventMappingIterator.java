package org.orbeon.saxon.evpull;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

/**
 * MappingIterator merges a sequence of sequences into a single sequence.
 * It takes as inputs an iteration, and a mapping function to be
 * applied to each Item returned by that iteration. The mapping function itself
 * returns another iteration. The result is an iteration of iterators. To convert this
 * int a single flat iterator over a uniform sequence of events, the result must be wrapped
 * in an {@link EventStackIterator}<p>
*/

public final class EventMappingIterator implements EventIterator {

    private SequenceIterator base;
    private EventMappingFunction action;

    /**
     * Construct a MappingIterator that will apply a specified MappingFunction to
     * each Item returned by the base iterator.
     * @param base the base iterator
     * @param action the mapping function to be applied
     */

    public EventMappingIterator(SequenceIterator base, EventMappingFunction action) {
        this.base = base;
        this.action = action;
    }


    public PullEvent next() throws XPathException {
        Item nextSource = base.next();
        return (nextSource == null ? null : action.map(nextSource));
    }

    /**
     * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
     * nested event iterators
     *
     * @return true if the next() method is guaranteed never to return an EventIterator
     */

    public boolean isFlatSequence() {
        return false;
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

