package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.GroundedValue;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.EmptySequenceTest;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;

/**
* An EmptySequence object represents a sequence containing no members.
*/


public final class EmptySequence extends Value implements GroundedValue {

    // This class has a single instance
    private static EmptySequence THE_INSTANCE = new EmptySequence();


    /**
    * Private constructor: only the predefined instances of this class can be used
    */

    private EmptySequence() {}

    /**
    * Get the implicit instance of this class
    */

    public static EmptySequence getInstance() {
        return THE_INSTANCE;
    }

    /**
    * Return an iteration over the sequence
    */

    public SequenceIterator iterate() {
        return EmptyIterator.getInstance();
    }

    /**
     * Return the value in the form of an Item
     * @return the value in the form of an Item
     */

    public Item asItem() {
        return null;
    }

    /**
     * Determine the item type
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return EmptySequenceTest.getInstance();
    }

    /**
    * Determine the static cardinality
    */

    public int getCardinality() {
        return StaticProperty.EMPTY;
    }

    /**
     * Get the length of the sequence
     * @return always 0 for an empty sequence
     */

    public final int getLength() {
        return 0;
    }
    /**
    * Is this expression the same as another expression?
    * @throws ClassCastException if the values are not comparable
    */

    public boolean equals(Object other) {
        if (!(other instanceof EmptySequence)) {
            throw new ClassCastException("Cannot compare " + other.getClass() + " to empty sequence");
        }
        return true;
    }

    public int hashCode() {
        return 42;
    }

    /**
    * Get the effective boolean value - always false
    */

    public boolean effectiveBooleanValue() {
        return false;
    }


    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     *
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     *         numbered zero. If n is negative or >= the length of the sequence, returns null.
     */

    public Item itemAt(int n) {
        return null;
    }

    /**
     * Get a subsequence of the value
     *
     * @param min    the index of the first item to be included in the result, counting from zero.
     *               A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     *               sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     *               get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     *               is returned. If the value goes off the end of the sequence, the result returns items up to the end
     *               of the sequence
     * @return the required subsequence. If min is
     */

    public GroundedValue subsequence(int min, int length) {
        return this;
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
