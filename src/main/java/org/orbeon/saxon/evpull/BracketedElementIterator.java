package org.orbeon.saxon.evpull;

import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

/**
 * The class is an EventIterator that handles the events arising from an element constructor:
 * that is, the start/end event pair for the element node, bracketing a sequence of events for the
 * content of the element.
 *
 * <p>This class does not normalize the content (for example by merging adjacent text nodes). That is the job
 * of the {@link ComplexContentProcessor}.</p>
 *
 * <p>The event stream consumed by a BracketedElementIterator may contain freestanding attribute and namespace nodes.
 * The event stream delivered by a BracketedElementIterator, however, packages all attributes and namespaces as
 * part of the startElement event.</p>
 */
public class BracketedElementIterator implements EventIterator {

    private PullEvent start;
    private EventIterator content;
    private PullEvent pendingContent;
    private PullEvent end;
    private int state = INITIAL_STATE;

    private static final int INITIAL_STATE = 0;
    private static final int PROCESSING_FIRST_CHILD = 1;
    private static final int PROCESSING_REMAINING_CHILDREN = 2;
    private static final int REACHED_END_TAG = 3;
    private static final int EXHAUSTED = 4;

    /**
     * Constructor
     * @param start the StartElementEvent object
     * @param content iterator that delivers the content of the element
     * @param end the EndElementEvent object
     */

    public BracketedElementIterator(PullEvent start, EventIterator content, PullEvent end) {
        this.start = start;
        this.content = EventStackIterator.flatten(content);
        this.end = end;
        state = 0;
    }

    /**
     * Get the next event in the sequence
     * @return the next event, or null when the sequence is exhausted
     * @throws XPathException if a dynamic evaluation error occurs
     */

    public PullEvent next() throws XPathException {

        switch (state) {
            case INITIAL_STATE:
                while (true) {
                    PullEvent pe = content.next();
                    if (pe == null) {
                        pendingContent = null;
                        state = REACHED_END_TAG;
                        break;
                    } else if (pe instanceof NodeInfo) {
                        int k = ((NodeInfo)pe).getNodeKind();
                        if (k == Type.NAMESPACE) {
                            NamePool pool = ((NodeInfo)pe).getNamePool();
                            int nscode = pool.allocateNamespaceCode(
                                    ((NodeInfo)pe).getLocalPart(), ((NodeInfo)pe).getStringValue());
                            ((StartElementEvent)start).addNamespace(nscode);
                            continue;
                        } else if (k == Type.ATTRIBUTE) {
                            ((StartElementEvent)start).addAttribute((NodeInfo)pe);
                            continue;
                        } else if (k == Type.TEXT && ((NodeInfo)pe).getStringValueCS().length() == 0) {
                            // ignore a zero-length text node
                            continue;
                        }
                    }
                    pendingContent = pe;
                    state = PROCESSING_FIRST_CHILD;
                    break;
                }
                ((StartElementEvent)start).namespaceFixup();
                return start;

            case PROCESSING_FIRST_CHILD:
                state = PROCESSING_REMAINING_CHILDREN;                                     
                return pendingContent;

            case PROCESSING_REMAINING_CHILDREN:
                PullEvent pe = content.next();
                if (pe == null) {
                    state = EXHAUSTED;
                    return end;
                } else {
                    return pe;
                }

            case REACHED_END_TAG:
                state = EXHAUSTED;
                return end;

            case EXHAUSTED:
                return null;

            default:
                throw new AssertionError("BracketedEventIterator state " + state);
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

