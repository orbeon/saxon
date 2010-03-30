package org.orbeon.saxon.evpull;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.evpull.PullEvent;

/**
 * Iterate over the instructions in a Block, concatenating the result of each instruction
 * into a single combined sequence.
 */

public class BlockEventIterator implements EventIterator {

    private Expression[] children;
    private int i = 0;
    private EventIterator child;
    private XPathContext context;

    /**
     * Create an EventIterator over the results of evaluating a Block
     * @param children the sequence of instructions comprising the Block
     * @param context the XPath dynamic context
     */

    public BlockEventIterator(Expression[] children, XPathContext context) {
        this.children = children;
        this.context = context;
    }

    /**
     * Get the next item in the sequence.
     *
     * @return the next item, or null if there are no more items.
     * @throws XPathException if an error occurs retrieving the next item
     */

    public PullEvent next() throws XPathException {
        while (true) {
            if (child == null) {
                child = children[i++].iterateEvents(context);
            }
            PullEvent current = child.next();
            if (current != null) {
                return current;
            }
            child = null;
            if (i >= children.length) {
                return null;
            }
        }
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

