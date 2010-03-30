package org.orbeon.saxon.evpull;

import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.VirtualUntypedCopy;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

import java.util.Iterator;

/**
 * This class is an EventIterator that filters a stream of pull events setting
 * the type annotation on element nodes to xs:untyped and on attribute nodes to
 * xs:untypedAtomic
 */
public class EventAnnotationStripper implements EventIterator {

    private EventIterator base;

    /**
     * Create an EventAnnotationStripper
     * @param base the stream of events whose type annotations are to be stripped (set to untyped)
     */

    public EventAnnotationStripper(EventIterator base) {
        this.base = EventStackIterator.flatten(base);
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
        PullEvent pe = base.next();
        if (pe instanceof StartElementEvent) {
            StartElementEvent see = (StartElementEvent)pe;
            see.stripTypeAnnotations();
            return see;
        } else if (pe instanceof NodeInfo) {
            // Make a virtual untyped copy of the node
            switch (((NodeInfo)pe).getNodeKind()) {
                case Type.ELEMENT:
                case Type.ATTRIBUTE:
                    return VirtualUntypedCopy.makeVirtualUntypedCopy((NodeInfo)pe, (NodeInfo)pe);
                default:
                    return pe;
            }
        } else {
            return pe;
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

