package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.AtomizableIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

/**
* ContextMappingIterator merges a sequence of sequences into a single flat
* sequence. It takes as inputs an iteration, and a mapping function to be
* applied to each Item returned by that iteration. The mapping function itself
* returns another iteration. The result is an iteration of the concatenation of all
* the iterations returned by the mapping function.<p>
*
* This is a specialization of the MappingIterator class: it differs in that it
* sets each item being processed as the context item
*/

public final class ContextMappingIterator implements SequenceIterator, AtomizableIterator {

    private SequenceIterator base;
    private ContextMappingFunction action;
    private XPathContext context;
    private SequenceIterator results = null;
    private boolean atomizing = false;
    private Item current = null;
    private int position = 0;

    /**
    * Construct a ContextMappingIterator that will apply a specified ContextMappingFunction to
    * each Item returned by the base iterator.
     * @param action the mapping function to be applied
     * @param context the processing context. The mapping function is applied to each item returned
     * by context.getCurrentIterator() in turn.
     */

    public ContextMappingIterator(ContextMappingFunction action, XPathContext context) {
        this.base = context.getCurrentIterator();
        this.action = action;
        this.context = context;
    }

    public Item next() throws XPathException {
        Item nextItem;
        while (true) {
            if (results != null) {
                nextItem = results.next();
                if (nextItem != null) {
                    break;
                } else {
                    results = null;
                }
            }
            if (base.next() != null) {
                // Call the supplied mapping function
                results = action.map(context);

                if (atomizing && results instanceof AtomizableIterator) {
                    ((AtomizableIterator)results).setIsAtomizing(atomizing);
                }
                nextItem = results.next();
                if (nextItem == null) {
                    results = null;
                } else {
                    break;
                }

            } else {
                results = null;
                current = null;
                position = -1;
                return null;
            }
        }

        current = nextItem;
        position++;
        return nextItem;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }


    public SequenceIterator getAnother() throws XPathException {
        SequenceIterator newBase = base.getAnother();
        XPathContext c2 = context;
        if (c2!=null) {
            c2 = c2.newMinorContext();
            c2.setCurrentIterator(newBase);
            c2.setOrigin(context.getOrigin());
        }
        ContextMappingIterator m = new ContextMappingIterator(action, c2);
        m.setIsAtomizing(atomizing);
        return m;
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link org.orbeon.saxon.om.SequenceIterator#GROUNDED}, {@link org.orbeon.saxon.om.SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link org.orbeon.saxon.om.SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return ATOMIZABLE;
    }

    /**
     * Indicate that any nodes returned in the sequence will be atomized. This
     * means that if it wishes to do so, the implementation can return the typed
     * values of the nodes rather than the nodes themselves. The implementation
     * is free to ignore this hint.
     * @param atomizing true if the caller of this iterator will atomize any
     * nodes that are returned, and is therefore willing to accept the typed
     * value of the nodes instead of the nodes themselves.
     */

    public void setIsAtomizing(boolean atomizing) {
        this.atomizing = atomizing;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
