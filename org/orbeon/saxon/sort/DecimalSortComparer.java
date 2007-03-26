package org.orbeon.saxon.sort;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.value.AtomicValue;

/**
 * A Comparator used for sorting values that are known to be instances of xs:decimal (including xs:integer),
 * It also supports a separate method for getting a collation key to test equality of items
 *
 * @author Michael H. Kay
 *
 */

public class DecimalSortComparer implements AtomicComparer {

    private static DecimalSortComparer THE_INSTANCE = new DecimalSortComparer();

    public static DecimalSortComparer getInstance() {
        return THE_INSTANCE;
    }

    private DecimalSortComparer() {

    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should normally be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compare(Object a, Object b) {
        if (a == null) {
            return (b == null ? 0 : -1);
        } else if (b == null) {
            return +1;
        }

        a = ((AtomicValue)a).getPrimitiveValue();
        b = ((AtomicValue)b).getPrimitiveValue();

        return ((Comparable)a).compareTo(b);
    }

    /**
     * Test whether two values compare equal.
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        return compare(a, b) == 0;
    }

    /**
     * Get a comparison key for an object. This must satisfy the rule that if two objects are equal as defined
     * by the XPath eq operator, then their comparison keys are equal as defined by the Java equals() method,
     * and vice versa. There is no requirement that the comparison keys should reflect the ordering of the
     * underlying objects.
    */

    public ComparisonKey getComparisonKey(AtomicValue a) {
        AtomicValue prim = a.getPrimitiveValue();
        return new ComparisonKey(StandardNames.XDT_NUMERIC, prim);
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