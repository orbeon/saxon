package org.orbeon.saxon.sort;

import org.orbeon.saxon.om.FastStringBuffer;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Set of int values. This class is modelled on the java.net.Set interface, but it does
 * not implement this interface, because the set members are int's rather than Objects.
 * It does implement the same interface as IntHashSet, although this is currently not
 * defined as an explicit interface.
 * <p/>
 * This implementation of a set of integers is optimized to use very little storage
 * and to provide fast comparison of two sets. The equals() method determines whether
 * two sets contain the same integers.
 * <p/>
 * Not thread safe.
 *
 * @author Michael Kay
 */
public class IntArraySet implements Serializable {

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * The array of integers, which will always be sorted
     */

    private int[] contents;

    /**
     * Hashcode, evaluated lazily
     */

    private int hashCode = -1;

    /**
     *  Create an empty set
     */
    public IntArraySet() {
        contents = EMPTY_INT_ARRAY;
    }

    /**
     * Create a set containing integers from the specified IntHashSet
     */

    public IntArraySet(IntHashSet input) {
        // exploits the fact that getValues() constructs a new array
        contents = input.getValues();
        Arrays.sort(contents);
    }

    /**
     * Create one IntArraySet as a copy of another
     */

    public IntArraySet(IntArraySet input) {
        contents = new int[input.contents.length];
        System.arraycopy(input.contents, 0, contents, 0, contents.length);
    }

    public void clear() {
        contents = EMPTY_INT_ARRAY;
    }

    public int size() {
        return contents.length;
    }

    public boolean isEmpty() {
        return contents.length == 0;
    }

    public int[] getValues() {
        return contents;
    }


    public boolean contains(int value) {
        return Arrays.binarySearch(contents, value) >= 0;
    }

    public boolean remove(int value) {
        int pos = Arrays.binarySearch(contents, value);
        if (pos < 0) {
            return false;
        }
        int[] newArray = new int[contents.length - 1];
        if (pos > 0) {
            // copy the items before the one that's being removed
            System.arraycopy(contents, 0, newArray, 0, pos);
        }
        if (pos < newArray.length) {
            // copy the items after the one that's being removed
            System.arraycopy(contents, pos+1, newArray, pos, contents.length - pos);
        }
        contents = newArray;
        return true;
    }

    /**
     * Add an integer to the set
     * @param value the integer to be added
     * @return true if the integer was added, false if it was already present
     */

    public boolean add(int value) {
        if (contents.length == 0) {
            int[] c = {value};
            contents=c;
            return true;
        }
        int pos = Arrays.binarySearch(contents, value);
        if (pos >= 0) {
            return false;   // value was already present
        }
        pos = -pos - 1;     // new insertion point
        int[] newArray = new int[contents.length + 1];
        if (pos > 0) {
            // copy the items before the insertion point
            System.arraycopy(contents, 0, newArray, 0, pos);
        }
        newArray[pos] = value;
        if (pos < contents.length) {
            // copy the items after the insertion point
            System.arraycopy(contents, pos, newArray, pos+1, newArray.length - pos);
        }
        contents = newArray;
        return true;
    }

    /**
     * Get the first value in the set.
     * @throws ArrayIndexOutOfBoundsException if the set is empty
     */

    public int getFirst() {
        return contents[0];
    }

    /**
     * Get an iterator over the values
     */

    public IntIterator iterator() {
        return new IntArraySetIterator();
    }

    /**
     * Form a new set that is the union of this set with another set.
     */

    public IntArraySet union(IntArraySet other) {
        // Look for special cases: one set empty, or both sets equal
        if (size() == 0) {
            return new IntArraySet(other);
        } else if (other.size() == 0) {
            return new IntArraySet(this);
        }
        if (this.equals(other)) {
            return this;
        }
        // Form the union by a merge of the two sorted arrays
        int[] merged = new int[size() + other.size()];
        int[] a = contents;
        int[] b = other.contents;
        int m = a.length, n = b.length;
        int o=0, i=0, j=0;
        while (true) {
            if (a[i] < b[j]) {
                merged[o++] = a[i++];
            } else if (b[j] < a[i]) {
                merged[o++] = b[j++];
            } else {
                merged[o++] = a[i++];
                j++;
            }
            if (i == m) {
                System.arraycopy(b, j, merged, o, n-j);
                o += (n-j);
                return make(merged, o);
            } else if (j == n) {
                System.arraycopy(a, i, merged, o, m-i);
                o += (m-i);
                return make(merged, o);
            }
        }
    }

    public static IntArraySet make(int[] in, int size) {
        int[] out;
        if (in.length == size) {
            out = in;
        } else {
            out = new int[size];
            System.arraycopy(in, 0, out, 0, size);
        }
        return new IntArraySet(out);
    }

    private IntArraySet(int[] content) {
        this.contents = content;
    }

    public String toString() {
        FastStringBuffer sb = new FastStringBuffer(contents.length*4);
        for (int i=0; i<contents.length; i++) {
            sb.append(contents[i] + ",");
        }
        return sb.toString();
    }

    /**
     * Form a new set that is the intersection of this set with another set.
     */

//    public IntArraySet intersect(IntArraySet other) {
//        IntArraySet n = new IntArraySet((int)size());
//        IntIterator it = iterator();
//        while (it.hasNext()) {
//            int v = it.next();
//            if (other.contains(v)) {
//                n.add(it.next());
//            }
//        }
//        return n;
//    }

    /**
     * Form a new set that is the difference of this set with another set.
     */

//    public IntArraySet except(IntArraySet other) {
//        IntArraySet n = new IntArraySet((int)size());
//        IntIterator it = iterator();
//        while (it.hasNext()) {
//            int v = it.next();
//            if (!other.contains(v)) {
//                n.add(it.next());
//            }
//        }
//        return n;
//    }

    /**
     * Test if this set is a superset of another set
     */

//    public boolean containsAll(IntArraySet other) {
//        IntIterator it = other.iterator();
//        while (it.hasNext()) {
//            if (!contains(it.next())) {
//                return false;
//            }
//        }
//        return true;
//    }

    /**
     * Test if this set has overlapping membership with another set
     */

//    public boolean containsSome(IntArraySet other) {
//        IntIterator it = other.iterator();
//        while (it.hasNext()) {
//            if (contains(it.next())) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * Test whether this set has exactly the same members as another set
     */

    public boolean equals(Object other) {
        if (other instanceof IntArraySet) {
            IntArraySet s = (IntArraySet)other;
            return Arrays.equals(contents, s.contents);
//            if (size() != s.size()) {
//                return false;
//            }
//            if (hashCode() != s.hashCode()) {
//                return false;
//            }
//            for (int i=0; i<contents.length; i++) {
//                if (contents[i] != s.contents[i]) {
//                    return false;
//                }
//            }
//            return true;
        } else {
            return false;
        }
    }

    /**
     * Construct a hash key that supports the equals() test
     */

    public int hashCode() {
        // Note, hashcodes are the same as those used by IntHashSet
        if (hashCode == -1) {
            int h = 936247625;
            IntIterator it = iterator();
            while (it.hasNext()) {
                h += it.next();
            }
            hashCode = h;
        }
        return hashCode;
    }

    /**
     * Iterator class
     */

    private class IntArraySetIterator implements IntIterator, Serializable {

        private int i = 0;

        public IntArraySetIterator() {
            i = 0;
        }

        public boolean hasNext() {
            return i < contents.length;
        }

        public int next() {
            return contents[i++];
        }
    }

//    public static void main(String[] args) {
//        int[] a = {0,20,21,33,44};
//        int[] b = {1,5,8,12,15};
//        IntArraySet x = new IntArraySet(a).union(new IntArraySet(b));
//        String s = x.toString();
//        System.out.println(s);
//    }

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
// The Initial Developer of the Original Code is Michael Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
