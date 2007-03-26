package org.orbeon.saxon.sort;

/**
 * An object used as a comparison key. Two XPath atomic values are equal under the "eq" operator
 * if and only if their comparison keys are equal under the Java equals() method.
 */

public class ComparisonKey {
    int category;
    Object value;

    /**
     * Create a comparison key for a value in a particular category. The "category" here represents a
     * set of primitive types that allow mutual comparison (so all numeric values are in the same category).
     * @param category the category
     * @param value the value within the category
     */

    public ComparisonKey(int category, Object value) {
        this.category = category;
        this.value = value;
    }

    /**
     * Test if two comparison keys are equal
     * @param other the other comparison key
     * @return true if they are equal
     * @throws ClassCastException if the other object is not a ComparisonKey
     */
    public boolean equals(Object other) {
        if (other instanceof ComparisonKey) {
            ComparisonKey otherKey = (ComparisonKey)other;
            return this.category == otherKey.category &&
                    this.value.equals(otherKey.value);
        } else {
            throw new ClassCastException("Cannot compare a ComparisonKey to an object of a different class");
        }
    }

    /**
     * Get a hashcode for a comparison key. If two comparison keys are equal, they must have the same hash code.
     * @return the hash code.
     */
    public int hashCode() {
        return value.hashCode() ^ category;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

