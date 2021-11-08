package org.orbeon.saxon.sort;

/**
 *  Interface defining a map from integers to integers
 */
public interface IntToIntMap {
    /**
     * Set the value to be returned to indicate an unused entry
     * @param defaultValue the value to be returned by {@link #get(int)} if no entry
     * exists for the supplied key
     */
    void setDefaultValue(int defaultValue);

    /**
     * Get the default value used to indicate an unused entry
     * @return the value to be returned by {@link #get(int)} if no entry
     * exists for the supplied key
     */

    int getDefaultValue();

    /**
     * Clear the map.
     */
    void clear();

    /**
     * Finds a key in the map.
     *
     * @param key Key
     * @return true if the key is mapped
     */
    boolean find(int key);

    /**
     * Gets the value for this key.
     *
     * @param key Key
     * @return the value, or the default value if not found.
     */
    int get(int key);

    /**
     * Gets the size of the map.
     *
     * @return the size
     */
    int size();

    /**
     * Removes a key from the map.
     *
     * @param key Key to remove
     * @return true if the value was removed
     */
    boolean remove(int key);

    /**
     * Adds a key-value pair to the map.
     *
     * @param key   Key
     * @param value Value
     */
    void put(int key, int value);

    /**
     * Get an iterator over the integer key values held in the hash map
     * @return an iterator whose next() call returns the key values (in arbitrary order)
     */

    IntIterator keyIterator();
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

