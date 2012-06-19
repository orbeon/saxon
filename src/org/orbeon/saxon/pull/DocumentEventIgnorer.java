package org.orbeon.saxon.pull;

import org.orbeon.saxon.trans.XPathException;

/**
 * This is a filter that can be added to a pull pipeline to remove START_DOCUMENT and END_DOCUMENT
 * events.
 */
public class DocumentEventIgnorer extends PullFilter {

    public DocumentEventIgnorer(PullProvider base) {
        super(base);
    }

    /**
     * Get the next event.
     * <p/>
     * <p>Note that a subclass that overrides this method is responsible for ensuring
     * that current() works properly. This can be achieved by setting the field
     * currentEvent to the event returned by any call on next().</p>
     *
     * @return an integer code indicating the type of event. The code
     *         {@link #END_OF_INPUT} is returned at the end of the sequence.
     */

    public int next() throws XPathException {
        do {
            currentEvent = super.next();
        } while (currentEvent == START_DOCUMENT || currentEvent == END_DOCUMENT);
        return currentEvent;
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
