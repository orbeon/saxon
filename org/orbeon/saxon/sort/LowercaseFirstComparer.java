package org.orbeon.saxon.sort;

import java.util.Comparator;

/**
 * A Comparer used for comparing keys
 *
 * @author Michael H. Kay
 */

public class LowercaseFirstComparer implements Comparator, java.io.Serializable {

    private Comparator baseCollator;

    public LowercaseFirstComparer(Comparator base) {
        baseCollator = base;
    }

    /**
     * Compare two string objects: case is irrelevant, unless the strings are equal ignoring
     * case, in which case lowercase comes first.
     *
     * @return <0 if a<b, 0 if a=b, >0 if a>b
     * @throws ClassCastException if the objects are of the wrong type for this Comparer
     */

    public int compare(Object a, Object b) {
        int diff = baseCollator.compare(a, b);
        if (diff != 0) {
            return diff;
        }

        // This is doing a character-by-character comparison, which isn't really right.
        // There might be a sequence of letters constituting a single collation unit.

        CharSequence a1 = (CharSequence)a;
        CharSequence b1 = (CharSequence)b;

        int i = 0;
        int j = 0;
        while (true) {
            // Skip characters that are equal in the two strings
            while (i < a1.length() && j < b1.length() && a1.charAt(i) == b1.charAt(j)) {
                i++;
                j++;
            }
            // Skip non-letters in the first string
            while (i < a1.length() && !Character.isLetter(a1.charAt(i))) {
                i++;
            }
            // Skip non-letters in the second string
            while (j < b1.length() && !Character.isLetter(b1.charAt(j))) {
                j++;
            }
            // If we've got to the end of either string, treat the strings as equal
            if (i >= a1.length()) {
                return 0;
            }
            if (j >= b1.length()) {
                return 0;
            }
            // If one of the characters is lower case and the other isn't, the issue is decided
            boolean aLower = Character.isLowerCase(a1.charAt(i++));
            boolean bLower = Character.isLowerCase(b1.charAt(j++));
            if (aLower && !bLower) {
                return -1;
            }
            if (bLower && !aLower) {
                return +1;
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
// The Initial Developer of this module is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//