package org.orbeon.saxon.sort;

import java.io.Serializable;


/**
 * A collating sequence that uses Unicode codepoint ordering
 */

public class CodepointCollator implements StringCollator, SubstringMatcher, Serializable {

    private static CodepointCollator theInstance = new CodepointCollator();

    public static CodepointCollator getInstance() {
        return theInstance;
    }

    /**
    * Compare two string objects.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are of the wrong type for this Comparer
    */

    public int compareStrings(String a, String b) {
        //return ((String)a).compareTo((String)b);
        // Note that Java does UTF-16 code unit comparison, which is not the same as Unicode codepoint comparison
        // except in the "equals" case. So we have to do a character-by-character comparison
        return compareCS((String)a, (String)b);
    }

    /**
     * Compare two CharSequence objects. This is hand-coded to avoid converting the objects into
     * Strings.
     * @return <0 if a<b, 0 if a=b, >0 if a>b
     * @throws ClassCastException if the objects are of the wrong type for this Comparer
     */

    public int compareCS(CharSequence a, CharSequence b) {
        int alen = a.length();
        int blen = b.length();
        int i = 0;
        int j = 0;
        while (true) {
            if (i == alen) {
                if (j == blen) {
                    return 0;
                } else {
                    return -1;
                }
            }
            if (j == blen) {
                return +1;
            }
            // Following code is needed when comparing a BMP character against a surrogate pair
            // Note: we could do this comparison without fully computing the codepoint, but it's a very rare case
            int nexta = (int)a.charAt(i++);
            if (nexta >= 55296 && nexta <= 56319) {
                nexta = ((nexta - 55296) * 1024) + ((int)a.charAt(i++) - 56320) + 65536;
            }
            int nextb = (int)b.charAt(j++);
            if (nextb >= 55296 && nextb <= 56319) {
                nextb = ((nextb - 55296) * 1024) + ((int)b.charAt(j++) - 56320) + 65536;
            }
            int c = nexta - nextb;
            if (c != 0) {
                return c;
            }
        }
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
        return s1.equals(s2);
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
        return s1.indexOf(s2) >= 0;
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
        return s1.endsWith(s2);
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
        return s1.startsWith(s2);
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
        int i = s1.indexOf(s2);
        if (i<0) {
            return "";
        }
        return s1.substring(i+s2.length());
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
        int j = s1.indexOf(s2);
        if (j<0) {
            return "";
        };
        return s1.substring(0, j);
    }

    /**
     * Get a collation key for two Strings. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     */

    public Object getCollationKey(String s) {
        return s;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//