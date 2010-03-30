package org.orbeon.saxon.om;

import org.orbeon.saxon.expr.LastPositionFinder;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;

/**
 * EmptyIterator: an iterator over an empty sequence. Since such an iterator has no state,
 * only one instance is required; therefore a singleton instance is available via the static
 * getInstance() method.
 */

public class EmptyIterator implements AxisIterator,
        org.orbeon.saxon.expr.ReversibleIterator, LastPositionFinder, GroundedIterator, LookaheadIterator {

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
     * private constructor
     */

    private EmptyIterator() {}

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext() {
        return false;
    }


    /**
     * Get the next item.
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

    public void close() {
    }

    /**
     * Return an iterator over an axis, starting at the current node.
     *
     * @param axis the axis to iterate over, using a constant such as
     *             {@link Axis#CHILD}
     * @param test a predicate to apply to the nodes before returning them.
     * @throws NullPointerException if there is no current node
     */

    public AxisIterator iterateAxis(byte axis, NodeTest test) {
        throw new NullPointerException();
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        throw new NullPointerException();
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        throw new NullPointerException();
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

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return GROUNDED | LAST_POSITION_FINDER | LOOKAHEAD;
    }

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator. This should be an "in-memory" value, not a Closure.
     *
     * @return the corresponding Value
     */

    public GroundedValue materialize() {
        return EmptySequence.getInstance();
    }

    /**
     * Determine whether there are more items to come. Note that this operation
     * is stateless and it is not necessary (or usual) to call it before calling
     * next(). It is used only when there is an explicit need to tell if we
     * are at the last element.
     *
     * @return true if there are more nodes
     */

    public boolean hasNext() {
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
