package net.sf.saxon.om;

/**
 * A SequenceIterator is used to iterate over a sequence. An AxisIterator
 * is a SequenceIterator that always iterates over a set of nodes, and that
 * throws no exceptions; it also supports the ability
 * to find the last() position, again with no exceptions.
 * This class is an abstract implementation of AxisIterator that is used
 * as a base class for many concrete implementations. The main functionality
 * that it provides is maintaining the current position.
 */

public abstract class AxisIteratorImpl implements AxisIterator, AtomizableIterator {

    protected int position = 0;
    protected Item current;
    private boolean isAtomizing;

    /**
     * Get the current node in the sequence.
     * @return the node returned by the most recent call on next()
     */

    public final Item current() {
        return current;
    }

    /**
     * Get the current position
     * @return the position of the most recent node returned by next()
     */

    public final int position() {
        return position;
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
        isAtomizing = atomizing;
    }

    /**
     * Determine whether any nodes returned by this iterator will be atomized,
     * in which case the supplier has the option of atomizing them eagerly.
     */

    protected final boolean isAtomizing() {
        return isAtomizing;
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
