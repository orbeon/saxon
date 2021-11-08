package org.orbeon.saxon.s9api;

import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

import java.util.Iterator;

/**
 * An iterator over an XPath sequence.
 *
 * <p>This class implements the standard Java Iterator interface.</p>
 *
 * <p>Because the <code>Iterator</code> interface does not define any checked
 * exceptions, the <code>hasNext()</code> method of this iterator throws an unchecked
 * exception if a dynamic error occurs while evaluating the expression. Applications
 * wishing to control error handling should take care to catch this exception.</p>
 */
public class XdmSequenceIterator implements Iterator<XdmItem> {

    private XdmItem next = null;
    private int state = BEFORE_ITEM;
    private SequenceIterator base;

    private final static int BEFORE_ITEM = 0;
    private final static int ON_ITEM = 1;
    private final static int FINISHED = 2;

    protected XdmSequenceIterator(SequenceIterator base) {
        this.base = base;
        this.state = BEFORE_ITEM;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     *
     * @throws SaxonApiUncheckedException if a dynamic error occurs during XPath evaluation that
     * is detected at this point.
     */
    public boolean hasNext() throws SaxonApiUncheckedException {
        switch (state) {
            case ON_ITEM:
                return true;
            case FINISHED:
                return false;
            case BEFORE_ITEM:
                try {
                    next = XdmItem.wrapItem(base.next());
                    if (next == null) {
                        state = FINISHED;
                        return false;
                    } else {
                        state = ON_ITEM;
                        return true;
                    }
                } catch (XPathException err) {
                    throw new SaxonApiUncheckedException(err);
                }
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Returns the next element in the iteration.  Calling this method
     * repeatedly until the {@link #hasNext()} method returns false will
     * return each element in the underlying collection exactly once.
     *
     * @return the next element in the iteration.
     * @throws java.util.NoSuchElementException
     *          iteration has no more elements.
     */
    public XdmItem next() {
        switch (state) {
            case ON_ITEM:
                state = BEFORE_ITEM;
                return next;
            case FINISHED:
                throw new java.util.NoSuchElementException();
            case BEFORE_ITEM:
                if (hasNext()) {
                    state = BEFORE_ITEM;
                    return next;
                } else {
                    throw new java.util.NoSuchElementException();
                }
            default:
                throw new IllegalStateException();    
        }

    }

    /**
     * Not supported on this implementation.
     *
     * @throws UnsupportedOperationException always
     */

    public void remove() {
        throw new UnsupportedOperationException();
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

