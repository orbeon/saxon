package net.sf.saxon.om;
import net.sf.saxon.trans.XPathException;

/**
 * <p>
 * A SequenceIterator is used to iterate over any XPath 2 sequence (of values or nodes).
 * To get the next item in a sequence, call next(); if this returns null, you've
 * reached the end of the sequence.</p>
 * <p>
 * A SequenceIterator keeps track of the current Item and the current position.
 * The objects returned by the SequenceIterator will always be either nodes
 * (class NodeInfo) or singleton values (class AtomicValue): these are represented
 * collectively by the interface Item.</p>
 */

public interface SequenceIterator {

    /**
     * Get the next item in the sequence. <BR>
     * @exception XPathException if an error occurs retrieving the next item
     * @return the next item, or null if there are no more items.
     */

    public Item next() throws XPathException;

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next().
     *
     * @return the current item, the one most recently returned by a call on
     *     next(); or null, if next() has not been called, or if the end
     *     of the sequence has been reached.
     */

    public Item current();

    /**
     * Get the current position. This will be zero before the first call
     * on next(), otherwise it will be the number of times that next() has
     * been called.
     *
     * @return the current position, the position of the item returned by the
     *     most recent call of next()
     */

    public int position();

    /**
     * Get another SequenceIterator that iterates over the same items as the original,
     * but which is repositioned at the start of the sequence.
     *
     * @exception XPathException if any error occurs
     * @return a SequenceIterator that iterates over the same items,
     *     positioned before the first item
     */

    public SequenceIterator getAnother() throws XPathException;

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
