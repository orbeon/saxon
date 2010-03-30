package org.orbeon.saxon.sort;



/**
 * This interface is implemented by a collation that is capable of supporting
 * the XPath functions that require matching of a substring: namely contains(),
 * starts-with, ends-with, substring-before, and substring-after.
 */
public interface SubstringMatcher extends StringCollator {

   /**
     * Test whether one string is equal to another, according to the rules
     * of the XPath compare() function. The result is true if and only if the
     * compareStrings() method returns zero: but the implementation may be more efficient
     * than calling compare and testing the result for zero
     * @param s1 the first string
     * @param s2 the second string
     * @return true iff s1 equals s2
     */

    public boolean comparesEqual(String s1, String s2);

    /**
     * Test whether one string contains another, according to the rules
     * of the XPath contains() function
     * @param s1 the containing string
     * @param s2 the contained string
     * @return true iff s1 contains s2
     */

    public boolean contains(String s1, String s2);

    /**
     * Test whether one string starts with another, according to the rules
     * of the XPath starts-with() function
     * @param s1 the containing string
     * @param s2 the contained string
     * @return true iff s1 starts with s2
     */

    public boolean startsWith(String s1, String s2);

    /**
     * Test whether one string ends with another, according to the rules
     * of the XPath ends-with() function
     * @param s1 the containing string
     * @param s2 the contained string
     * @return true iff s1 ends with s2
     */

    public boolean endsWith(String s1, String s2);

    /**
     * Return the part of a string before a given substring, according to the rules
     * of the XPath substring-before() function
     * @param s1 the containing string
     * @param s2 the contained string
     * @return the part of s1 that precedes the first occurrence of s2
     */

    public String substringBefore(String s1, String s2);

    /**
     * Return the part of a string after a given substring, according to the rules
     * of the XPath substring-after() function
     * @param s1 the containing string
     * @param s2 the contained string
     * @return the part of s1 that follows the first occurrence of s2
     */

    public String substringAfter(String s1, String s2);

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