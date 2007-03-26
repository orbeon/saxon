package org.orbeon.saxon.sort;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.FastStringBuffer;

import java.text.CollationElementIterator;
import java.text.RuleBasedCollator;
import java.util.Comparator;

/**
 * This class wraps a RuleBasedCollator to provide a SubstringMatcher. This
 * users the facilities offered by the RuleBasedCollator to implement the XPath
 * functions contains(), starts-with(), ends-with(), substring-before(), and
 * substring-after().
 */
public class RuleBasedSubstringMatcher implements SubstringMatcher {

    private RuleBasedCollator collator;

    public RuleBasedSubstringMatcher(RuleBasedCollator collator) {
        this.collator = collator;
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
        return compare(s1, s2) == 0;
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
        CollationElementIterator iter1 = collator.getCollationElementIterator(s1);
        CollationElementIterator iter2 = collator.getCollationElementIterator(s2);
        return collationContains(iter1, iter2, null, false);
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
        CollationElementIterator iter1 = collator.getCollationElementIterator(s1);
        CollationElementIterator iter2 = collator.getCollationElementIterator(s2);
        return collationContains(iter1, iter2, null, true);
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
        CollationElementIterator iter1 = collator.getCollationElementIterator(s1);
        CollationElementIterator iter2 = collator.getCollationElementIterator(s2);
        return collationStartsWith(iter1, iter2);
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
        CollationElementIterator iter1 = collator.getCollationElementIterator(s1);
        CollationElementIterator iter2 = collator.getCollationElementIterator(s2);
        int[] ia = new int[2];
        boolean ba = collationContains(iter1, iter2, ia, false);
        if (ba) {
            return s1.substring(ia[1]);
        } else {
            return "";
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
        CollationElementIterator iter1 = collator.getCollationElementIterator(s1);
        CollationElementIterator iter2 = collator.getCollationElementIterator(s2);
        int[] ib = new int[2];
        boolean bb = collationContains(iter1, iter2, ib, false);
        if (bb) {
            return s1.substring(0, ib[0]);
        } else {
            return "";
        }
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
        return collator.compare(o1, o2);
    }

    /**
    * Determine whether one string starts with another, under the terms of a given
    * collating sequence.
    * @param s0 iterator over the collation elements of the containing string
    * @param s1 iterator over the collation elements of the contained string
    */

    private boolean collationStartsWith(CollationElementIterator s0,
                                        CollationElementIterator s1) {
        while (true) {
            int e0, e1;
            do {
                e1 = s1.next();
            } while (e1 == 0);
            if (e1 == -1) {
                return true;
            }
            do {
                e0 = s0.next();
            } while (e0 == 0);
            if (e0 != e1) {
                return false;
            }
        }
    }

    /**
    * Determine whether one string contains another, under the terms of a given
    * collating sequence. If operation=ENDSWITH, the match must be at the end of the first
    * string.
    * @param s0 iterator over the collation elements of the containing string
    * @param s1 iterator over the collation elements of the contained string
    * @param offsets may be null, but if it is supplied, it must be an array of two
    * integers which, if the function returns true, will contain the start position of the
    * first matching substring, and the offset of the first character after the first
    * matching substring. This is not available for operation=endswith
    * @return true if the first string contains the second
    */

    private boolean collationContains(CollationElementIterator s0,
                                      CollationElementIterator s1,
                                      int[] offsets,
                                      boolean matchAtEnd) {
        int e0, e1;
        do {
            e1 = s1.next();
        } while (e1 == 0);
        if (e1 == -1) {
            return true;
        }
        e0 = -1;
        while (true) {
            // scan the first string to find a matching character
            while (e0 != e1) {
                do {
                    e0 = s0.next();
                } while (e0 == 0);
                if (e0 == -1) {
                    // hit the end, no match
                    return false;
                }
            }
            // matched first character, note the position of the possible match
            int start = s0.getOffset();
            if (collationStartsWith(s0, s1)) {
                if (matchAtEnd) {
                    do {
                        e0 = s0.next();
                    } while (e0 == 0);
                    if (e0 == -1) {
                        // the match is at the end
                        return true;
                    }
                    // else ignore this match and keep looking
                } else {
                    if (offsets != null) {
                        offsets[0] = start-1;
                        offsets[1] = s0.getOffset();
                    }
                    return true;
                }
            }
            // reset the position and try again
            s0.setOffset(start);

            // workaround for a difference between JDK 1.4.0 and JDK 1.4.1
            if (s0.getOffset() != start) {
                // JDK 1.4.0 takes this path
                s0.next();
            }
            s1.reset();
            e0 = -1;
            do {
                e1 = s1.next();
            } while (e1 == 0);
            // loop round to try again
        }
    }

    /**
     * Test program to output the sequence of collation element iterators for a given input string
     */
    public static void main(String[] args) {
        Configuration config = new Configuration();
        Comparator collator = StandardCollationURIResolver.getInstance().resolve(args[0], args[0], config);
        FastStringBuffer sb = new FastStringBuffer(100);
        if (collator instanceof RuleBasedCollator) {
            CollationElementIterator iter = ((RuleBasedCollator)collator).getCollationElementIterator(args[1]);
            while (true) {
                int e = iter.next();
                if (e==-1) {
                    break;
                }
                sb.append(e+" ");
            }
            System.err.println(sb.toString());
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
