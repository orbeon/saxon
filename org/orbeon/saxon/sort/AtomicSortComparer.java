package org.orbeon.saxon.sort;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;

/**
 * A Comparator used for comparing atomic values of arbitrary item types. It encapsulates
 * a Collator that is used when the values to be compared are strings. It also supports
 * a separate method for testing equality of items, which can be used for data types that
 * are not ordered.
 *
 * The AtomicSortComparer is identical to the AtomicComparer except for its handling
 * of NaN: it treats NaN values as lower than any other value, and NaNs as equal to
 * each other.
 *
 * @author Michael H. Kay
 *
 */

public class AtomicSortComparer implements Comparator, java.io.Serializable {

    private Comparator collator;

    public AtomicSortComparer(Comparator collator) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compare(Object a, Object b) {

        // System.err.println("Comparing " + a.getClass() + "(" + a + ") with " + b.getClass() + "(" + b + ") using " + collator);
        if (a instanceof AtomicValue && !((AtomicValue)a).hasBuiltInType()) {
            a = ((AtomicValue)a).getPrimitiveValue();
        }
        if (b instanceof AtomicValue && !((AtomicValue)b).hasBuiltInType()) {
            b = ((AtomicValue)b).getPrimitiveValue();
        }
        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator);
        } else if (b instanceof UntypedAtomicValue) {
            return -((UntypedAtomicValue)b).compareTo(a, collator);
        } else if (a instanceof DoubleValue && Double.isNaN(((DoubleValue)a).getDoubleValue())) {
            if (b instanceof DoubleValue && Double.isNaN(((DoubleValue)b).getDoubleValue())) {
                return 0;
            } else {
                return -1;
            }
        } else if (b instanceof DoubleValue && Double.isNaN(((DoubleValue)b).getDoubleValue())) {
            return +1;
        } else if (a instanceof Comparable) {
            return ((Comparable)a).compareTo(b);
        } else if (a instanceof StringValue) {
            return collator.compare(((StringValue)a).getStringValue(), ((StringValue)b).getStringValue());
        } else {
            throw new ClassCastException("Objects are not comparable (" + a.getClass() + ", " + b.getClass() + ")");
        }
    }

    /**
    * Get a comparison key for an object. This must satisfy the rule that if two objects are equal,
    * then their comparison keys are equal, and vice versa. There is no requirement that the
    * comparison keys should reflect the ordering of the underlying objects.
    */

    public ComparisonKey getComparisonKey(AtomicValue a) {
        AtomicValue prim = a.getPrimitiveValue();
        if (prim instanceof NumericValue) {
            if (((NumericValue)prim).isNaN()) {
                // Deal with NaN specially. For this function, NaN is considered equal to itself
                return new ComparisonKey(Type.NUMBER, new StringValue("NaN"));
            } else {
                return new ComparisonKey(Type.NUMBER, prim);
            }
        } else if (prim instanceof StringValue) {
            if (collator instanceof Collator) {
                return new ComparisonKey(Type.STRING,
                        ((Collator)collator).getCollationKey(((StringValue)prim).getStringValue()));
            } else {
                return new ComparisonKey(Type.STRING, prim);
            }
        } else {
            return new ComparisonKey(prim.getItemType().getPrimitiveType(), prim);
        }
    }

    /**
     * Inner class: an object used as a comparison key. Two XPath atomic values are equal if and only if their
     * comparison keys are equal.
     */

    public static class ComparisonKey {
        int category;
        Object value;

        /**
         * Create a comparison key for a value in a particular category. The "category" here represents a
         * set of primitive types that allow mutual comparison (so all numeric values are in the same category).
         * @param category the category
         * @param value the value within the category
         */

        public ComparisonKey(int category, AtomicValue value) {
            this.category = category;
            this.value = value;
        }

        /**
         * Create a comparison key for strings using a particular collation. In this case the value compared is
         * the collation key provided by the collator
         * @param category Always Type.STRING
         * @param value The collation key
         */
        public ComparisonKey(int category, CollationKey value) {
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