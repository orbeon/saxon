package net.sf.saxon.om;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.expr.ReversibleIterator;

/**
 * EmptyIterator: an iterator over an empty sequence. Since such an iterator has no state,
 * only one instance is required; therefore a singleton instance is available via the static
 * getInstance() method.
 */

public class EmptyIterator implements AxisIterator,
        ReversibleIterator, LastPositionFinder {

    private static EmptyIterator theInstance = new EmptyIterator();

    /**
     * Get an EmptyIterator, an iterator over an empty sequence.
     * @return an EmptyIterator (in practice, this always returns the same
     *     one)
     */
    public static EmptyIterator getInstance() {
        return theInstance;
    }

    /**
     * Get the next item. This method should not be called unless hasNext() returns true.
     * @return the next item. For the EmptyIterator this is always null.
     */
    public Item next() {
        return null;
    }

    /**
     * Get the current item, that is, the item returned by the most recent call of next().
     * @return the current item. For the EmptyIterator this is always null.
     */
    public Item current() {
        return null;
    }

    /**
     * Get the position of the current item.
     * @return the position of the current item. For the EmptyIterator this is always zero
     * (whether or not the next() method has been called).
     */
    public int position() {
        return 0;
    }

    /**
     * Get the position of the last item in the sequence.
     * @return the position of the last item in the sequence, always zero in
     *     this implementation
     */
    public int getLastPosition() {
        return 0;
    }

    /**
     * Get another iterator over the same items, positioned at the start.
     * @return another iterator over an empty sequence (in practice, it
     *     returns the same iterator each time)
     */
    public SequenceIterator getAnother() {
        return theInstance;
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

    //public void setIsAtomizing(boolean atomizing) {}

    /**
     * Get another iterator over the same items, in reverse order.
     * @return a reverse iterator over an empty sequence (in practice, it
     *     returns the same iterator each time)
     */
    public SequenceIterator getReverseIterator() {
        return theInstance;
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
