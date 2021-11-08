package org.orbeon.saxon.evpull;

import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.evpull.PullEvent;

/**
 * This class is an EventIterator over an empty sequence. It is a singleton class.
 */
public class EmptyEventIterator implements EventIterator {

    private static EmptyEventIterator THE_INSTANCE = new EmptyEventIterator();

    /**
     * Get the singular instance of this class
     * @return the singular instance
     */

    public static EmptyEventIterator getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Get the next event in the sequence
     * @return null (there is never a next event)
     */

    public PullEvent next() {
        return null;
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

