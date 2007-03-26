package org.orbeon.saxon.sort;

import java.io.Serializable;

/**
 * Set of int values. This class is modelled on the java.net.Set interface, but it does
 * not implement this interface, because the set members are int's rather than Objects.
 * <p/>
 * Not thread safe.
 *
 * @author Dominique Devienne
 * @author Michael Kay: retrofitted to JDK 1.4, added iterator()
 */
public class IntHashSet implements Serializable {

    private static final int NBIT = 30; // MAX_SIZE = 2^NBIT

    /**
     * The maximum number of elements this container can contain.
     */
    public static final int MAX_SIZE = 1 << NBIT; // maximum number of keys mapped

    /**
     * This set's NO-DATA-VALUE.
     */
    public final int ndv;

    /**
     * Initializes a set with a capacity of 8 and a load factor of 0,25.
     *
     * @see #IntHashSet(int,double,int)
     */
    public IntHashSet() {
        this(8, 0.25, Integer.MIN_VALUE);
    }

    /**
     * Initializes a set with the given capacity and a load factor of 0,25.
     *
     * @param capacity the initial capacity.
     * @see #IntHashSet(int,double,int)
     */
    public IntHashSet(int capacity) {
        this(capacity, 0.25, Integer.MIN_VALUE);
    }

    /**
     * Initializes a set with a load factor of 0,25.
     *
     * @param capacity    the initial capacity.
     * @param noDataValue the value to use for non-values.
     * @see #IntHashSet(int,double,int)
     */
    public IntHashSet(int capacity, int noDataValue) {
        this(capacity, 0.25, noDataValue);
    }

    /**
     * Constructs a new set with initial capacity, and load factor.
     * <p/>
     * The capacity is the number of keys that can be mapped without resizing
     * the arrays in which keys and values are stored. For efficiency, only
     * a fraction of the elements in those arrays are used. That fraction is
     * the specified load factor. The initial length of the arrays equals the
     * smallest power of two not less than the ratio capacity/factor. The
     * capacity of the set is increased, as necessary. The maximum number
     * of keys that can be mapped is 2^30.
     *
     * @param capacity    the initial capacity.
     * @param factor      the load factor.
     * @param noDataValue the value to use for non-values.
     */
    public IntHashSet(int capacity, double factor, int noDataValue) {
        ndv = noDataValue;
        _factor = factor;
        setCapacity(capacity);
    }

    public void clear() {
        _size = 0;
        for (int i = 0; i < _nmax; ++i) {
            _values[i] = ndv;
        }
    }

    public int size() {
        return _size;
    }

    public boolean isEmpty() {
        return _size == 0;
    }

    public int getFirst(int defaultValue) {
        for (int v=0; v<_values.length; v++) {
            if (_values[v] != ndv) {
                return _values[v];
            }
        }
        return defaultValue;
    }

    public int[] getValues() {
        int index = 0;
        final int[] values = new int[_size];
        for (int v=0; v<_values.length; v++) {
            if (_values[v] != ndv) {
                values[index++] = _values[v];
            }
        }
        return values;
    }


    public boolean contains(int value) {
        return (_values[indexOf(value)] != ndv) ? true : false;
    }


    public boolean remove(int value) {
        // Knuth, v. 3, 527, Algorithm R.
        int i = indexOf(value);
        if (_values[i] == ndv) {
            return false;
        }
        --_size;
        for (; ;) {
            _values[i] = ndv;
            int j = i;
            int r;
            do {
                i = (i - 1) & _mask;
                if (_values[i] == ndv) {
                    return true;
                }
                r = hash(_values[i]);
            } while ((i <= r && r < j) || (r < j && j < i) || (j < i && i <= r));
            _values[j] = _values[i];
        }
    }


    public boolean add(int value) {
        if (value == ndv) {
            throw new IllegalArgumentException("Can't add the 'no data' value");
        }
        int i = indexOf(value);
        if (_values[i] == ndv) {
            ++_size;
            _values[i] = value;

            // Check new size
            if (_size > MAX_SIZE) {
                throw new RuntimeException("Too many elements (> " + MAX_SIZE + ')');
            }
            if (_nlo < _size && _size <= _nhi) {
                setCapacity(_size);
            }
            return true;
        } else {
            return false; // leave set unchanged
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // private

    private double _factor; // 0.0 <= _factor <= 1.0
    private int _nmax; // 0 <= _nmax = 2^nbit <= 2^NBIT = MAX_SIZE
    private int _size; // 0 <= _size <= _nmax <= MAX_SIZE
    private int _nlo; // _nmax*_factor (_size<=_nlo, if possible)
    private int _nhi; //  MAX_SIZE*_factor (_size< _nhi, if possible)
    private int _shift; // _shift = 1 + NBIT - nbit (see function hash() below)
    private int _mask; // _mask = _nmax - 1
    private int[] _values; // array[_nmax] of values

    private int hash(int key) {
        // Knuth, v. 3, 509-510. Randomize the 31 low-order bits of c*key
        // and return the highest nbits (where nbits <= 30) bits of these.
        // The constant c = 1327217885 approximates 2^31 * (sqrt(5)-1)/2.
        return ((1327217885 * key) >> _shift) & _mask;
    }

    /**
     * Gets the index of the value, if it exists, or the index at which
     * this value would be added if it does not exist yet.
     */
    private int indexOf(int value) {
        int i = hash(value);
        while (_values[i] != ndv) {
            if (_values[i] == value) {
                return i;
            }
            i = (i - 1) & _mask;
        }
        return i;
    }

    private void setCapacity(int capacity) {
        if (capacity < _size) {
            capacity = _size;
        }
        double factor = (_factor < 0.01) ? 0.01 : (_factor > 0.99) ? 0.99 : _factor;
        int nbit, nmax;
        for (nbit = 1, nmax = 2; nmax * factor < capacity && nmax < MAX_SIZE; ++nbit, nmax *= 2) {
            ;
        }
        int nold = _nmax;
        if (nmax == nold) {
            return;
        }

        _nmax = nmax;
        _nlo = (int)(nmax * factor);
        _nhi = (int)(MAX_SIZE * factor);
        _shift = 1 + NBIT - nbit;
        _mask = nmax - 1;

        _size = 0;
        int[] values = _values;
        _values = new int[nmax];
        java.util.Arrays.fill(_values, ndv); // empty all values
        if (values != null) {
            for (int i = 0; i < nold; ++i) {
                int value = values[i];
                if (value != ndv) {
                    // Don't use add, because the capacity is necessarily large enough,
                    // and the value is necessarily unique (since in this set already)!
                    //add(values[i]);
                    ++_size;
                    _values[indexOf(value)] = value;
                }
            }
        }
    }

    /**
     * Get an iterator over the values
     */

    public IntIterator iterator() {
        return new IntHashSetIterator();
    }

    /**
     * Form a new set that is the union of this set with another set.
     */

    public IntHashSet union(IntHashSet other) {
        IntHashSet n = new IntHashSet((int)(size() + other.size()));
        IntIterator it = iterator();
        while (it.hasNext()) {
            n.add(it.next());
        }
        it = other.iterator();
        while (it.hasNext()) {
            n.add(it.next());
        }
        return n;
    }

    /**
     * Form a new set that is the intersection of this set with another set.
     */

    public IntHashSet intersect(IntHashSet other) {
        IntHashSet n = new IntHashSet((int)size());
        IntIterator it = iterator();
        while (it.hasNext()) {
            int v = it.next();
            if (other.contains(v)) {
                n.add(v);
            }
        }
        return n;
    }

    /**
     * Form a new set that is the difference of this set with another set.
     */

    public IntHashSet except(IntHashSet other) {
        IntHashSet n = new IntHashSet((int)size());
        IntIterator it = iterator();
        while (it.hasNext()) {
            int v = it.next();
            if (!other.contains(v)) {
                n.add(v);
            }
        }
        return n;
    }

    /**
     * Test if this set is a superset of another set
     */

    public boolean containsAll(IntHashSet other) {
        IntIterator it = other.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if this set has overlapping membership with another set
     */

    public boolean containsSome(IntHashSet other) {
        IntIterator it = other.iterator();
        while (it.hasNext()) {
            if (contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether this set has exactly the same members as another set
     */

    public boolean equals(Object other) {
        if (other instanceof IntHashSet) {
            IntHashSet s = (IntHashSet)other;
            return (size() == s.size() && containsAll(s));
        } else {
            return false;
        }
    }

    /**
     * Construct a hash key that supports the equals() test
     */

    public int hashCode() {
        int h = 936247625;
        IntIterator it = iterator();
        while (it.hasNext()) {
            h += it.next();
        }
        return h;
    }

    /**
     * Iterator class
     */

    private class IntHashSetIterator implements IntIterator, Serializable {

        private int i = 0;

        public IntHashSetIterator() {
            i = 0;
        }

        public boolean hasNext() {
            while (i < _values.length) {
                if (_values[i] != ndv) {
                    return true;
                } else {
                    i++;
                }
            }
            return false;
        }

        public int next() {
            return _values[i++];
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
// The Initial Developer of the Original Code is Dominique Devienne of Landmark Graphics.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
