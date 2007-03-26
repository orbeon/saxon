package org.orbeon.saxon.expr;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over a single object (typically a sub-expression of an expression)
 */
public class MonoIterator implements Iterator {

    private Object thing;  // the single object in the collection
    private boolean gone;  // true if the single object has already been returned

    public MonoIterator(Object thing) {
        this.gone = false;
        this.thing = thing;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */

    public boolean hasNext() {
        return !gone;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @exception NoSuchElementException iteration has no more elements.
     */

    public Object next() {
        if (gone) {
            throw new NoSuchElementException();
        } else {
            gone = true;
            return thing;
        }
    }

    /**
     *
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation).  This method can be called only once per
     * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     *
     * @exception UnsupportedOperationException if the <tt>remove</tt>
     *		  operation is not supported by this Iterator (which is the
     *        case for this iterator).
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s): Michael Kay
//
