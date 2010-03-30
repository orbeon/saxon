package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

/**
 * EveryItemMappingIterator applies a mapping function to each item in a sequence.
 * The mapping function always returns a single item (never null)
 * <p/>
 * This is a specialization of the more general MappingIterator class, for use
 * in cases where a single input item always maps to exactly one output item
 */

public final class EveryItemMappingIterator implements SequenceIterator {

    private SequenceIterator base;
    private ItemMappingFunction action;
    private Item current = null;

    /**
     * Construct an ItemMappingIterator that will apply a specified ItemMappingFunction to
     * each Item returned by the base iterator.
     *
     * @param base   the base iterator
     * @param action the mapping function to be applied
     */

    public EveryItemMappingIterator(SequenceIterator base, ItemMappingFunction action) {
        this.base = base;
        this.action = action;
    }

    public Item next() throws XPathException {
        Item nextSource = base.next();
        if (nextSource == null) {
            current = null;
            return null;
        }
        // Call the supplied mapping function
        current = action.map(nextSource);
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return base.position();
    }

    public void close() {
        base.close();
    }

    public SequenceIterator getAnother() throws XPathException {
        return new EveryItemMappingIterator(base.getAnother(), action);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link org.orbeon.saxon.om.SequenceIterator#GROUNDED},
     *         {@link org.orbeon.saxon.om.SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link org.orbeon.saxon.om.SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
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
