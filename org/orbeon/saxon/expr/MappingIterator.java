package net.sf.saxon.expr;
import net.sf.saxon.om.AtomizableIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
* MappingIterator merges a sequence of sequences into a single flat
* sequence. It takes as inputs an iteration, and a mapping function to be
* applied to each Item returned by that iteration. The mapping function itself
* returns another iteration. The result is an iteration of the concatenation of all
* the iterations returned by the mapping function.<p>
*
* This is a powerful class. It is used, with different mapping functions,
* in a great variety of ways. It underpins the way that "for" expressions and
* path expressions are evaluated, as well as sequence expressions. It is also
* used in the implementation of the document(), key(), and id() functions.
*/

public final class MappingIterator implements SequenceIterator, AtomizableIterator {

    private SequenceIterator base;
    private MappingFunction action;
    private XPathContext context;
    private Object info;
    private SequenceIterator results = null;
    private boolean atomizing = false;
    private Item current = null;
    private int position = 0;

    /**
    * Construct a MappingIterator that will apply a specified MappingFunction to
    * each Item returned by the base iterator.
    * @param base the base iterator
    * @param action the mapping function to be applied
    * @param context the processing context. This should be supplied only if each item to be processed
    * is to become the context item. In this case "base" should be the same as context.getCurrentIterator().
    * @param info an arbitrary object to be passed as a parameter to the mapping function
    * each time it is called
    */

    public MappingIterator(SequenceIterator base, MappingFunction action, XPathContext context, Object info) {
        // System.err.println("New Mapping iterator " + this + " with base " + base);
        this.base = base;
        this.action = action;
        this.context = context;
        this.info = info;
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
            Item nextSource = base.next();
            if (nextSource != null) {
                // Call the supplied mapping function
                Object obj = action.map(nextSource, context, info);

                // The result may be null (representing an empty sequence), an item
                // (representing a singleton sequence), or a SequenceIterator (any sequence)

                if (obj != null) {
                    if (obj instanceof Item) {
                        nextItem = (Item)obj;
                        results = null;
                        break;
                    }
                    results = (SequenceIterator)obj;
                    if (atomizing && results instanceof AtomizableIterator) {
                        ((AtomizableIterator)results).setIsAtomizing(atomizing);
                    }
                    nextItem = results.next();
                    if (nextItem == null) {
                        results = null;
                    } else {
                        break;
                    }
                }
                // now go round the loop to get the next item from the base sequence
            } else {
                results = null;
                return null;
            }
        }

        current = nextItem;
        // System.err.println("MappingIterator.next(), this = " + this + " returning " + ((net.sf.saxon.om.NodeInfo)current).generateId() );
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
        // System.err.println(this + " getAnother() ");
        SequenceIterator newBase = base.getAnother();
        XPathContext c = context;
        if (c!=null) {
            c = c.newMinorContext();
            c.setCurrentIterator(newBase);
            c.setOrigin(context.getOrigin());
        }
        MappingIterator m = new MappingIterator(newBase, action, c, info);
        m.setIsAtomizing(atomizing);
        return m;
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
