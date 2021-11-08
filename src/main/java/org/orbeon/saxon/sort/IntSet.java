package org.orbeon.saxon.sort;

/**
 * A set of integers represented as int values
 */
public interface IntSet {

    /**
     * Clear the contents of the IntSet (making it an empty set)
     */
    void clear();

    /**
     * Get the number of integers in the set
     * @return the size of the set
     */

    int size();

    /**
     * Determine if the set is empty
     * @return true if the set is empty, false if not
     */

    boolean isEmpty();

    /**
     * Determine whether a particular integer is present in the set
     * @param value the integer under test
     * @return true if value is present in the set, false if not
     */

    boolean contains(int value);

    /**
     * Remove an integer from the set
     * @param value the integer to be removed
     * @return true if the integer was present in the set, false if it was not present
     */

    boolean remove(int value);

    /**
     * Add an integer to the set
     * @param value the integer to be added
     * @return true if the integer was added, false if it was already present
     */

    boolean add(int value);

    /**
     * Get an iterator over the values
     */

    IntIterator iterator();

    /**
     * Test if this set is a superset of another set
     */

    public boolean containsAll(IntSet other);    
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

