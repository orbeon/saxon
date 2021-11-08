package org.orbeon.saxon.evpull;

import org.orbeon.saxon.trans.XPathException;

/**
 * The class is an EventIterator that handles the events arising from a document node constructor:
 * that is, the start/end event pair for the document node, bracketing a sequence of events for the
 * content of the document.
 *
 * <p>This class does not normalize the content (for example by merging adjacent text nodes). That is the job
 * of the {@link ComplexContentProcessor}.</p>
 *
 */
public class BracketedDocumentIterator implements EventIterator {

    private EventIterator content;
    private int state = INITIAL_STATE;

    private static final int INITIAL_STATE = 0;
    private static final int PROCESSING_CHILDREN = 1;
    private static final int EXHAUSTED = 2;

    /**
     * Constructor
     * @param content iterator that delivers the content of the document
     */

    public BracketedDocumentIterator(EventIterator content) {
        this.content = EventStackIterator.flatten(content);
        state = 0;
    }

    /**
     * Get the next event in the sequence
     * @return the next event, or null when the sequence is exhausted
     * @throws org.orbeon.saxon.trans.XPathException if a dynamic evaluation error occurs
     */

    public PullEvent next() throws XPathException {

        switch (state) {
            case INITIAL_STATE:
                state = PROCESSING_CHILDREN;
                return StartDocumentEvent.getInstance();

            case PROCESSING_CHILDREN:
                PullEvent pe = content.next();
                if (pe == null) {
                    state = EXHAUSTED;
                    return EndDocumentEvent.getInstance();
                } else {
                    return pe;
                }

            case EXHAUSTED:
                return null;

            default:
                throw new AssertionError("BracketedDocumentIterator state " + state);
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

