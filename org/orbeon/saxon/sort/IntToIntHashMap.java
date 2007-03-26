package org.orbeon.saxon.sort;

import java.io.Serializable;

/**
 * A hash table that maps int keys to int values.
 *
 * @author Dave Hale, Landmark Graphics
 * @author Dominique Devienne
 * @author Michael Kay: created this class based on IntHashMap
 */
public class IntToIntHashMap implements Serializable {

    /**
     * Initializes a map with a capacity of 8 and a load factor of 0,25.
     */
    public IntToIntHashMap() {
        this(8, 0.25);
    }

    /**
     * Initializes a map with the given capacity and a load factor of 0,25.
     *
     * @param capacity the initial capacity.
     */
    public IntToIntHashMap(int capacity) {
        this(capacity, 0.25);
    }

    /**
     * Constructs a new map with initial capacity, and load factor.
     * <p/>
     * The capacity is the number of keys that can be mapped without resizing
     * the arrays in which keys and values are stored. For efficiency, only
     * a fraction of the elements in those arrays are used. That fraction is
     * the specified load factor. The initial length of the arrays equals the
     * smallest power of two not less than the ratio capacity/factor. The
     * capacity of the map is increased, as necessary. The maximum number
     * of keys that can be mapped is 2^30.
     *
     * @param capacity the initial capacity.
     * @param factor   the load factor.
     */
    public IntToIntHashMap(int capacity, double factor) {
        _factor = factor;
        setCapacity(capacity);
    }

    /**
     * Set the value to be returned to indicate an unused entry
     */
    public void setDefaultValue(int defaultValue) {
        _defaultValue = defaultValue;
    }

    /**
     * Get the default value used to indicate an unused entry
     */

    public int getDefaultValue() {
        return _defaultValue;
    }

    /**
     * Clears the map.
     */
    public void clear() {
        _n = 0;
        for (int i = 0; i < _nmax; ++i) {
            _filled[i] = false;
        }
    }

    /**
     * Finds a key in the map.
     *
     * @param key Key
     * @return true if the key is mapped
     */
    public boolean find(int key) {
        return _filled[indexOf(key)] ? true : false;
    }

    /**
     * Gets the value for this key.
     *
     * @param key Key
     * @return the value, null if not found.
     */
    public int get(int key) {
        int i = indexOf(key);
        return _filled[i] ? _value[i] : _defaultValue;
    }

    /**
     * Gets the size of the map.
     *
     * @return the size
     */
    public int size() {
        return _n;
    }

    /**
     * Removes a key from the map.
     *
     * @param key Key to remove
     * @return true if the value was removed
     */
    public boolean remove(int key) {
        // Knuth, v. 3, 527, Algorithm R.
        int i = indexOf(key);
        if (!_filled[i]) {
            return false;
        }
        --_n;
        for (; ;) {
            _filled[i] = false;
            int j = i;
            int r;
            do {
                i = (i - 1) & _mask;
                if (!_filled[i]) {
                    return true;
                }
                r = hash(_key[i]);
            } while ((i <= r && r < j) || (r < j && j < i) || (j < i && i <= r));
            _key[j] = _key[i];
            _value[j] = _value[i];
            _filled[j] = _filled[i];
        }
    }

    /**
     * Adds a key-value pair to the map.
     *
     * @param key   Key
     * @param value Value
     */
    public void put(int key, int value) {
        int i = indexOf(key);
        if (_filled[i]) {
            _value[i] = value;
        } else {
            _key[i] = key;
            _value[i] = value;
            _filled[i] = true;
            grow();
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // private

    private static final int NBIT = 30; // NMAX = 2^NBIT
    private static final int NMAX = 1 << NBIT; // maximum number of keys mapped
    private double _factor; // 0.0 <= _factor <= 1.0
    private int _defaultValue = Integer.MAX_VALUE;
    private int _nmax; // 0 <= _nmax = 2^nbit <= 2^NBIT = NMAX
    private int _n; // 0 <= _n <= _nmax <= NMAX
    private int _nlo; // _nmax*_factor (_n<=_nlo, if possible)
    private int _nhi; //  NMAX*_factor (_n< _nhi, if possible)
    private int _shift; // _shift = 1 + NBIT - nbit (see function hash() below)
    private int _mask; // _mask = _nmax - 1
    private int[] _key; // array[_nmax] of keys
    //@SuppressWarnings(value = {"unchecked"})
    private int[] _value; // array[_nmax] of values
    private boolean[] _filled; // _filled[i]==true iff _key[i] is mapped

    private int hash(int key) {
        // Knuth, v. 3, 509-510. Randomize the 31 low-order bits of c*key
        // and return the highest nbits (where nbits <= 30) bits of these.
        // The constant c = 1327217885 approximates 2^31 * (sqrt(5)-1)/2.
        return ((1327217885 * key) >> _shift) & _mask;
    }

    private int indexOf(int key) {
        int i = hash(key);
        while (_filled[i]) {
            if (_key[i] == key) {
                return i;
            }
            i = (i - 1) & _mask;
        }
        return i;
    }

    private void grow() {
        ++_n;
        if (_n > NMAX) {
            throw new RuntimeException("number of keys mapped exceeds " + NMAX);
        }
        if (_nlo < _n && _n <= _nhi) {
            setCapacity(_n);
        }
    }

    private void setCapacity(int capacity) {
        if (capacity < _n) {
            capacity = _n;
        }
        double factor = (_factor < 0.01) ? 0.01 : (_factor > 0.99) ? 0.99 : _factor;
        int nbit, nmax;
        for (nbit = 1, nmax = 2; nmax * factor < capacity && nmax < NMAX; ++nbit, nmax *= 2) {
            ;
        }
        int nold = _nmax;
        if (nmax == nold) {
            return;
        }
        _nmax = nmax;
        _nlo = (int)(nmax * factor);
        _nhi = (int)(NMAX * factor);
        _shift = 1 + NBIT - nbit;
        _mask = nmax - 1;
        int[] key = _key;
        int[] value = _value;
        boolean[] filled = _filled;
        _n = 0;
        _key = new int[nmax];
        // semantically equivalent to _value = new V[nmax]
        _value = new int[nmax];
        _filled = new boolean[nmax];
        if (key != null) {
            for (int i = 0; i < nold; ++i) {
                if (filled[i]) {
                    put(key[i], value[i]);
                }
            }
        }
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
// The Initial Developer of the Original Code is Dave Hale and Dominique Devienne of Landmark Graphics;
// the code was retrofitted to JDK 1.4 by Michael Kay, Saxonica.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.

