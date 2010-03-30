package org.orbeon.saxon.om;
import org.orbeon.saxon.trans.XPathException;

/**
 * A SequenceIterator is used to iterate over any XPath 2 sequence (of values or nodes).
 * To get the next item in a sequence, call next(); if this returns null, you've
 * reached the end of the sequence.
 * <p>
 * A SequenceIterator keeps track of the current Item and the current position.
 * The objects returned by the SequenceIterator will always be either nodes
 * (class NodeInfo) or singleton values (class AtomicValue): these are represented
 * collectively by the interface Item.
 * <p>
 * This interface forms part of the public Saxon API. The JavaDoc "since" flag is used from
 * release 8.4 onwards to indicate methods that are considered to be a stable part
 * of the API. Methods without a "since" flag should not be regarded as a stable part
 * of the API.
 * <p>
 * Note that the stability of this interface applies to classes that use the interface,
 * not to classes that implement it. The interface may be extended in future to add new methods.
 *
 * @author Michael H. Kay
 * @since 8.4
 */

public interface SequenceIterator {

    /**
     * Get the next item in the sequence. This method changes the state of the
     * iterator, in particular it affects the result of subsequent calls of
     * position() and current().
     * @throws XPathException if an error occurs retrieving the next item
     * @return the next item, or null if there are no more items. Once a call
     * on next() has returned null, no further calls should be made. The preferred
     * action for an iterator if subsequent calls on next() are made is to return
     * null again, and all implementations within Saxon follow this rule.
     * @since 8.4
     */

    public Item next() throws XPathException;

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next(). This method does not change the state of the iterator.
     *
     * @return the current item, the one most recently returned by a call on
     *     next(). Returns null if next() has not been called, or if the end
     *     of the sequence has been reached.
     * @since 8.4
     */

    public Item current();

    /**
     * Get the current position. This will usually be zero before the first call
     * on next(), otherwise it will be the number of times that next() has
     * been called. Once next() has returned null, the preferred action is
     * for subsequent calls on position() to return -1, but not all existing
     * implementations follow this practice. (In particular, the EmptyIterator
     * is stateless, and always returns 0 as the value of position(), whether
     * or not next() has been called.)
     * <p>
     * This method does not change the state of the iterator.
     *
     * @return the current position, the position of the item returned by the
     *     most recent call of next(). This is 1 after next() has been successfully
     *     called once, 2 after it has been called twice, and so on. If next() has
     *     never been called, the method returns zero. If the end of the sequence
     *     has been reached, the value returned will always be <= 0; the preferred
     *     value is -1.
     *
     * @since 8.4
     */

    public int position();

    /**
     * Close the iterator. This indicates to the supplier of the data that the client
     * does not require any more items to be delivered by the iterator. This may enable the
     * supplier to release resources. After calling close(), no further calls on the
     * iterator should be made; if further calls are made, the effect of such calls is undefined.
     *
     * <p>(Currently, closing an iterator is important only when the data is being "pushed" in
     * another thread. Closing the iterator terminates that thread and means that it needs to do
     * no additional work. Indeed, failing to close the iterator may cause the push thread to hang
     * waiting for the buffer to be emptied.)</p>
     */

    public void close();

    /**
     * Get another SequenceIterator that iterates over the same items as the original,
     * but which is repositioned at the start of the sequence.
     * <p>
     * This method allows access to all the items in the sequence without disturbing the
     * current position of the iterator. Internally, its main use is in evaluating the last()
     * function.
     * <p>
     * This method does not change the state of the iterator.
     *
     * @exception XPathException if any error occurs
     * @return a SequenceIterator that iterates over the same items,
     *     positioned before the first item
     * @since 8.4
     */

    public SequenceIterator getAnother() throws XPathException;

    /**
     * Get properties of this iterator, as a bit-significant integer.
     * @return the properties of this iterator. This will be some combination of
     * properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     * and {@link #LOOKAHEAD}. It is always
     * acceptable to return the value zero, indicating that there are no known special properties.
     * It is acceptable for the properties of the iterator to change depending on its state.
     * @since 8.6
     */

    public int getProperties();

    /**
     * Property value: the iterator is "grounded". This means that (a) the
     * iterator must be an instance of {@link GroundedIterator}, and (b) the
     * implementation of the materialize() method must be efficient (in particular,
     * it should not involve the creation of new objects)
     */

    public static final int GROUNDED = 1;

    /**
     * Property value: the iterator knows the number of items that it will deliver.
     * This means that (a) the iterator must be an instance of {@link org.orbeon.saxon.expr.LastPositionFinder},
     * and (b) the implementation of the getLastPosition() method must be efficient (in particular,
     * it should take constant time, rather than time proportional to the length of the sequence)
     */

    public static final int LAST_POSITION_FINDER = 1<<1;

    /**
     * Property value: the iterator knows whether there are more items still to come. This means
     * that (a) the iterator must be an instance of {@link LookaheadIterator}, and (b) the
     * implementation of the hasNext() method must be efficient (more efficient than the client doing
     * it)
     */

    public static final int LOOKAHEAD = 1<<2;

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
