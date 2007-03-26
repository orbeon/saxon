package org.orbeon.saxon.dotnet;

import cli.System.Globalization.CompareInfo;
import cli.System.Globalization.CompareOptions;
import cli.System.Globalization.SortKey;
import org.orbeon.saxon.sort.SubstringMatcher;

import java.util.Comparator;

/**
 * A Collation implemented as a wrapper around a .NET CompareInfo object. Note that the
 * objects to be compared must be strings.
 */
public class DotNetComparator implements SubstringMatcher, Comparator {

    private CompareInfo comparer;
    private CompareOptions options;

    /**
     * Create a collation based on a given .NET CompareInfo and CompareOptions
     * @param comparer the CompareInfo, which determines the language-specific
     * collation rules to be used
     * @param options Options to be used in performing comparisons, for example
     * whether they are to be case-blind and/or accent-blind
     */

    public DotNetComparator(CompareInfo comparer, CompareOptions options) {
        this.comparer = comparer;
        this.options = options;
    }

    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     * <p/>
     * The implementor must ensure that <tt>sgn(compare(x, y)) ==
     * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>compare(x, y)</tt> must throw an exception if and only
     * if <tt>compare(y, x)</tt> throws an exception.)<p>
     * <p/>
     * The implementor must also ensure that the relation is transitive:
     * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies
     * <tt>compare(x, z)&gt;0</tt>.<p>
     * <p/>
     * Finally, the implementer must ensure that <tt>compare(x, y)==0</tt>
     * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
     * <tt>z</tt>.<p>
     * <p/>
     * It is generally the case, but <i>not</i> strictly required that
     * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
     * any comparator that violates this condition should clearly indicate
     * this fact.  The recommended language is "Note: this comparator
     * imposes orderings that are inconsistent with equals."
     *
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     * @throws ClassCastException if the arguments' types prevent them from
     *                            being compared by this Comparator.
     */
    public int compare(Object o1, Object o2) {
        return comparer.Compare((String)o1, (String)o2, options);
    }

    /**
     * Test whether one string is equal to another, according to the rules
     * of the XPath compare() function. The result is true if and only if the
     * compare() method returns zero: but the implementation may be more efficient
     * than calling compare and testing the result for zero
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return true iff s1 equals s2
     */

    public boolean comparesEqual(String s1, String s2) {
        return comparer.Compare(s1, s2, options) == 0;
    }

    /**
     * Test whether one string contains another, according to the rules
     * of the XPath contains() function
     *
     * @param s1 the containing string
     * @param s2 the contained string
     * @return true iff s1 contains s2
     */

    public boolean contains(String s1, String s2) {
        return comparer.IndexOf(s1, s2, options) >= 0;
    }

    /**
     * Test whether one string ends with another, according to the rules
     * of the XPath ends-with() function
     *
     * @param s1 the containing string
     * @param s2 the contained string
     * @return true iff s1 ends with s2
     */

    public boolean endsWith(String s1, String s2) {
        return comparer.IsSuffix(s1, s2, options);
    }

    /**
     * Test whether one string starts with another, according to the rules
     * of the XPath starts-with() function
     *
     * @param s1 the containing string
     * @param s2 the contained string
     * @return true iff s1 starts with s2
     */

    public boolean startsWith(String s1, String s2) {
        return comparer.IsPrefix(s1, s2, options);
    }

    /**
     * Return the part of a string after a given substring, according to the rules
     * of the XPath substring-after() function
     *
     * @param s1 the containing string
     * @param s2 the contained string
     * @return the part of s1 that follows the first occurrence of s2
     */

    public String substringAfter(String s1, String s2) {
        int i = comparer.IndexOf(s1, s2, options);
        if (i<0) {
            return "";
        };
        // We need to know where the matched string ends. Start with a guess, that the matched string
        // is the same length as s2. If it's not, try shorter and longer strings until we find one that's
        // equal. The search strategy is designed on the assumption that the matched string is of similar
        // length to the input string.
        int pivot = i+s2.length();
        if (comparer.Compare(s1.substring(i, pivot), s2, options) == 0) {
            return s1.substring(pivot);
        }

        int z=0;
        boolean reachedStart;
        boolean reachedEnd;
        while (true) {
            z++;
            reachedStart = pivot-z < i;
            reachedEnd = pivot+z >= s1.length();
            if (!reachedEnd && comparer.Compare(s1.substring(i, pivot+z), s2, options) == 0) {
                return s1.substring(pivot+z);
            }
            if (!reachedStart && comparer.Compare(s1.substring(i, pivot-z), s2, options) == 0) {
                return s1.substring(pivot-z);
            }
            if (reachedStart && reachedEnd) {
                // shouldn't happen
                return "";
            }
        }
    }

    /**
     * Return the part of a string before a given substring, according to the rules
     * of the XPath substring-before() function
     *
     * @param s1 the containing string
     * @param s2 the contained string
     * @return the part of s1 that precedes the first occurrence of s2
     */

    public String substringBefore(String s1, String s2) {
        int j = comparer.IndexOf(s1, s2, options);
        if (j<0) {
            return "";
        };
        return s1.substring(0, j);
    }

    /**
     * Return a collation key. This is a wrapper around the System.Globalization.Sortkey object,
     * where the wrapper implements the required comparison methods.
     */

    public SortKeyWrapper getCollationKey(String value) {
        final SortKey sortKey = comparer.GetSortKey(value, options);
        return new SortKeyWrapper(sortKey);
    }

    private static class SortKeyWrapper implements Comparable {

        private SortKey key;

        public SortKeyWrapper(SortKey key) {
            this.key = key;
        }

        public int compareTo(Object other) {
            return SortKey.Compare(key, (SortKey)other);
        }

        /**
         * Returns a hash code value for the object.
         */
        public int hashCode() {
            byte[] data = key.get_KeyData();
            int h = 0x7d6a8521;
            for (int i=0; i<data.length; i++) {
                h ^= (data[i] << (i & 31));
            }
            return h;
        }

        /**
         * Indicates whether some other SortKeyWrapper is "equal to" this one (that is, it represents
         * the sortkey of a value that is equal to this value).
         */
        public boolean equals(Object obj) {
            byte[] data1 = key.get_KeyData();
            byte[] data2 = (((SortKeyWrapper)obj).key).get_KeyData();
            if (data1.length != data2.length) {
                return false;
            }
            for (int i=data1.length - 1; i >= 0; i--) {
                if (data1[i] != data2[i]) {
                    return false;
                }
            }
            return true;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

