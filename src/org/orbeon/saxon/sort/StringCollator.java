package org.orbeon.saxon.sort;

import java.io.Serializable;

/**
 * This interface represents a "collation" as defined in XPath, that is, a set of rules for comparing strings
 */
public interface StringCollator extends Serializable {

    /**
     * Compare two strings
     * @param o1 the first string
     * @param o2 the second string
     * @return 0 if the strings are considered equal, a negative integer if the first string is less than the second,
     * a positive integer if the first string is greater than the second
     */

    int compareStrings(String o1, String o2);

    /**
     * Get a collation key for two Strings. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     * @param s the string whose collation key is required
     * @return the collation key
     */

    Object getCollationKey(String s);


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

